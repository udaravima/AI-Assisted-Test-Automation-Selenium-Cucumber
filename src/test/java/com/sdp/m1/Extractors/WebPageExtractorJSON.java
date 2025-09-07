package com.sdp.m1.Extractors;

import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
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

import junit.framework.Test;

// import com.sdp.m1.Runner.TestConfigs;

/* 
// TODO:
//
// - Good Naming scheme
// - Add Test name to the file 
// - If the type is 'hidden' of a element ignore :)
// - add checks for css selector and 'hidden' (bool vals) :)
// - Adjust the text not just element.text() but with element.attr("placeholder") if exists or with other info :|
// - Make this as modulable 
// - Login and session feature fix
// - Select tag upgrades! :)
*/
public class WebPageExtractorJSON {

    @SuppressWarnings("unused")
    private final SelfHealingDriverWait wait;
    private final SelfHealingDriver driver;
    private static final Boolean SELECT_HIDDEN = false;
    private final Gson gson;
    private static final Boolean USE_CSS_SELECTOR = false;
    private static final Boolean USE_COOKIE = false;
    private static final Integer THREAD_DELAY = 8000;
    private static final String COOKIE_NAME = "JSESSIONID";
    private static final Cookie COOKIE = new Cookie(COOKIE_NAME, "412887E1FC827446C377BFE9D4BEB332");
    private static final Boolean LOGIN_REQUIRED = true;
    private static final String NAV_URL = "https://m1-impl.hsenidmobile.com/provisioning/home.html";
    private static final Logger logger = Logger.getLogger(WebPageExtractorJSON.class.getName());

    @SuppressWarnings("unused")
    private static final class Component {
        String type, tag, text, id, classes, ariaLabel, selector, role;// e.g. form, navbar, section
        Map<String, Integer> boundingBox = new HashMap<>();
        Set<Map<String, String>> actions = new HashSet<>();
        Set<Map<String, String>> fields = new HashSet<>();
        Map<String, String> attributes = new HashMap<>();
    }

    @SuppressWarnings("unused")
    private static final class PageData {
        String pageUrl;
        List<Component> components;

