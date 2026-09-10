# SeleniumRobot Recorder

This extension is heavily inspired by SeleniumBase Recorder.

The original project was created by Michael Mintz.

This version is an adaptation and extension developed by Covéa for the SeleniumRobot Recorder plugin.

SeleniumRobot Recorder is an IntelliJ plugin combined with a Chrome extension.

It records user interactions performed in Chrome and generates SeleniumRobot-compatible Java code directly in the active
IntelliJ editor.

> The generated code is a starting point. It must be reviewed, executed, and stabilized before being added to a
> production test suite.

## Current Constraint

- Only one Recorder server can run at a time across all IntelliJ instances.

---

## Table of Contents

- #main-features
- #prerequisites
- Getting Started
- Chrome Extension Recorder
    - Prepare the Target Class
    - prepare-recording
    - Start Recording/stop-recording/
      locator-selection
    - Locator Rules
    - supported-stable-data-attributes
    - [important-rules
- Supported Actions
- #key-files
    - #contentScript.js
    - #seleniumtarget.java
    - #frameinfo.java
    - #manifest.json
- #build
    - #build-the-intellij-plugin
    - #build-output
    - #Clean the Project
- [Main Areas to Understand

---

## Main Features

- Record clicks performed in Chrome.
- Record text input.
- Record HTML select interactions.
- Record checkbox state changes.
- Record double-click actions.
- Generate SeleniumRobot element declarations.
- Generate Java actions in the active method.
- Add the required Java imports.
- Select a locator from several available candidates.
- Reject known dynamic IDs.
- Avoid duplicate element declarations.
- Consolidate text input into a single action.
- Transmit iframe context information when available.

---

## Prerequisites

Install the following tools before running the project:

- Java 21
- IntelliJ IDEA
- Google Chrome
- Git
- Gradle, or the Gradle wrapper included in the repository

The following resources are also required:

- a SeleniumRobot test project;
- the SeleniumRobot Recorder Chrome extension;
- an available local port `5222`.

---

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd <repository-directory>
```

Replace the placeholders with the actual repository URL and directory name.

### 2. Open the Project

1. Open IntelliJ IDEA.
2. Select **File > Open**.
3. Select the project directory.
4. Configure the project SDK with Java 21.
5. Wait for Gradle synchronization to complete.

### 3. Verify the Build

On Windows:

```powershell
.\gradlew.bat clean build
```

On Linux or macOS:

```bash
./gradlew clean build
```

---

## Chrome Extension Setup

The Chrome extension is currently loaded manually.

1. Open Chrome.
2. Navigate to:

```text
chrome://extensions
```

3. Enable **Developer mode**.
4. Click **Load unpacked**.
5. Select the extension directory containing `manifest.json`.
6. Verify that the extension is enabled.
7. Verify that Chrome does not display an extension error.

> Do not move or delete the selected extension directory after loading it. Chrome uses the files directly from this
> directory.

For the illustrated installation procedure, see the internal Confluence documentation.

---

## Running the Recorder

### Prepare the Target Class

1. Open an existing SeleniumRobot test project in IntelliJ.
2. Open the Java class that will receive the generated code.
3. Open or create the target method.
4. Place the cursor inside this method.

The active file and cursor position determine where generated actions are inserted.

### Start Recording

1. Open the IntelliJ **Tools** menu.
2. Start SeleniumRobot Recorder.
3. Wait for the Jetty server to start.
4. Verify that port `5222` is available.
5. Open the application to test in Chrome.
6. Perform the required user interactions.
7. Return to IntelliJ regularly to review the generated code.

### Stop Recording

1. Return to IntelliJ.
2. Open the **Tools** menu.
3. Stop SeleniumRobot Recorder.

Stopping the Recorder:

- releases the local server;
- prevents additional browser actions from being recorded;
- keeps the generated code in the active class.

---

## Target Format

Each target contains:

```text
[target type, target value]
```

---

## Locator Selection

Locator candidates are ranked by stability, uniqueness, and readability.

### Locator Rules

- A stable and unique ID has the highest priority.
- Dynamic IDs must be rejected.
- A locator should identify exactly one element.
- `ByC.text()` must only be used for unique and stable text.
- A shared `name` must not be used alone.
- Long DOM-based XPath expressions must be the final fallback.
- Position-based XPath expressions must be considered fragile.
- Locator order in the JSON payload does not determine the final locator.
- The final locator is selected by the Java scoring system.

### Supported Stable Data Attributes

The Recorder can collect the following attributes:

```text
data-testid
data-test
data-cy
data-css
data-qa
data-tid
data-auto
data-test-id
data-test-selector
```

### Important Rules

- A real HTML `<select>` element is mapped to `SelectList`.
- A custom component using `<div role="combobox">` is not automatically mapped to `SelectList`.
- A checkbox command produces a `CheckBoxElement`.
- Clicking an icon or a `<span>` inside a button should target the parent button.
- Clicking an image inside a link should target the parent link.

---

## Supported Actions

| Browser interaction | Generated action       |
|---------------------|------------------------|
| Standard click      | `click()`              |
| Double-click        | `doubleClickAction()`  |
| Text input          | `sendKeys(value)`      |
| Check a checkbox    | `check()`              |
| Uncheck a checkbox  | `uncheck()`            |
| Select by label     | `selectByText(value)`  |
| Select by value     | `selectByValue(value)` |
| Select by index     | `selectByIndex(index)` |
| Drag and drop       | `dragAndDropTo()`      |
| Context menu        | `contextMenuAction()`  |

> Context menu recording is listed as a target behavior but is not currently functional. See #known-limitations.

---

## Project Structure

The following structure is provided as a reference.

Update it if the repository structure changes.

```text
project-root/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/github/bhecquet/seleniumRobot/recorder/
│   │   │       ├── SeleniumAction.java
│   │   │       ├── SeleniumTarget.java
│   │   │       └── FrameInfo.java
│   │   └── resources/
│   └── test/
├── extension/
│   └── recorder/
│       ├── manifest.json
│       └── contentScript.js
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Key Files

### `contentScript.js`

Main responsibilities:

- capture browser events;
- normalize clicked elements;
- detect interactive parent elements;
- collect locator candidates;
- check CSS locator uniqueness;
- build fallback XPath expressions;
- consolidate text input;
- detect checkbox state;
- build the JSON payload;
- send events to the Jetty server;
- collect iframe context information.

### `SeleniumAction.java`

Main responsibilities:

- read locator candidates;
- calculate locator scores;
- apply locator penalties;
- select the best valid locator;
- detect the SeleniumRobot element type;
- generate element declarations;
- generate Java actions;
- generate logical names;
- avoid invalid generic selectors.

### `SeleniumTarget.java`

Represents one locator candidate.

A target contains:

- a target type;
- a target value.

### `FrameInfo.java`

Represents one iframe in the current frame hierarchy.

Typical fields:

- frame ID;
- frame name;
- frame title;
- frame selector.

### `manifest.json`

Defines:

- where the Chrome extension can run;
- which scripts are injected;
- required permissions;
- iframe injection behavior.

### Jetty Endpoint

The Chrome extension sends events to:

```text
POST http://localhost:5222/event
```

Content type:

```text
application/json
```

---

## Build

### Build the IntelliJ Plugin

On Windows:

```powershell
.\gradlew.bat clean buildPlugin --no-configuration-cache
```

On Linux or macOS:

```bash
./gradlew clean buildPlugin --no-configuration-cache
```

### Build Output

The generated plugin archive is available in:

```text
build/distributions/
```

### Clean the Project

On Windows:

```powershell
.\gradlew.bat clean
```

On Linux or macOS:

```bash
./gradlew clean
```

---

## Known Limitations

- Only one Recorder server can run at a time across all IntelliJ instances.
- Right-click recording is not currently functional.
- Shadow DOM support is incomplete.
- Cross-origin iframe access is restricted.
- Custom web components may require manual review.
- Position-based XPath expressions can be fragile.
- Generated code must be reviewed before production use.
- The Chrome extension is currently loaded manually.
- Some application-specific components may require custom handling.

---

## Main Areas to Understand

A new maintainer should understand:

- browser event listeners in `contentScript.js`;
- target normalization;
- interactive parent detection;
- locator candidate collection;
- locator uniqueness checks;
- input consolidation;
- checkbox state detection;
- the event JSON format;
- the Jetty endpoint;
- locator scoring in `SeleniumAction.java`;
- locator conversion in `buildSelectorFromTarget()`;
- SeleniumRobot element type detection;
- Java declaration generation;
- Java action generation;
- editor insertion logic;
- iframe context handling;
- duplicate event prevention.

<!-- Plugin description -->
IntelliJ plugin that records user interactions through the SeleniumRobot Recorder Chrome extension and generates
SeleniumRobot-compatible Java code in the active IntelliJ project.
<!-- Plugin description end -->