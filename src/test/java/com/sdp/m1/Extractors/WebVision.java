package com.sdp.m1.Extractors;

import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingDriverWait;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sdp.m1.Pages.LoginPage;
import com.sdp.m1.Utils.TestConfigs;
import com.sdp.m1.Utils.TestUtils;

public class WebVision {

    private final SelfHealingDriverWait wait;
    private final SelfHealingDriver driver;
    private static final Boolean SELECT_HIDDEN = false;
    private final Gson gson;
    private static final Logger logger = Logger.getLogger(WebPageExtractorJSON.class.getName());

    private static final class Component {
        String type, tag, text, id, classes, ariaLabel, selector, role;
        Map<String, Integer> boundingBox = new HashMap<>();
        Set<Map<String, String>> actions = new HashSet<>();
        Set<Map<String, String>> fields = new HashSet<>();
        Map<String, String> attributes = new HashMap<>();
    }

    private static final class PageData {
        String pageUrl;
        List<Component> components;

        PageData(String pageUrl, List<Component> components) {
            this.pageUrl = pageUrl;
            this.components = components;
        }
    }

    public WebVision(SelfHealingDriver driver, SelfHealingDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    private PageData extractPageData() {
        String currentUrl = driver.getCurrentUrl();
        Document doc = Jsoup.parse(driver.getPageSource());
        List<Component> components = new ArrayList<>();
        extractByTag(doc, driver, "form", "form", components);
        extractByTag(doc, driver, "nav", "navbar", components);
        extractByTag(doc, driver, "header", "header", components);
        extractByTag(doc, driver, "aside", "sidebar", components);
        extractByTag(doc, driver, "main", "main", components);
        extractByTag(doc, driver, "footer", "footer", components);
        extractByTag(doc, driver, "div", "section", components);
        return new PageData(currentUrl, components);
    }

    public void runExtractor(String fileName) {
        PageData pageData = extractPageData();
        writeFile(pageData, fileName);
    }

    private void extractByTag(Document doc, WebDriver driver, String tag, String type, List<Component> components) {
        for (Element el : doc.select(tag)) {
            try {
                WebElement we = driver.findElement(By.xpath(getXPath(el)));
                if (!we.isDisplayed() && !SELECT_HIDDEN) continue;

                Component c = new Component();
                c.type = type;
                c.tag = el.tagName();
                c.text = el.ownText().trim();
                c.id = el.id();
                c.classes = el.className();
                c.role = el.attr("role");
                c.selector = buildSelector(el);
                c.ariaLabel = el.attr("aria-label");

                el.attributes().forEach(attr -> c.attributes.put(attr.getKey(), attr.getValue()));

                Point p = we.getLocation();
                Dimension d = we.getSize();
                c.boundingBox.put("x", p.getX());
                c.boundingBox.put("y", p.getY());
                c.boundingBox.put("width", d.getWidth());
                c.boundingBox.put("height", d.getHeight());

                Elements buttons = el.select("button, input[type=submit], input[type=button], input[type=reset], a");
                Elements inputs = el.select("input, textarea, select");
                fieldBuilder(buttons, c.actions, doc, null);
                fieldBuilder(inputs, c.fields, doc, new ArrayList<>(Arrays.asList("submit", "reset", "button")));

                if (components.stream().noneMatch(existing -> existing.selector.equals(c.selector) && existing.type.equals(c.type))) {
                    components.add(c);
                }
            } catch (org.openqa.selenium.NoSuchElementException e) {
                logger.warning("Could not find element in Selenium DOM, skipping: " + buildSelector(el));
            }
        }
    }

    private Set<Map<String, String>> fieldBuilder(Elements elements, Set<Map<String, String>> fieldSets, Document doc, ArrayList<String> ignoreTypes) {
        if (elements == null) return fieldSets;

        for (Element element : elements) {
            if ("hidden".equals(element.attr("type")) && !SELECT_HIDDEN) continue;
            if (ignoreTypes != null && ignoreTypes.contains(element.attr("type"))) continue;

            Map<String, String> meta = new HashMap<>();
            meta.put("selector", buildSelector(element));
            meta.put("text", element.text().trim());
            meta.put("type", element.attr("type").isEmpty() ? element.tagName() : element.attr("type"));
            meta.put("name", element.attr("name"));
            meta.put("role", element.attr("role"));
            meta.put("value", element.attr("value"));
            meta.put("placeholder", element.attr("placeholder"));
            meta.put("readonly", String.valueOf(element.hasAttr("readonly")));
            meta.put("disabled", String.valueOf(element.hasAttr("disabled")));

            String label = findLabelText(doc, element);
            if (label != null && !label.isEmpty()) meta.put("label", label);

            if ("select".equals(element.tagName())) {
                meta.put("options", String.join(",", getSelectOptions(element)));
            }
            if (element.hasAttr("href")) meta.put("href", element.attr("href"));
            if (element.hasAttr("aria-label")) meta.put("aria-label", element.attr("aria-label"));

            fieldSets.add(meta);
        }
        return fieldSets;
    }

    private Set<String> getSelectOptions(Element selectElement) {
        Set<String> options = new HashSet<>();
        for (Element option : selectElement.children()) {
            if ("option".equals(option.tagName())) {
                options.add(option.text().trim());
            }
        }
        return options;
    }

    private String buildSelector(Element el) {
        if (el.hasAttr("id") && !el.id().isEmpty()) {
            return "#" + el.id();
        }
        return getXPath(el);
    }

    private String getXPath(Element el) {
        if (el == null) return "";
        if (el.hasAttr("id") && !el.id().isEmpty()) {
            return "//*[@id='" + el.id() + "']";
        }
        List<String> path = new ArrayList<>();
        for (Element current = el; current != null && !"html".equals(current.tagName()); current = current.parent()) {
            if (current.hasAttr("id") && !current.id().isEmpty()) {
                path.add(0, "//*[@id='" + current.id() + "']");
                return String.join("/", path);
            }
            int index = 1;
            for (Element sibling = current.previousElementSibling(); sibling != null; sibling = sibling.previousElementSibling()) {
                if (sibling.tagName().equals(current.tagName())) index++;
            }
            path.add(0, current.tagName() + "[" + index + "]");
        }
        return "/html/" + String.join("/", path);
    }

    private String findLabelText(Document doc, Element input) {
        String id = input.id();
        if (id != null && !id.isEmpty()) {
            Element label = doc.selectFirst("label[for=" + id + "]");
            if (label != null) return label.text();
        }
        return "";
    }

    private void writeFile(PageData pageData, String fileName) {
        try {
            Files.createDirectories(Paths.get(fileName).getParent());
            try (FileWriter fw = new FileWriter(fileName)) {
                this.gson.toJson(pageData, fw);
                logger.info(String.format("Extracted %d components -> %s", pageData.components.size(), fileName));
            }
        } catch (Exception e) {
            logger.severe("Failed to write JSON file: " + e.getMessage());
        }
    }

    /**
     * This is the new static method to be called from the Hooks on test failure.
     * It captures the current page state and saves it to the specified file.
     */
    public static void capturePageOnFailure(SelfHealingDriver driver, String outputFilePath) {
        if (driver == null) {
            logger.severe("Cannot capture page on failure: WebDriver is null.");
            return;
        }
        try {
            logger.info("Failure detected. Capturing page structure to: " + outputFilePath);
            SelfHealingDriverWait wait = TestUtils.getWaitDriver(driver);
            WebPageExtractorJSON extractor = new WebPageExtractorJSON(driver, wait);
            extractor.runExtractor(outputFilePath);
        } catch (Exception e) {
            logger.severe("Failed to capture page structure on failure: " + e.getMessage());
            // Do not re-throw, as this is a best-effort operation during teardown.
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            logger.severe("Usage: java WebVision <URL> <output_file_path>");
            System.exit(1);
        }

        String targetUrl = args[0];
        String fileName = args[1];

        SelfHealingDriver driver = TestUtils.getDriver(TestConfigs.getBrowser());
        SelfHealingDriverWait wait = TestUtils.getWaitDriver(driver);

        try {
            // Perform login to access protected pages
            driver.get(TestConfigs.getBaseUrl());
            LoginPage loginPage = new LoginPage(driver, wait);
            loginPage.waitForPageLoad();
            loginPage.enterUsername(TestConfigs.getAdminUsername());
            loginPage.enterPassword(TestConfigs.getAdminPassword());
            loginPage.clickLoginButton();
            loginPage.verifyDashboard();

            // Navigate to the target URL provided as an argument
            driver.navigate().to(targetUrl);
            logger.info("Navigated to target URL: " + targetUrl);

            logger.info("Waiting for page to be fully loaded...");
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState == 'complete'"));

            // Use WebVision itself to perform the extraction
            WebVision extractor = new WebVision(driver, wait);
            extractor.runExtractor(fileName);
            logger.info("Extraction complete. Output saved to: " + fileName);

        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
