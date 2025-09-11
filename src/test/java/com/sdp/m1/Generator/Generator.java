package com.sdp.m1.Generator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Command(name = "generator", mixinStandardHelpOptions = true, version = "generator 1.0", description = "Parse SRS to JSON & Generate a master prompt for creating automated test artifacts.")
public class Generator implements Runnable {

    @Option(names = "--srs2json", description = "If provided, convert a PDF SRS to JSON.")
    private String srs2json;

    @Option(names = "--jsrs", description = "Path to the SRS JSON file containing feature requirements.")
    private String jsrs;

    @Option(names = "--ui", arity = "1..*", description = "Path(s) to the JSON file(s) containing the extracted UI components.")
    private List<String> ui;

    @Option(names = "--prefix", defaultValue = "src/test/java/com/sdp/m1", description = "Optional prefix for the prompt for examples\nDefault will be : `src/test/java/com/sdp/m1`.")
    private String prefix;

    @Option(names = "--separate", description = "Path to a JSON file to be separated into individual section files.")
    private String separate;
    
    @Option(names = "--seperate", description = "If true, separates the generated SRS JSON into individual section files.")
    private boolean seperate = false;

    private static final String MASTER_PROMPT_TEMPLATE = """
            ROLE: You are an expert QA Automation Engineer. Your expertise is in creating robust, maintainable, and comprehensive test suites using Java 21, Selenium 4, Cucumber 7, and Maven. You adhere strictly to the Page Object Model and BDD best practices.

            ---
            CODE STYLE AND STRUCTURE EXAMPLES:
            You MUST generate code that strictly follows the style, patterns, and conventions of the examples below.

            **1. Feature File Example (`service_provider_registration.feature`):**
            Note the use of Background, Scenario Outlines, and Examples tables.
            ```gherkin
            {feature_example}
            ```

            **2. Page Object Class Example (`ServiceProviderRegistrationPage.java`):**
            Note the constructor, @FindBy annotations, private WebElements, and public methods for interactions and assertions. All Selenium calls are encapsulated here.
            ```java
            {page_object_example}
            ```

            **3. Step Definition Class Example (`ServiceProviderRegistrationSteps.java`):**
            Note the dependency injection in the constructor, how it calls methods on the Page Object, and that it contains NO Selenium `driver` calls.
            ```java
            {steps_example}
            ```

            **4. Configuration Utility (`TestConfigs.java`):**
            Use these static methods to get configuration values like URLs and credentials. Do NOT hardcode them.
            Available methods include: `getBaseUrl()`, `getAdminUsername()`, `getAdminPassword()`, `getBrowser()`.
            ```java
            {configs_example}
            ```

            **5. General Utility (`TestUtils.java`):**
            Use these static methods for common tasks like getting the driver instance (`TestUtils.getDriver()`) or creating explicit waits (`TestUtils.getWaitDriver()`)
            ```java
            {utils_example}
            ```

            **6. Hooks (`Hooks.java`):**
            This file contains global setup and teardown logic that runs before and after each scenario.
            - **`@Before`**: A new scenario is logged. The WebDriver is initialized lazily.
            - **`@After`**: The scenario result is logged. A screenshot is automatically taken on failure (or success, if configured). Most importantly, the WebDriver is **always closed and cleaned up**.
            - **IMPLICATION**: Do NOT generate any code for taking screenshots or closing the driver (`driver.quit()`, `driver.close()`) in the Step Definitions, as it is handled automatically by the hooks.

            ```java
            {hooks_example}
            ```
            ---

            TASK:
            Generate a complete and correct set of test automation artifacts for the given feature. The output must be three separate, complete code blocks for the following files:
            1.  A Cucumber `.feature` file.
            2.  A Java Page Object class.
            3.  A Java Step Definitions class.

            INSTRUCTIONS:

            **1. Correlate Requirements to UI:**
            *   For each field in the **SRS JSON**, find its corresponding element in the **Page Structure JSON**.
            *   Use a multi-pass strategy:
                1.  Attempt to match the SRS field key (e.g., `ServiceProviderID`) directly with an element's `id` or `name` or `selector` attribute.
                2.  If no match, perform a case-insensitive, semantic match between the SRS field key/description and the element's visible `label` text.
            *   If a clear mapping cannot be found, add a `// TODO: Manual locator needed` comment in the generated Page Object.

            **2. Generate the `.feature` File:**
            *   Create a `Feature:` and `Background:` section that clearly describes the user story.
            *   For each field in the SRS, generate scenarios for the happy path, mandatory validation, and format/length validation based on the `validation` and `errorResponses` objects in the SRS.
            *   Use `Scenario Outlines` for validation tests.

            **3. Generate the Java Page Object Class:**
            *   The class name must end with `Page`.
            *   It **must** have a constructor that accepts `SelfHealingDriver` and `SelfHealingDriverWait`.
            *   Define all UI elements as private `WebElement` fields with `@FindBy` annotations.
            *   Encapsulate all Selenium actions (`.sendKeys()`, `.click()`) in public methods.

            **4. Generate the Java Step Definitions Class:**
            *   The class name must end with `Steps`.
            *   The constructor **must** accept the Page Object class for dependency injection.
            *   Step definition methods **must not** contain any `driver.findElement` or Selenium calls. They should only call methods on the Page Object instance.
            *   Use JUnit 5 `Assertions.assertEquals` for assertions.

            ---
            HERE IS THE CONTEXT FOR THE NEW FEATURE:

            **SRS JSON:**
            ```json
            {srs_json}
            ```

            **Page Structure JSON:**
            ```json
            {ui_json}
            ```
            ---
            YOUR OUTPUT:
            Provide three separate, complete, and immediately usable code blocks for the following files:
            1. A new `.feature` file.
            2. A new Java Page Object class.
            3. A new Java Step Definitions class.
            """;