        PageData(String pageUrl, List<Component> components) {
            this.pageUrl = pageUrl;
            this.components = components;
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    public WebPageExtractorJSON() {
        // This constructor is for standalone execution via the main method.
        this.driver = TestUtils.getDriver(TestConfigs.getBrowser());
        this.wait = TestUtils.getWaitDriver(this.driver);
        this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        this.driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public WebPageExtractorJSON(SelfHealingDriver driver, SelfHealingDriverWait wait) {
        // This constructor is for use within the test framework.
        this.driver = driver;
        this.wait = wait;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    private PageData extractPageData() {
        String currentUrl = driver.getCurrentUrl();
        Document doc = Jsoup.parse(driver.getPageSource());
        List<Component> components = new ArrayList<>();
        // Main semantic elements
        extractByTag(doc, driver, "form", "form", components);
        extractByTag(doc, driver, "nav", "navbar", components);
        extractByTag(doc, driver, "header", "header", components);
        extractByTag(doc, driver, "aside", "sidebar", components);
        extractByTag(doc, driver, "main", "main", components);
        extractByTag(doc, driver, "footer", "footer", components);
        // // Fallback: sections and large DIVs
        extractByTag(doc, driver, "div", "section", components);
        return new PageData(currentUrl, components);
    }

    public String extractComponentsAsJsonString() {
        PageData pageData = extractPageData();
        return this.gson.toJson(pageData);
    }

    public void runExtractor(String fileName) {
        PageData pageData = extractPageData();
        writeFile(pageData, fileName);
    }

    public String getFileName(String urlString) throws Exception {
        // Breaking URL to domain and locators
        URL url = new URI(urlString).toURL();
        String domain = url.getHost();
        String path = url.getPath();
        // Trim trailing slash if it's not the root path, to handle URLs like /login/
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        String pageName;
        if (path != null && !path.equals("/") && path.length() > 1) {
            // Get the last part of the path, e.g., "login" from "/auth/login"
            pageName = path.substring(path.lastIndexOf('/') + 1);
            // Also remove file extensions like .html or .php to get a cleaner name
            int lastDot = pageName.lastIndexOf('.');
            if (lastDot > 0) {
                pageName = pageName.substring(0, lastDot);
            }
        } else {
            // Use the first part of the domain, e.g., "google" from "google.com"
            pageName = domain.split("\\.")[0];
        }

        // Sanitize pageName for use in a filename
        pageName = pageName.replaceAll("[^a-zA-Z0-9\\-]", "_").toLowerCase();
        if (pageName.isEmpty()) {
            pageName = "index"; // Fallback for cases like "http://example.com/"
        }

        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("target/ExJson/page_components_%s_%s.json", pageName, timestamp);
        logger.info(String.format("Output file will be: %s", fileName));
        return fileName;
    }

    private void extractByTag(Document doc, WebDriver driver, String tag, String type,
            List<Component> components) {

        Elements elements = doc.select(tag);

        for (Element el : elements) {
            try {
                // Locate in Selenium DOM
                WebElement we;
                if (el.id().length() > 0) {
                    we = driver.findElement(By.id(el.id()));
                } else {
                    we = driver.findElement(By.xpath(getXPath(el)));
                }

                Component c = new Component();
                c.type = type;
                c.tag = el.tagName();
                c.text = el.ownText().trim();
                c.id = el.id();
                c.classes = el.className();
                c.role = el.attr("role");
                c.selector = buildSelector(el);
                if (el.attr("aria-label").length() > 0)
                    c.ariaLabel = el.attr("aria-label");

                // Collect all attributes
                el.attributes().forEach(attr -> c.attributes.put(attr.getKey(), attr.getValue()));

                // Bounding box
                Map<String, Integer> box = new HashMap<>();
                Point p = we.getLocation();
                Dimension d = we.getSize();
                box.put("x", p.getX());
                box.put("y", p.getY());
                box.put("width", d.getWidth());
                box.put("height", d.getHeight());
                if (we.isDisplayed())
                    c.boundingBox = box;

                // Extract actions (buttons, submit, links)
                Elements buttons = el.select("button, input[type=submit], input[type=button], input[type=reset], a");
                Elements inputs = el.select("input, textarea, select");
                fieldBuilder(buttons, c.actions, doc, null);
                fieldBuilder(inputs, c.fields, doc, new ArrayList<>(Arrays.asList("submit", "reset", "button")));

                // Avoid duplicates by selector+type
                boolean duplicateSelector = components.stream()
                        .anyMatch(existing -> existing.selector.equals(c.selector) && existing.type.equals(c.type));
                if (duplicateSelector) {
                    continue;
                }

                // Avoid nested duplicates (subset of another component’s fields+actions)
                boolean subset = components.stream().anyMatch(
                        existing -> existing.fields.containsAll(c.fields) && existing.actions.containsAll(c.actions));
                if (subset) {
                    continue;
                }

                // Only add if it's a "new" component
                components.add(c);

            } catch (org.openqa.selenium.NoSuchElementException e) {
                // This is expected if an element is in Jsoup's DOM but not visible/interactable
                // in Selenium's DOM
                // (e.g. because of 'display:none'). We can safely ignore it or log for
                // debugging.
                System.err.printf("Could not find element in Selenium DOM, skipping: %s. Reason: %s%n",
                        buildSelector(el), e.getMessage());
                logger.severe(String.format("Could not find element in Selenium DOM, skipping: %s. Reason: %s",
                        buildSelector(el), e.getMessage()));
            }
        }
    }

    private Set<Map<String, String>> fieldBuilder(Elements elements, Set<Map<String, String>> fieldSets,
            Document doc, ArrayList<String> ignoreTypes) {
        if (elements == null || elements.isEmpty()) {
            return fieldSets;
        }

        for (Element element : elements) {
            if (element.attr("type").equals("hidden") && !SELECT_HIDDEN) {
                continue; // Skip hidden elements unless explicitly selected
            }
            String type = element.attr("type");
            if (ignoreTypes != null && ignoreTypes.contains(type)) {
                continue;
            }

            Map<String, String> meta = new HashMap<>();
            meta.put("selector", buildSelector(element));
            meta.put("text", element.text().trim());
            meta.put("type", element.attr("type").isEmpty() ? element.tagName() : element.attr("type"));
            meta.put("name", element.attr("name"));
            meta.put("role", element.attr("role"));
            meta.put("value", element.attr("value"));
            meta.put("placeholder", element.attr("placeholder"));
            String label = findLabelText(doc, element);
            if (label != null && !label.isEmpty()) {
                meta.put("label", label);
            }

            if (element.tagName().equals("select")) {
                // Handle <select> elements
                meta.put("options", String.join(",", getSelectOptions(element)));
            }

            if (element.attr("href").length() > 0)
                meta.put("href", element.attr("href"));
            if (element.attr("aria-label").length() > 0)
                meta.put("aria-label", element.attr("aria-label"));

            fieldSets.add(meta);
        }
        return fieldSets;
    }

    private Set<String> getSelectOptions(Element selectElement) {
        Set<String> options = new HashSet<>();
        for (Element option : selectElement.children()) {
            if (option.tagName().equals("option")) {
                options.add(option.text().trim());
            }
        }
        return options;
    }

    private String buildSelector(Element el) {
        if (!el.id().isEmpty()) {
            // Chrome prefers ID because it's unique
            return "#" + el.id();
        } else if (!el.className().isEmpty() && USE_CSS_SELECTOR) {
            // multiple classes -> join with dots
            return el.tagName() + "." + el.className().trim().replace(" ", ".");
        } else {
            return getXPath(el);
        }
    }

    private String getXPath(Element el) {
        if (el == null)
            return null;

        // If element has an ID, DevTools returns that directly
        if (!el.id().isEmpty()) {
            return "//*[@id='" + el.id() + "']";
        }

        List<String> path = new ArrayList<>();
        Element current = el;

        while (current != null && !current.tagName().equals("html")) {
            // If we got a id then we can get it from there.
            if (!current.id().isEmpty()) {
                path.add(0, "//*[@id='" + current.id() + "']");
                // break; // ID is unique, no need to go further
                return String.join("/", path); // Return early if ID found
            }

            int index = 1;
            Element sibling = current.previousElementSibling();
            while (sibling != null) {
                if (sibling.tagName().equals(current.tagName())) {
                    index++;
                }
                sibling = sibling.previousElementSibling();
            }
            path.add(0, current.tagName() + "[" + index + "]");

            current = current.parent();
        }

        return "/html/" + String.join("/", path);
    }

    @SuppressWarnings("unused")
    private String getCssSelector(Element el, Document doc) {
        if (el == null)
            return null;

        // If element has an ID, Chrome uses CSS ID
        if (!el.id().isEmpty()) {
            return "#" + el.id();
        }

        List<String> parts = new ArrayList<>();
        Element current = el;

        while (current != null && !current.tagName().equals("html")) {
            StringBuilder part = new StringBuilder(current.tagName());

            // Add classes (first class only, DevTools often picks shortest unique form)
            if (!current.className().isEmpty()) {
                String[] classes = current.className().split("\\s+");
                for (String cls : classes) {
                    part.append(".").append(cls);
                }
            }

            // Add :nth-of-type() if needed
            int index = 1;
            Element sibling = current.previousElementSibling();
            while (sibling != null) {
                if (sibling.tagName().equals(current.tagName())) {
                    index++;
                }
                sibling = sibling.previousElementSibling();
            }
            if (index > 1) {
                part.append(":nth-of-type(").append(index).append(")");
            }

            parts.add(0, part.toString());

            String candidate = String.join(" > ", parts);

            // --- uniqueness check like Chrome does ---
            if (doc.select(candidate).size() == 1) {
                return candidate;
            }

            current = current.parent();
        }

        return String.join(" > ", parts);
    }

    private String findLabelText(Document doc, Element input) {
        String id = input.id();
        if (id != null && !id.isEmpty()) {
            Element label = doc.selectFirst("label[for=" + id + "]");
            if (label != null) {
                return label.text();
            }
        }
        String name = input.attr("name");
        if (name != null && !name.isEmpty()) {
            Element label = doc.selectFirst("label[for=" + name + "]");
            if (label != null) {
                return label.text();
            }
        }
        return "";
    }

    public String findFieldFromLabel(Document doc, String LabelStr) {
        Element label = doc.selectFirst("label:contains(" + LabelStr + ")");
        if (label != null) {
            String forAttr = label.attr("for");
            if (forAttr != null && !forAttr.isEmpty()) {
                return forAttr;
            }
        }
        return null;
    }

    public String findFieldFromLabel(SelfHealingDriver driver, String LabelStr) {
        Document doc = Jsoup.parse(driver.getPageSource());
        return findFieldFromLabel(doc, LabelStr);
    }

    private void writeFile(PageData pageData, String fileName) {
        // create directories if not exists
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("target/ExJson"));
        } catch (java.io.IOException e) {
            logger.severe(String.format("Failed to create directories: %s", e.getMessage()));
        }

        try (FileWriter fw = new FileWriter(fileName)) {
            this.gson.toJson(pageData, fw);
            logger.info(String.format("Extracted %d components -> %s", pageData.components.size(), fileName));
        } catch (Exception e) {
            logger.severe(String.format("Failed to write JSON file: %s", e.getMessage()));
        }
    }

    private static void login(SelfHealingDriver driver, SelfHealingDriverWait wait) {
        LoginPage loginPage = new LoginPage(driver, wait);
        driver.get(NAV_URL);
        loginPage.waitForPageLoad();
        loginPage.enterUsername(TestConfigs.getUsername());
        loginPage.enterPassword(TestConfigs.getPassword());
        loginPage.clickLoginButton();
        loginPage.verifyDashboard();
    }

    public static void main(String[] args) throws Exception {
        SelfHealingDriver driver = TestUtils.getDriver(TestConfigs.getBrowser());
        SelfHealingDriverWait wait = TestUtils.getWaitDriver(driver);

        driver.get(NAV_URL);
        if (LOGIN_REQUIRED)
            login(driver, wait);

        if (USE_COOKIE) {
            driver.manage().deleteAllCookies();
            driver.manage().deleteCookieNamed(COOKIE_NAME);

            driver.manage().addCookie(COOKIE);
            driver.navigate().refresh();

            // Debugging
            try {
                Thread.sleep(THREAD_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Wait for a key element to be present after reload, confirming login state
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            } catch (TimeoutException e) {
                logger.severe("Page did not reload correctly after setting cookie.");
            }

            logger.info("Navigated with cookies");
        }
        driver.navigate().to(NAV_URL);
        driver.navigate().to(NAV_URL);

        // logger.info("Waiting for page to be fully loaded...");
        // try {
        // new WebDriverWait(driver, Duration.ofSeconds(THREAD_DELAY / 1000))
        // .until(ExpectedConditions.jsReturnsValue("return document.readyState ==
        // 'complete'"));
        // } catch (TimeoutException e) {
        // logger.severe("Page did not finish loading within the timeout.");
        // }

        logger.info("Done waiting!");

        WebPageExtractorJSON extractor = new WebPageExtractorJSON(driver, TestUtils.getWaitDriver(driver));
        String fileName = extractor
                .getFileName(NAV_URL);
        extractor.runExtractor(fileName);
        driver.quit();
    }

}

/*
 * List of All the functions available to Public
 * 
 * WebPageExtractorJSON(driver, waitDriver) : To use with other components
 * WebPageExtractorJSON(driver, waitDriver) : Build for standalone use.
 * getFileName(String url) -> String
 * runExtractor(String fileName) -> void : will write the components to JSON
 * file in filename
 * findFieldFromLabel(Document doc, String LabelStr) -> String
 * findFieldFromLabel(SelfHealingDriver driver, String LabelStr) -> String
 * extractComponentsAsJsonString() -> String : Extract components and returns
 * JSON string
 * getFileName(String URL) -> String : Generate a filename based on the URL
 * findFieldFromLabel(Document doc, String LabelStr) -> String : Find the form
 * field associated with a label in the document
 * findFieldFromLabel(SelfHealingDriver driver, String LabelStr) -> String :
 * Find the form field associated with a label using the driver
 * 
 * List of All the functions available to private
 * 
 * extractByTag(Document doc, WebDriver driver, String tag, String type,
 * List<Component> components) -> void : Extract components by HTML tag
 * writeFile(PageData pageData, String fileName) -> void : Write
 * components to a JSON file
 * getXPath(Element el) -> String : Generate an XPath expression for a given
 * element
 * buildSelector(Element el) -> String : Build a CSS selector for a given
 * element
 * extractPageData() -> PageData : Extract all components from the
 * current page
 * fieldBuilder(Elements elements, Set<Map<String, String>> fields) -> void :
 * Build form fields from the given elements
 * getCssSelector(Element el, Document doc) -> String : Generate a CSS selector
 * for a given element
 * findLabelText(Document doc, String label) -> String : Find the visible text
 * associated with a label in the document
 */