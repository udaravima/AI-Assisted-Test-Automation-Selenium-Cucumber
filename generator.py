import argparse
import json
import os
import pdfplumber
from openai import OpenAI, api_key
from dotenv import load_dotenv
# The Refactored Master Prompt, which now includes placeholders for code examples.
MASTER_PROMPT_TEMPLATE = """ROLE: You are an expert QA Automation Engineer. Your expertise is in creating robust, maintainable, and comprehensive test suites using Java 21, Selenium 4, Cucumber 7, and Maven. You adhere strictly to the Page Object Model and BDD best practices.

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
Use these static methods for common tasks like getting the driver instance (`TestUtils.getDriver()`) or creating explicit waits (`TestUtils.getWaitDriver()`).
```java
{utils_example}
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
"""
MASTER_PARSER_PROMPT = """ROLE: You are an Quality Assurance Engineer and a System Requirement Analysis that parse SRS Document into JSON. Produce STRICT, VALID JSON only.

Goal:
- Convert an SRS excerpt into a hierarchical JSON schema with Sections → Sub_Sections (nested).
- If a parent subsection (e.g., “Service Provider Profile Management”) lists actions like Register/Edit/Search that are described in their own numbered child subsections (e.g., 2.1.1, 2.1.2, 2.1.3), then:
  - Keep the parent as a SUMMARY (epic), DO NOT duplicate its fields.
  - Create separate child nodes (Type: "Action") for each leaf subsection and attach their Requirements, Fields, Validation_Rules, UI_Elements, and Flows to the appropriate leaf.

Rules:
- Preserve all section numbers exactly (e.g., "2.1.1").
- Each requirement must have REQ_ID and Description.
- Extract tables of fields into "Fields" with Constraints and Validation_Rules (mandatory, lengths, patterns, ranges, allowed values, and error responses).
- If the SRS references related subsections, fill "Related_Sub_Sections" with their IDs.
- If UI identifiers (id/xpath/name/aria-label) are present in the text, place them under "UI_Elements".
- If procedural steps exist (like “navigate → fill → submit → verify”), create a "Flows" array describing them at the leaf node.
- Keep arrays even if empty.
- Do not invent data; if missing, leave nulls or empty arrays.

Input:
{context}

Example Output:
[
  {{
        "Section_ID": "2",
    "Section_Name": "Provisioning Module",
    "Sub_Sections": [
      {{
            "Sub_Section_ID": "2.1",
            "Sub_Section_Name": "Service Provider Profile Management",
            "Requirements": [
                {{
                    "REQ_ID": "REQ-SP-PRO-1",
                    "Description": "SP SLA is an agreement between SDP and service provider which should be enforced before the application SLA during provisioning."
                }},
                {{
                    "REQ_ID": "REQ-SP-PRO-4",
                    "Description": "The SP provisioning UI shall allow users to perform actions based on access rights.",
                    "Actions": ["Register new SP", "View/Edit SP profile", "Search SPs"],
                    "Related_Sub_Sections": [
                        {{
                            "Sub_Section_ID": "2.1.1",
                            "Sub_Section_Name": "Register New Service Provider"
                        }},
                        {{
                            "Sub_Section_ID": "2.1.1.1",
                            "Sub_Section_Name": "Configuration of SLA for SMS"
                        }}
                    ]
                }}
            ],
            "Fields": [
                {{
                    "Field_Name": "SP Name",
                    "Type": "Text",
                    "Validation": "Mandatory, max 50 characters",
                    "Error_Response": "Service Provider Name is required"
                }},
                {{
                    "Field_Name": "SP ID",
                    "Type": "Alphanumeric",
                    "Validation": "13 characters required",
                    "Error_Response": "Invalid Service Provider ID"
                }}
            ]
        }}
    ]
  }}
]

Output:
[JSON Format]

"""


def run_model(prompt):
    """
    Runs the AI model with the given prompt and returns the response.
    """
    response = model.chat.completions.create(
        model="gpt-5-mini",
        messages=[
            {"role": "system", "content": prompt}
        ],
        # max_tokens=4000,
        # temperature=0.6,
    )
    # print Usage
    print(response.usage)
    return response.choices[0].message.content


def pdf_extraction(pdf_path):
    """
    Placeholder function to extract text from a PDF.
    """
    print(f"Extracting text from PDF at {pdf_path}...")
    try:
        with pdfplumber.open(pdf_path) as pdf:
            text = ""
            for page in pdf.pages:
                text += page.extract_text() + "\n"
            return text
    except FileNotFoundError:
        print(f"Error: PDF file not found at {pdf_path}")
        return None
    except Exception as e:
        print(f"Error extracting PDF text: {e}")
        return None


