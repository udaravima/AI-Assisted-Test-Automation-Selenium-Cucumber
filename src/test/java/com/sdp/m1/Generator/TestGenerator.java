package com.sdp.m1.Generator;

import com.google.gson.Gson;
import com.sdp.m1.Extractors.WebPageExtractorJSON;
import com.sdp.m1.Generator.Model.CorrelatedField;
import com.sdp.m1.Generator.Model.PageData;
import com.sdp.m1.Generator.Model.Srs;
import com.sdp.m1.Generator.Model.UiComponent;
import com.sdp.m1.Pages.LoginPage;
import com.sdp.m1.Utils.TestConfigs;
import com.sdp.m1.Utils.TestUtils;
import com.epam.healenium.SelfHealingDriver;
import com.epam.healenium.SelfHealingDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.List;

public class TestGenerator {
    private static final Logger logger = Logger.getLogger(TestGenerator.class.getName());
    private static final Boolean LOGIN_NEEDED = true;

    public static void main(String[] args) throws Exception {
        // 1. Configuration from command-line arguments
        String srsPath = null;
        String targetUrl = null;
        String name = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--srs":
                    srsPath = args[++i];
                    break;
                case "--url":
                    targetUrl = args[++i];
                    break;
                case "--name":
                    name = args[++i];
                    break;
            }
        }

        if (srsPath == null || targetUrl == null || name == null) {
            System.err.println(
                    "Usage: java TestGenerator --srs <path_to_srs.json> --url <target_url> --name <FeatureName>");
            System.exit(1);
        }

        // Generate output paths based on the name
        String featureName = name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(); // Convert PascalCase to
                                                                                       // snake_case
        String className = name.substring(0, 1).toUpperCase() + name.substring(1);

        String featureOutputPath = "src/test/resources/Features/Generated_" + featureName + ".feature";
        String stepsOutputPath = "src/test/java/com/sdp/m1/Steps/Generated/Generated" + className + "Steps.java";

        logger.info("Starting test generation process...");
        logger.info("SRS Path: " + srsPath);
        logger.info("Target URL: " + targetUrl);
        logger.info("Feature Name: " + name);
        logger.info("Feature Output Path: " + featureOutputPath);
        logger.info("Steps Output Path: " + stepsOutputPath);

        // 2. Initialize Browser and Extractor
        SelfHealingDriver driver = TestUtils.getDriver(TestConfigs.getBrowser());
        SelfHealingDriverWait wait = TestUtils.getWaitDriver(driver);

        if (LOGIN_NEEDED) {
            login(driver, wait);
        }

        driver.get(targetUrl);
        driver.get(targetUrl); // Navigate twice to ensure full load
        WebPageExtractorJSON extractor = new WebPageExtractorJSON(driver, wait);

        // 3. Load and Parse Inputs
        String srsJsonContent = new String(Files.readAllBytes(Paths.get(srsPath)));
        String pageComponentsJson = extractor.extractComponentsAsJsonString();

        Gson gson = new Gson();
        Srs srsData = gson.fromJson(srsJsonContent, Srs.class);
        PageData pageData = gson.fromJson(pageComponentsJson, PageData.class);
        List<UiComponent> componentData = pageData.components;

        // 4. Correlate SRS with UI Components
        List<CorrelatedField> correlatedData = correlate(srsData, componentData);
        logger.info(String.format("Correlation complete. Found %d mappable fields.", correlatedData.size()));
        correlatedData.forEach(field -> logger.info(String.format("Mapped SRS field '%s' to UI element '%s' via %s",
                field.srsRequirement.label, field.uiField.selector, field.matchType)));

        // 5. Generate Test Artifacts
        String featureFileContent = generateFeatureFile(srsData, correlatedData);
        writeFile(featureOutputPath, featureFileContent);
        logger.info("Generated feature file: " + featureOutputPath);

        String stepDefsContent = generateStepDefinitions(correlatedData, className);
        writeFile(stepsOutputPath, stepDefsContent);
        logger.info("Generated step definitions: " + stepsOutputPath);

        driver.quit();
        logger.info("Test generation finished successfully.");
    }

    private static void login(SelfHealingDriver driver, SelfHealingDriverWait wait) {
        LoginPage loginPage = new LoginPage(driver, wait);
        driver.get(TestConfigs.getBaseUrl());
        loginPage.waitForPageLoad();
        loginPage.enterUsername(TestConfigs.getUsername());
        loginPage.enterPassword(TestConfigs.getPassword());
        loginPage.clickLoginButton();
        loginPage.verifyDashboard();
    }

    private static List<CorrelatedField> correlate(Srs srsData, List<UiComponent> uiComponents) {
        List<CorrelatedField> correlatedFields = new ArrayList<>();
        List<UiComponent.Field> allUiFields = new ArrayList<>();
        if (uiComponents != null) {
            uiComponents.forEach(component -> {
                if (component.fields != null) {
                    allUiFields.addAll(component.fields);
                }
            });
        }

        logger.info("Starting correlation...");
        logger.info("SRS Requirements: " + srsData.requirements.size());
        logger.info("UI Fields: " + allUiFields.size());

        for (Srs.Requirement req : srsData.requirements) {
            logger.info("Processing SRS requirement: " + req.label + " (id: " + req.id + ")");
            boolean matched = false;

            // Pass 1: Match by ID
            for (UiComponent.Field uiField : allUiFields) {
                logger.info("  Comparing with UI field (selector): " + uiField.selector);
                if (uiField.selector != null && uiField.selector.equals("#" + req.id)) {
                    logger.info("  Matched by ID: " + req.id + " with selector " + uiField.selector);
                    addCorrelation(correlatedFields, req, uiField, CorrelatedField.MatchType.ID);
                    matched = true;
                    break;
                }
            }
            if (matched)
                continue;

            // Pass 2: Match by 'name' attribute
            for (UiComponent.Field uiField : allUiFields) {
                logger.info("  Comparing with UI field (name): " + uiField.name);
                if (uiField.name != null && uiField.name.equals(req.id)) {
                    logger.info("  Matched by name: " + req.id + " with name " + uiField.name);
                    addCorrelation(correlatedFields, req, uiField, CorrelatedField.MatchType.NAME);
                    matched = true;
                    break;
                }
            }
            if (matched)
                continue;

            // Pass 3: Match by label text (normalized)
            for (UiComponent.Field uiField : allUiFields) {
                if (req.label != null && !req.label.isEmpty() && uiField.label != null && !uiField.label.isEmpty()) {
                    // Normalize both labels for a more robust comparison
                    String srsLabel = req.label.toLowerCase().trim().replaceAll(":$", "");
                    String uiLabel = uiField.label.toLowerCase().trim().replaceAll(":$", "");

                    logger.info("  Comparing normalized labels: '" + srsLabel + "' vs '" + uiLabel + "'");

                    if (srsLabel.equals(uiLabel)) {
                        logger.info("  Matched by label: " + req.label + " with label " + uiField.label);
                        addCorrelation(correlatedFields, req, uiField, CorrelatedField.MatchType.LABEL);
                        matched = true;
                        break;
                    }
                }
            }
        }
        return correlatedFields;
    }

    private static void addCorrelation(List<CorrelatedField> list, Srs.Requirement req, UiComponent.Field uiField,
            CorrelatedField.MatchType type) {
        CorrelatedField correlatedField = new CorrelatedField();
        correlatedField.srsRequirement = req;
        correlatedField.uiField = uiField;
        correlatedField.matchType = type;
        list.add(correlatedField);
    }

    private static String generateFeatureFile(Srs srsData, List<CorrelatedField> correlatedData) {
        StringBuilder featureBuilder = new StringBuilder();
        featureBuilder.append("Feature: ").append(srsData.feature).append("\n\n");
        featureBuilder.append("  ").append(srsData.featureDescription).append("\n\n");
        featureBuilder.append("  Scenario: Fill and submit the service provider registration form\n");

        for (CorrelatedField field : correlatedData) {
            featureBuilder.append("    When I fill the '" + field.srsRequirement.label + "' with a valid value\n");
        }

        featureBuilder.append("    And I click the 'Submit' button\n");
        featureBuilder.append("    Then I should see a success message\n");

        return featureBuilder.toString();
    }

    private static String generateStepDefinitions(List<CorrelatedField> correlatedData, String className) {
        StringBuilder stepsBuilder = new StringBuilder();
        String header = "package com.sdp.m1.Steps.Generated;\n\n" +
                "import io.cucumber.java.en.When;\n" +
                "import io.cucumber.java.en.Given;\n" +
                "import io.cucumber.java.en.Then;\n" +
                "import io.cucumber.java.en.And;\n\n" +
                "import org.openqa.selenium.By;\n" +
                "import org.openqa.selenium.NoSuchElementException;\n" +
                "import org.openqa.selenium.TimeoutException;\n" +
                "import org.openqa.selenium.WebElement;\n" +
                "import org.openqa.selenium.support.ui.ExpectedConditions;\n\n" +
                "import com.epam.healenium.SelfHealingDriverWait;\n" +
                "import com.epam.healenium.SelfHealingDriver;\n\n" +
                "import com.sdp.m1.Utils.TestUtils;\n" +
                "import com.sdp.m1.Utils.TestConfigs;\n\n";
        stepsBuilder.append(header);

        String class_body = "public class Generated" + className + "Steps {\n\n" +
                "    private final SelfHealingDriver driver;\n" +
                "    private final SelfHealingDriverWait wait;\n" +
                "    private String browserType = TestConfigs.getBrowser();\n" +
                "\n" +
                "    public Generated" + className + "Steps(SelfHealingDriver driver, SelfHealingDriverWait wait) {\n" +
                "        this.driver = driver;\n" +
                "        this.wait = wait;\n" +
                "    }\n\n";
        stepsBuilder.append(class_body);

        // for (CorrelatedField field : correlatedData) {
        // stepsBuilder.append(" @When(\"I fill the '" + field.srsRequirement.label +
        // "'\n");
        // stepsBuilder.append(" public void i_fill_the_")
        // .append(field.srsRequirement.label.replaceAll("[^a-zA-Z0-9]",
        // "_").toLowerCase())
        // .append("_with_a_valid_value() {\n");
        // stepsBuilder.append(" driver.findElement(By.cssSelector(\"" +
        // field.uiField.selector
        // + "\")).sendKeys(\"some_value\");\n");
        // stepsBuilder.append(" }

        // ");
        // }

        stepsBuilder.append("}    \n");
        return stepsBuilder.toString();
    }

    private static String generatePageObjectModel(Srs srsData, List<CorrelatedField> correlatedData) {
        StringBuilder pomBuilder = new StringBuilder();
        pomBuilder.append("package com.sdp.m1.PageObjects;\n\n");
        pomBuilder.append("import org.openqa.selenium.By;\n");
        pomBuilder.append("import org.openqa.selenium.WebDriver;\n");
        pomBuilder.append("import org.openqa.selenium.WebElement;\n");
        pomBuilder.append("import org.openqa.selenium.support.PageFactory;\n");
        pomBuilder.append("import org.openqa.selenium.support.FindBy;\n");
        pomBuilder.append("import org.openqa.selenium.support.ui.ExpectedConditions;\n");
        pomBuilder.append("import org.openqa.selenium.support.ui.WebDriverWait;\n");
        pomBuilder.append("import java.util.List;\n");
        pomBuilder.append("import java.util.stream.Collectors;\n");
        pomBuilder.append("\n");
        pomBuilder.append("public class ServiceProviderRegistrationPage {\n");
        pomBuilder.append("    private WebDriver driver;\n");
        pomBuilder.append("    private WebDriverWait wait;\n");
        pomBuilder.append("\n");
        pomBuilder.append("    public ServiceProviderRegistrationPage(WebDriver driver) {\n");
        pomBuilder.append("        this.driver = driver;\n");
        pomBuilder.append("        this.wait = new WebDriverWait(driver, 10);\n");
        pomBuilder.append("        PageFactory.initElements(driver, this);\n");
        pomBuilder.append("    }\n");
        pomBuilder.append("\n");

        for (CorrelatedField field : correlatedData) {
            pomBuilder.append("    @FindBy(css = \"" + field.uiField.selector + "\")\n");
            pomBuilder.append("    private WebElement "
                    + field.srsRequirement.label.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase() + ";\n");
        }

        pomBuilder.append("\n");
        pomBuilder.append("    public void fillForm() {\n");
        for (CorrelatedField field : correlatedData) {
            pomBuilder.append("        " + field.srsRequirement.label.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase()
                    + ".sendKeys(\"some_value\");\n");
        }
        pomBuilder.append("    }\n");
        pomBuilder.append("}\n");

        return pomBuilder.toString();
    }

    private static void writeFile(String path, String content) throws IOException {
        java.nio.file.Path p = Paths.get(path);
        java.nio.file.Files.createDirectories(p.getParent());
        Files.write(p, content.getBytes());
    }
}