    private static final String MASTER_PARSER_PROMPT = """
            ROLE: You are an Quality Assurance Engineer and a System Requirement Analysis that parse SRS Document into JSON. Produce STRICT, VALID JSON only.

            Goal:
            - Convert an SRS excerpt into a hierarchical JSON schema with Sections → Sub_Sections (nested).
            - If a parent subsection (e.g., “Service Provider Profile Management”) lists actions like Register/Edit/Search that are described in their own numbered child subsections (e.g., 2.1.1, 2.1.2, 2.1.3), then:
              - Keep the parent as a SUMMARY (epic), DO NOT duplicate its fields.
              - Create separate child nodes (Type: \"Action\") for each leaf subsection and attach their Requirements, Fields, Validation_Rules, UI_Elements, and Flows to the appropriate leaf.

            Rules:
            - Preserve all section numbers exactly (e.g., \"2.1.1\").
            - Each requirement must have REQ_ID and Description.
            - Extract tables of fields into \"Fields\" with Constraints and Validation_Rules (mandatory, lengths, patterns, ranges, allowed values, and error responses).
            - If the SRS references related subsections, fill \"Related_Sub_Sections\" with their IDs.
            - If UI identifiers (id/xpath/name/aria-label) are present in the text, place them under \"UI_Elements\".
            - If procedural steps exist (like \"navigate → fill → submit → verify\"), create a \"Flows\" array describing them at the leaf node.
            - Keep arrays even if empty.
            - Do not invent data; if missing, leave nulls or empty arrays.

            Input:
            {context}

            Example Output:
            [
              {{
                    \"Section_ID\": \"2\",
                \"Section_Name\": \"Provisioning Module\",
                \"Sub_Sections\": [
                  {{
                        \"Sub_Section_ID\": \"2.1\",
                        \"Sub_Section_Name\": \"Service Provider Profile Management\",
                        \"Requirements\": [
                            {{
                                \"REQ_ID\": \"REQ-SP-PRO-1\",
                                \"Description\": \"SP SLA is an agreement between SDP and service provider which should be enforced before the application SLA during provisioning.\" 
                            }},
                            {{
                                \"REQ_ID\": \"REQ-SP-PRO-4\",
                                \"Description\": \"The SP provisioning UI shall allow users to perform actions based on access rights.\",
                                \"Actions\": [\"Register new SP\", \"View/Edit SP profile\", \"Search SPs\"],
                                \"Related_Sub_Sections\": [
                                    {{
                                        \"Sub_Section_ID\": \"2.1.1\",
                                        \"Sub_Section_Name\": \"Register New Service Provider\"
                                    }},
                                    {{
                                        \"Sub_Section_ID\": \"2.1.1.1\",
                                        \"Sub_Section_Name\": \"Configuration of SLA for SMS\"
                                    }}
                                ]
                            }}
                        ],
                        \"Fields\": [
                            {{
                                \"Field_Name\": \"SP Name\",
                                \"Type\": \"Text\",
                                \"Validation\": \"Mandatory, max 50 characters\",
                                \"Error_Response\": \"Service Provider Name is required\"
                            }},
                            {{
                                \"Field_Name\": \"SP ID\",
                                \"Type\": \"Alphanumeric\",
                                \"Validation\": \"13 characters required\",
                                \"Error_Response\": \"Invalid Service Provider ID\"
                            }}
                        ]
                    }}
                ]
              }}
            ]

            Output:
            [JSON Format]

            """;

    @Override
    public void run() {
        if (srs2json != null) {
            srsToJson(srs2json, seperate);
        } else if (separate != null) {
            separateSrsJson(separate, null);
        } else {
            if (jsrs == null) {
                System.err.println("Without --srs2json or --separate, --jsrs is required.");
                new CommandLine(this).usage(System.err);
                return;
            }
            String prompt = generateTestPrompt(jsrs, ui, prefix);
            if (prompt != null) {
                System.out.println(prompt);
            }
        }
    }