def read_file_content(base_path, file_path):
    """Safely reads content of a file."""
    full_path = os.path.join(base_path, file_path)
    try:
        with open(full_path, 'r', encoding='utf-8') as f:
            return f.read()
    except FileNotFoundError:
        print(
            f"Warning: Example file not found at {full_path}. Prompt will be less detailed.")
        return f"// Example file not found at: {file_path}"
    except Exception as e:
        print(f"Warning: Error reading {full_path}: {e}")
        return f"// Error reading example file: {file_path}"


def generate_test_prompt(srs_json_path, ui_json_path, prefix="src/test/java/com/sdp/m1"):
    """
    Generates a comprehensive prompt for AI-powered test generation.

    Args:
        srs_json_path (str): Path to the SRS JSON file.
        ui_json_path (str): Path to the UI components JSON file.

    Returns:
        str: The formatted master prompt with all context included.
    """
    try:
        # Assuming the script is run from the project root.
        project_root = os.getcwd()

        # Read example files
        feature_example = read_file_content(
            project_root, 'src/test/resources/Features/service_provider_registration.feature.example')
        page_object_example = read_file_content(
            project_root, f'{prefix}/Pages/ServiceProviderRegistrationPage.java.example')
        steps_example = read_file_content(
            project_root, f'{prefix}/Steps/ServiceProviderRegistrationSteps.java.example')
        configs_example = read_file_content(
            project_root, f'{prefix}/Utils/TestConfigs.java')
        utils_example = read_file_content(
            project_root, f'{prefix}/Utils/TestUtils.java')

        # Read task-specific files
        with open(srs_json_path, 'r', encoding='utf-8') as f:
            srs_content = f.read()

        with open(ui_json_path, 'r', encoding='utf-8') as f:
            ui_content = f.read()

        # Format the master prompt with all the context
        final_prompt = MASTER_PROMPT_TEMPLATE.format(
            feature_example=feature_example,
            page_object_example=page_object_example,
            steps_example=steps_example,
            configs_example=configs_example,
            utils_example=utils_example,
            srs_json=srs_content,
            ui_json=ui_content
        )

        return final_prompt
    except FileNotFoundError as e:
        print(f"Error: Input file not found - {e}")
        return None
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
        return None


def srs_to_json(pdf_path):
    """
    Placeholder function to convert SRS PDF to JSON.
    """
    print(f"Converting SRS PDF at {pdf_path} to JSON...")

    pdf_text = pdf_extraction(pdf_path)
    if not pdf_text:
        return None

    response = run_model(MASTER_PARSER_PROMPT.format(context=pdf_text))
    output_path = os.path.splitext(pdf_path)[0] + ".json"
    with open(output_path, 'w', encoding='utf-8') as f:
        # json.dump(response, f, indent=4)
        if response:
            f.write(response)
        else:
            print("Error: No response from model.")
            return None

    print(f"SRS JSON saved to {output_path}")

    return output_path


def main():
    """
    Main function to generate a comprehensive prompt for AI-powered test generation.
    """

    parser = argparse.ArgumentParser(
        description="Parse SRS to JSON & Generate a master prompt for creating automated test artifacts."
    )
    parser.add_argument(
        '--srs2json',
        type=str,
        help="If provided, convert a PDF SRS to JSON."
    )
    parser.add_argument(
        "--jsrs",
        type=str,
        help="Path to the SRS JSON file containing feature requirements."
    )
    parser.add_argument(
        "--ui",
        type=str,
        help="Path to the JSON file containing the extracted UI components."
    )
    parser.add_argument(
        "--prefix", type=str, default="src/test/java/com/sdp/m1", help="Optional prefix for the prompt for examples\nDefault will be : `src/test/java/com/sdp/m1`."
    )

    args = parser.parse_args()

    if args.srs2json:
        srs_to_json(args.srs2json)
    else:
        if not args.jsrs or not args.ui:
            parser.error(
                "Without --srs2json, both --jsrs and --ui are required.")

    prompt = generate_test_prompt(args.jsrs, args.ui, args.prefix)
    if prompt:
        print(prompt)


if __name__ == "__main__":
    load_dotenv()
    api_key = os.getenv("OPEN_API_KEY")
    model = OpenAI(api_key=api_key)
    main()
