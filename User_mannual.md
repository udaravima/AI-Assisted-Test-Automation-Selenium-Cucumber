
---

# 🚀 AI-Assisted Test Case Builder with Selenium & Cucumber

## 📑 Table of Contents

* [📘 User Guide (VSCode Setup)](#-user-guide-vscode-setup)

  * [Step 1: Prerequisites & Workspace Setup](#step-1-prerequisites--workspace-setup)

    * [Requirements](#-requirements)
    * [Preparing the Selenium Test Suite](#-preparing-the-selenium-test-suite)
  * [Step 2: OpenAI Environment & JSON Conversion](#step-2-openai-environment--json-conversion)

    * [Configure OpenAI API Key](#-configure-openai-api-key)
    * [Install Python Dependencies](#-install-python-dependencies)
    * [Convert SRS to JSON](#-convert-srs-to-json)
  * [Step 3: Building Test Cases with AI](#step-3-building-test-cases-with-ai)

    * [Extracting Web UI Components](#-extracting-web-ui-components)
    * [Generating the AI Prompt](#-generating-the-ai-prompt)
* [🔄 Workflow Diagram](#-workflow-diagram)
* [📩 Support](#-support)

---

## 📘 User Guide (VSCode Setup)

---

### **Step 1: Prerequisites & Workspace Setup**

#### 🔧 Requirements

* **Python**: `3.12`
* **Java JDK**: `21`
* **Maven**: `21`
* **Healenium**
* **Docker**

#### ⚙️ Preparing the Selenium Test Suite

1. Install **Language Support for Java(TM) by Red Hat** (VSCode extension).

   > Helps with Java refactoring and project structure.
2. Install **Gemini Code Assist** (AI-powered code generation).
3. Clone the project structure from the repository and rename it to your preference.

   > Default package: `(com.sdp.m1)`
4. **Do not modify** `.example` files → they are used to maintain the project structure.
5. Configure properties in:

   ```
   src/test/resources/test.properties
   ```

---

#### Setup Healenium backend

1. clone Healenium latest available [Healenium Repository](https://github.com/healenium/healenium.git)
2. run the docker compose file and bring the services up and running

---

### **Step 2: OpenAI Environment & JSON Conversion**

#### 🔑 Configure OpenAI API Key

1. Create a file named `.env` in your project root.
2. Add the following entry:

   ```env
   OPEN_API_KEY="<Your OpenAI Key>"
   ```

#### 📦 Install Python Dependencies

```sh
python3.12 -m pip install venv 
python3.12 -m venv .venv

# Activate virtual environment
[Linux]   source .venv/bin/activate
[Windows] .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

#### 📄 Convert SRS to JSON

##### 1. Python Method

Run the **generator script** with your SRS document:

```sh
python3 generator.py --srs2json <path_to_SRS_PDF>
```

* Output will be saved as:

  ```
  <path_to_SRS_PDF>.json
  ```

Also if you want to split them into sections

```sh
python3 generator.py --srs2json <path_to_SRS_PDF> --seperate
```

Or you can specify a json file to split them into sections

```sh
python3 generator.py --seperate <path_to_SRS>.json
```

##### 2. Java Method

Follow the same styles as in python
example:

```sh
mvn exec:java -Dexec.mainClass="com.sdp.m1.Generator.Generator" -Dexec.classpathScope=test -Dexec.args="--srs2json <path_to_SRS_PDF> --seperate"
```

---

### **Step 3: Building Test Cases with AI**

> ⚠️ Currently, you must extract **section by section** from the JSON and feed it back into the generator to build test unit prompts.

Now that you have the **SRS JSON**, you can generate test cases by combining:

* SRS sections (JSON format)
* Web UI component structures

#### 🖥️ Extracting Web UI Components

**Option 1: Standalone Execution**

* Open `WebPageExtractorJSON.java`
* Configure:

  * `NAV_URL` → Page URL
  * `LOGGING_REQUIRED` → `true/false`
  * (Optional) Provide cookies for authentication
* Run the `main()` method → JSON output will be saved under:

  ```
  target/exJson/<name>
  ```

**Option 2: Within Tests**
You can also invoke the extractor inside your test cases:

```java
WebPageExtractorJSON pageComponents = new WebPageExtractorJSON(driver, wait);

// Return JSON as String
String webUIJSON = pageComponents.extractComponentsAsJsonString();

// Save JSON to file
pageComponents.runExtractor();
```

---

#### ✍️ Generating the AI Prompt

##### 1. Python Method

Run the generator script with both **SRS section** and **UI components**:

```sh
python generator.py --srs <path_to_SRS_JSON_section> \
                    --ui <path_to_UI_JSON> \
                    --prefix <src/test/java/com/sdp/m1> \
> master_prompt.txt
```

##### 2. Java Method

Follow the same styles as in python
example:

```sh
mvn exec:java -Dexec.mainClass="com.sdp.m1.Generator.Generator" -Dexec.classpathScope=test -Dexec.args="--jsrs Documents/SRS_Els/2.1.2_ServiceProviderSearch_SRS.json --ui Documents/UI_Els/page_components_home_20250904_084926.json" > master_prompt.txt
```

* Copy the generated **prompt** into **Gemini Assist**.
* Gemini will create **three files** automatically in the correct location for that section.

---

## 🔄 Workflow Diagram

```mermaid
flowchart TD
    A[SRS PDF] --> B[Convert to JSON<br>(generator.py --srs2json)]
    B --> C[Extract JSON Section<br>(Unit by Unit)]
    C --> D[UI Extraction<br>WebPageExtractorJSON.java]
    D --> E[Generate Prompt<br>(generator.py --srs --ui --prefix)]
    E --> F[AI Model<br>Gemini Assist]
    F --> G[Cucumber & Java Test Files]
```

---

## 📩 Support

For inquiries or issues, contact:
**[udara.v@hsenidmobile.com](mailto:udara.v@hsenidmobile.com)**

---