    String generateTestPrompt(String srsJsonPath, List<String> uiJsonPaths, String prefix) {
        try {
            String projectRoot = System.getProperty("user.dir");

            String featureExample = readFileContent(Paths
                    .get(projectRoot, "src/test/resources/Features/service_provider_registration.feature").toString());
            String pageObjectExample = readFileContent(
                    Paths.get(projectRoot, prefix, "Pages/ServiceProviderRegistrationPage.java").toString());
            String stepsExample = readFileContent(
                    Paths.get(projectRoot, prefix, "Steps/ServiceProviderRegistrationSteps.java").toString());
            String configsExample = readFileContent(
                    Paths.get(projectRoot, prefix, "Utils/TestConfigs.java").toString());
            String utilsExample = readFileContent(Paths.get(projectRoot, prefix, "Utils/TestUtils.java").toString());
            String hooksExample = readFileContent(Paths.get(projectRoot, prefix, "Hooks/Hooks.java").toString());

            String srsContent = readFileContent(srsJsonPath);
            String uiContent = "";
            if (uiJsonPaths != null && !uiJsonPaths.isEmpty()) {
                StringBuilder uiContentBuilder = new StringBuilder();
                for (String uiPath : uiJsonPaths) {
                    uiContentBuilder.append(readFileContent(uiPath)).append("\n");
                }
                uiContent = uiContentBuilder.toString();
            }

            return MASTER_PROMPT_TEMPLATE
                    .replace("{feature_example}", featureExample)
                    .replace("{page_object_example}", pageObjectExample)
                    .replace("{steps_example}", stepsExample)
                    .replace("{configs_example}", configsExample)
                    .replace("{utils_example}", utilsExample)
                    .replace("{hooks_example}", hooksExample)
                    .replace("{srs_json}", srsContent)
                    .replace("{ui_json}", uiContent);

        } catch (IOException e) {
            System.err.println("Error reading file for prompt generation: " + e.getMessage());
            return null;
        }
    }

    String readFileContent(String filePath) throws IOException {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Warning: Could not read file at " + filePath + ". Prompt will be less detailed.");
            // Return a placeholder instead of throwing, to match python script's behavior
            return "// Example file not found at: " + filePath;
        }
    }

    private void srsToJson(String pdfPath, boolean seperate) {
        System.out.println("Converting SRS PDF at " + pdfPath + " to JSON...");

        String pdfText = pdfExtraction(pdfPath);
        if (pdfText == null) {
            return;
        }

        String response = runModel(MASTER_PARSER_PROMPT.replace("{context}", pdfText));
        String outputPath = pdfPath.substring(0, pdfPath.lastIndexOf('.')) + ".json";
        try {
            Files.writeString(Paths.get(outputPath), response);
            if (seperate) {
                separateSrsJson(outputPath, response);
            }
            System.out.println("SRS JSON saved to " + outputPath);
        } catch (IOException e) {
            System.err.println("Error writing JSON to file: " + e.getMessage());
        }
    }

    private void separateSrsJson(String jsonPath, String dataStr) {
        try {
            JsonArray sections;
            if (dataStr != null) {
                sections = new Gson().fromJson(dataStr, JsonArray.class);
            } else {
                String content = new String(Files.readAllBytes(Paths.get(jsonPath)));
                sections = new Gson().fromJson(content, JsonArray.class);
            }

            String outputDir = jsonPath.substring(0, jsonPath.lastIndexOf('.')) + "_sections";
            Files.createDirectories(Paths.get(outputDir));

            for (JsonElement sectionElement : sections) {
                JsonObject section = sectionElement.getAsJsonObject();
                String sectionId = section.has("Section_ID") ? section.get("Section_ID").getAsString() : "N/A";
                String sectionName = section.has("Section_Name")
                        ? section.get("Section_Name").getAsString().replace(" ", "_")
                        : "Unnamed";
                String filename = Paths.get(outputDir, sectionId + "-" + sectionName + ".json").toString();
                try (Writer writer = Files.newBufferedWriter(Paths.get(filename))) {
                    new Gson().toJson(section, writer);
                    System.out.println("Created " + filename);
                } catch (IOException e) {
                    System.err.println("Error writing separated JSON file: " + e.getMessage());
                }
            }
            System.out.println("Separated JSON sections into " + outputDir);
        } catch (IOException e) {
            System.err.println("Error separating JSON: " + e.getMessage());
        }
    }

    private String pdfExtraction(String pdfPath) {
        System.out.println("Extracting text from PDF at " + pdfPath + "...");
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            System.err.println("Error extracting PDF text: " + e.getMessage());
            return null;
        }
    }

    private String runModel(String prompt) {
        String apiKey = System.getenv("OPEN_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: OPEN_API_KEY environment variable not set.");
            return null;
        }
        OpenAIClient openAI = OpenAIOkHttpClient.fromEnv();

        ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_5_MINI)
                .addDeveloperMessage(prompt)
                .build();

        try {
            return openAI.chat().completions().create(createParams).choices().getFirst().message().content().toString();
        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Generator()).execute(args);
        System.exit(exitCode);
    }
}