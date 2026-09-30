package io.github.bhecquet.seleniumRobot.recorder;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SeleniumAction {
    private String command;
    private String value;
    private String tagName;
    private List<List<String>> targets;
    private List<FrameInfo> framePath;


    public void setFramePath(List<FrameInfo> framePath) {
        this.framePath = framePath;
    }

    public List<FrameInfo> getFramePath() {
        return framePath;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getCommand() {
        return command;
    }


    public String getFormattedCommand() {
        String action = null;
        String formattedValue = null;

        switch (command) {
            case "type":
                action = "sendKeys";
                formattedValue = "\"" + escape(value) + "\"";
                break;

            case "sendKeys":
            case "keydown":
            case "keyup":
                action = "sendKeys";
                formattedValue = formatKeyValue(value);
                break;


            case "doubleClick":
            case "dblclick":
                action = "doubleClickAction";
                break;


            case "click":

            case "check":
                if ("CheckBoxElement".equals(getElementType())) {
                    action = "check";
                } else {
                    action = "click";
                }
                break;

            case "uncheck":
                if ("CheckBoxElement".equals(getElementType())) {
                    action = "uncheck";
                } else {
                    action = "click";
                }
                break;


            case "dragAndDropToObject":
                action = "dragAndDropTo";
                formattedValue = "";
                break;
            case "contextmenu":
                action = "rightClickMouse";
                break;

            case "select":
                if (value != null && value.startsWith("label=")) {
                    action = "selectByText";
                    formattedValue = "\"" + escape(value.replace("label=", "").trim()) + "\"";
                } else if (value != null && value.startsWith("value=")) {
                    action = "selectByValue";
                    formattedValue = "\"" + escape(value.replace("value=", "").trim()) + "\"";
                } else if (value != null && value.startsWith("index=")) {
                    action = "selectByIndex";
                    formattedValue = value.replace("index=", "").trim();
                } else {

                    String selText = getSelectedTextSafe(); // <-- helper ci-dessous
                    if (selText != null && !selText.isBlank()) {
                        action = "selectByText";
                        formattedValue = "\"" + escape(selText.trim()) + "\"";
                    } else if (value == null || value.trim().isEmpty()) {
                        action = "selectByIndex";
                        formattedValue = "0";
                    } else {
                        action = "selectByValue";
                        formattedValue = "\"" + escape(value.trim()) + "\"";
                    }
                }
                break;

            case "change":

                if ("SelectList".equals(getElementType())) {
                    String selText = getSelectedTextSafe();
                    if (selText != null && !selText.isBlank()) {
                        action = "selectByText";
                        formattedValue = "\"" + escape(selText.trim()) + "\"";
                    } else if (value != null && !value.trim().isEmpty()) {
                        action = "selectByValue";
                        formattedValue = "\"" + escape(value.trim()) + "\"";
                    } else {
                        action = "selectByIndex";
                        formattedValue = "0";
                    }
                } else {

                    action = "click";
                    formattedValue = null;
                }
                break;


            case "removeSelection":
                if (value != null && value.startsWith("label=")) {
                    action = "deselectByText";
                    formattedValue = "\"" + escape(value.replace("label=", "").trim()) + "\"";
                } else if (value != null && value.startsWith("value=")) {
                    action = "deselectByValue";
                    formattedValue = "\"" + escape(value.replace("value=", "").trim()) + "\"";
                } else if (value != null && value.startsWith("index=")) {
                    action = "deselectByIndex";
                    formattedValue = value.replace("index=", "").trim();
                } else {
                    action = "deselectByText";
                    formattedValue = value == null ? "" : "\"" + escape(value.trim()) + "\"";
                }
                break;

            case "clickAt": // value is of the form "x,y"
                action = "clickAt";
                formattedValue = value;
                break;

            case "selectFrame":
                break;


            case "selectWindow":
                action = "selectNewWindow";
                break;

            default:

                action = command;
        }

        return String.format("\t\t%s.%s(%s);\n",
                getElementName(),
                action,
                formattedValue == null ? "" : formattedValue
        );
    }

    public List<SeleniumTarget> getTargets() {
        return targets.stream()
                .map(t -> new SeleniumTarget(t.get(1), t.get(0)))
                .collect(Collectors.toList());
    }

    public String getValue() {
        return value;
    }

    public String getWebElementString() {
        String elementName = getElementName();
        String elementType = getElementType();
        String selector = getSelector();


        String logicalName = "\"" + escape(normalizeLabel(firstNonEmpty(
                extractAriaLabelFromTargets(),
                extractButtonTextFromTargets(),
                extractLinkTextFromTargets(),
                extractAdjacentTextFromTargets(),
                extractPlaceholder(selector),
                extractElementTextFromTargets(),
                extractNameFromTargets(),
                extractIdFromTargets(),
                extractDataTestidFromTargets(),
                elementName
        ))) + "\"";

        String frameRef = "";
        if (framePath != null && !framePath.isEmpty()) {
            FrameInfo fr = framePath.get(framePath.size() - 1);
            String frameVar = frameVarNameFrom(fr);
            frameRef = ", " + frameVar;
        }


        return String.format(
                "\n\tprivate static %s %s = new %s(%s, %s%s);\n",
                elementType,
                elementName,
                elementType,
                logicalName,
                selector,
                frameRef
        );

    }

    private String computeFrameLogicalId(FrameInfo fr) {
        String id = fr.getId();
        if (id != null && !id.isBlank()) return id.trim();
        String sel = String.valueOf(fr.getSelector());
        return Integer.toHexString(sel.hashCode());
    }

    private String frameVarNameFrom(FrameInfo fr) {
        String logicalId = computeFrameLogicalId(fr);
        String sanitized = logicalId.replaceAll("[^A-Za-z0-9_]", "_");
        return "frame_" + sanitized;
    }

    protected String getSelector() {
        List<SeleniumTarget> rankedTargets = getTargets().stream()
                .sorted(Comparator.comparingInt(this::scoreTarget).reversed())
                .collect(Collectors.toList());

        String selector = null;

        for (SeleniumTarget target : rankedTargets) {
            String candidate = buildSelectorFromTarget(target);

            if (!isInvalidSelector(candidate)) {
                selector = candidate;
                break;
            }
        }

        if (isInvalidSelector(selector)) {
            String elemType = getElementType();

            if ("LinkElement".equals(elemType)) {
                String linkText = extractLinkTextFromTargets();

                if (linkText != null && !linkText.isBlank() && !isBadNameSource(linkText)) {
                    selector = String.format(
                            "By.linkText(\"%s\")",
                            escape(cleanText(linkText))
                    );
                }
            } else if ("ButtonElement".equals(elemType)) {
                selector = "By.cssSelector(\"button, input[type='button'], input[type='submit'], input[type='reset']\")";
            } else if ("SelectList".equals(elemType)) {
                selector = "By.cssSelector(\"select\")";
            } else if ("CheckBoxElement".equals(elemType)) {
                selector = "By.cssSelector(\"input[type='checkbox']\")";
            } else if ("TextFieldElement".equals(elemType)) {
                selector = "By.cssSelector(\"input, textarea\")";
            }
        }

        return selector;
    }

    private boolean isInvalidSelector(String s) {
        if (s == null) return true;
        String t = s.trim();
        return t.isEmpty() || t.equals("By.cssSelector(\"*\")") || t.equals("By.cssSelector(\"css\")");
    }


    private String buildSelectorFromTarget(SeleniumTarget target) {
        if (target == null) return null;

        String type = target.getTargetType() == null ? "" : target.getTargetType().trim();
        String raw = target.getTargetSelector() == null ? "" : target.getTargetSelector().trim();
        if (raw.isEmpty()) return null;

        switch (type) {
            case "data-testid":
            case "dataTestid":
                return "ByC.attribute(\"data-testid\", \""
                        + escape(raw)
                        + "\")";
            case "data-selenium-id":
                return "ByC.attribute(\"data-selenium-id\", \""
                        + escape(raw)
                        + "\")";
            case "data-test":
                return "ByC.attribute(\"data-test\", \""
                        + escape(raw)
                        + "\")";

            case "data-cy":
                return "ByC.attribute(\"data-cy\", \""
                        + escape(raw)
                        + "\")";
            case "data-css":
                return "ByC.attribute(\"data-css\", \""
                        + escape(raw)
                        + "\")";

            case "data-qa":
                return "ByC.attribute(\"data-qa\", \""
                        + escape(raw)
                        + "\")";

            case "data-tid":
                return "ByC.attribute(\"data-tid\", \""
                        + escape(raw)
                        + "\")";

            case "data-auto":
                return "ByC.attribute(\"data-auto\", \""
                        + escape(raw)
                        + "\")";

            case "data-test-id":
                return "ByC.attribute(\"data-test-id\", \""
                        + escape(raw)
                        + "\")";

            case "data-test-selector":
                return "ByC.attribute(\"data-test-selector\", \""
                        + escape(raw)
                        + "\")";
            case "uniqueElementText":
                String uniqueText =
                        cleanText(raw);

                String textTagName =
                        getTagName() == null
                                ? ""
                                : getTagName()
                                .trim()
                                .toLowerCase(Locale.ROOT);

                if (
                        uniqueText == null
                                || uniqueText.isBlank()
                                || textTagName.isBlank()
                ) {
                    return null;
                }

                return "ByC.text(\""
                        + escape(uniqueText)
                        + "\", \""
                        + escape(textTagName)
                        + "\")";
            case "formControlName":
            case "formcontrolname":
                return "ByC.attribute(\"formcontrolname\", \""
                        + escape(raw)
                        + "\")";

            case "ariaLabelledBy":
            case "aria-labelledby":
                return "ByC.attribute(\"aria-labelledby\", \""
                        + escape(raw)
                        + "\")";

            case "placeholder":
                return "ByC.attribute(\"placeholder\", \""
                        + escape(raw)
                        + "\")";

            case "id":
                if (!isBusinessId(raw)) {
                    return null;
                }

                return String.format("By.id(\"%s\")", escape(raw));

            case "checkboxNameValue":
                String[] checkboxParts =
                        raw.split(
                                Pattern.quote("||VALUE||"),
                                2
                        );

                if (checkboxParts.length != 2) {
                    return null;
                }

                String checkboxName =
                        checkboxParts[0];

                String checkboxValue =
                        checkboxParts[1];

                if (
                        checkboxName.isBlank() ||
                                checkboxValue.isBlank()
                ) {
                    return null;
                }

                return "ByC.and("
                        + "ByC.attribute(\"name\", \""
                        + escape(checkboxName)
                        + "\"), "
                        + "ByC.attribute(\"value\", \""
                        + escape(checkboxValue)
                        + "\")"
                        + ")";
            case "name":
                return String.format("By.name(\"%s\")", escape(raw));


            case "buttonText":
                String buttonText = cleanText(raw);

                if (buttonText == null || buttonText.isBlank()) {
                    return null;
                }

                return "By.xpath(\"//button[normalize-space(.)='"
                        + escape(buttonText)
                        + "']"
                        + " | "
                        + "//input["
                        + "(@type='button' or @type='submit' or @type='reset')"
                        + " and @value='"
                        + escape(buttonText)
                        + "']\")";


            case "linkText":
                String cleaned = cleanText(raw);

                if (cleaned == null || cleaned.isBlank()) {
                    return null;
                }


                if (cleaned.length() <= 80 && !isBadNameSource(cleaned)) {
                    return "By.linkText(\""
                            + escape(cleaned)
                            + "\")";
                }


                return "By.xpath(\"//a[contains(normalize-space(.), '"
                        + escape(cleaned)
                        + "')]\")";

            case "ariaLabel":
            case "aria-label":
                return "ByC.attribute(\"aria-label\", \""
                        + escape(cleanText(raw))
                        + "\")";

            case "css":
            case "css:finder":

                return buildBestByCFromCss(raw);


            case "xpath:scopedCheckbox":
                return "By.xpath(\"" + escape(raw) + "\")";

            case "xpath":
            case "xpath:attributes":
            case "xpath:idRelative":
            case "xpath:position":
            case "xpath:link":
            case "xpath:href":
            case "xpath:innerText":
            case "xpath:img":

                String dt = extractAttributeFromXPath(raw, "data-testid");
                if (dt != null) {
                    return "ByC.attribute(\"data-testid\", \""
                            + escape(dt)
                            + "\")";
                }

                String aria = extractAttributeFromXPath(raw, "aria-label");
                if (aria != null && !aria.isBlank()) {
                    return "ByC.attribute(\"aria-label\", \""
                            + escape(cleanText(aria))
                            + "\")";
                }


                String href = extractAttributeFromXPath(raw, "href");

                if (href != null && href.length() > 10) {

                    String key = href.substring(href.lastIndexOf("/") + 1)
                            .replaceAll("_\\d+.*", "")
                            .replace(".html", "");

                    if (key.length() > 5) {
                        return "By.cssSelector(\"a[href*='" + escape(key) + "']\")";
                    }
                }


                String id = extractAttributeFromXPath(raw, "id");
                if (id != null && isBusinessId(id)) {
                    return "By.id(\"" + escape(id) + "\")";
                }


                if (getElementType().equals("CheckBoxElement")) {

                    String idAttr = extractAttributeFromXPath(raw, "id");
                    if (idAttr != null) {
                        return "By.id(\"" + escape(idAttr) + "\")";
                    }

                    return null;
                }


                String title = extractAttributeFromXPath(raw, "title");
                if (title != null && title.length() < 50) {
                    return "By.cssSelector(\"[title='" + escape(title) + "']\")";
                }

                String text = cleanText(extractLinkTextFromTargets());

                if (raw.startsWith("//div[") || raw.contains("/div[") || raw.length() > 120) {

                    String fallbackTagName =
                            getTagName() == null
                                    ? ""
                                    : getTagName()
                                    .trim()
                                    .toLowerCase(Locale.ROOT);

                    if (
                            text != null
                                    && text.length() < 40
                                    && !fallbackTagName.isBlank()
                    ) {
                        return "ByC.text(\""
                                + escape(text)
                                + "\", \""
                                + escape(fallbackTagName)
                                + "\")";
                    }

                    return "By.xpath(\"" + escape(raw.startsWith("xpath=") ? raw.substring(6) : raw) + "\")";
                }


                if (text != null && !text.isEmpty()) {
                    return "By.xpath(\"//*[contains(normalize-space(.),'"
                            + escape(text.substring(0, Math.min(30, text.length())))
                            + "')]\")";
                }


                String fallbackText = firstNonEmpty(
                        safeNameSource(extractButtonTextFromTargets()),
                        safeNameSource(extractAriaLabelFromTargets()),
                        safeNameSource(extractAdjacentTextFromTargets()),
                        safeNameSource(extractLinkTextFromTargets())
                );

                if (!"element".equals(fallbackText)) {
                    return "By.xpath(\"//*[contains(normalize-space(.), '"
                            + escape(cleanText(fallbackText))
                            + "')]\")";
                }

                String fallbackXPath = raw.startsWith("xpath=")
                        ? raw.substring("xpath=".length())
                        : raw;

                return "By.xpath(\""
                        + escape(fallbackXPath)
                        + "\")";


            default:
                return null;
        }
    }

    private String cleanText(String text) {
        if (text == null) {
            return null;
        }

        return text
                // Espaces Unicode à convertir en espace normal
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2007', ' ')

                // Caractères invisibles à supprimer
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u2060", "")
                .replace("\uFEFF", "")


                .replaceAll("\\s+", " ")
                .trim();
    }


    private String extractAttributeFromXPath(String raw, String attributeName) {
        if (raw == null) return null;


        if (raw.startsWith("xpath=")) raw = raw.substring(6);

        Pattern p = Pattern.compile(attributeName + "\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher m = p.matcher(raw);
        if (m.find()) return m.group(1);

        return null;
    }


    private String buildBestByCFromCss(String css) {
        if (css == null) return null;
        if (css.startsWith("css=")) css = css.substring(4);
        String s = css.trim();

        // REFUSER les paths fragiles (dom path)
        if (looksLikeDomPath(s)) {
            return null;
        }

        //  #id => By.id
        if (s.matches("^#([A-Za-z0-9_-]+)$")) {
            return String.format("By.id(\"%s\")", escape(s.substring(1)));
        }

        //  [id='x'] => By.id
        Matcher idMatcher = Pattern.compile("\\[id=['\"]?([^'\"\\]]+)['\"]?\\]").matcher(s);
        if (idMatcher.find()) {
            return String.format("By.id(\"%s\")", escape(idMatcher.group(1)));
        }

        //  [name='x'] => By.name
        Matcher nameMatcher = Pattern.compile("\\[name=['\"]?([^'\"\\]]+)['\"]?\\]").matcher(s);
        if (nameMatcher.find()) {
            return String.format("By.name(\"%s\")", escape(nameMatcher.group(1)));
        }

        //  [data-testid='x'] => SeleniumRobot (ByC)
        Matcher dtMatcher = Pattern.compile("\\[data-testid=['\"]?([^'\"\\]]+)['\"]?\\]").matcher(s);
        if (dtMatcher.find()) {
            return "ByC.attribute(\"data-testid\", \""
                    + escape(dtMatcher.group(1))
                    + "\")";
        }

        // [aria-label='x'] => SeleniumRobot (ByC)
        Matcher ariaMatcher = Pattern.compile("\\[aria-label=['\"]?([^'\"\\]]+)['\"]?\\]").matcher(s);
        if (ariaMatcher.find()) {
            return "ByC.attribute(\"aria-label\", \""
                    + escape(ariaMatcher.group(1))
                    + "\")";
        }

        Matcher roleMatcher = Pattern.compile("\\[role=['\"]?([^'\"\\]]+)['\"]?\\]").matcher(s);
        if (roleMatcher.find()) {
            return String.format("By.cssSelector(\"[role='%s']\")", escape(roleMatcher.group(1)));
        }


        if (s.matches("^[A-Za-z][A-Za-z0-9_-]*$")) {
            return String.format("By.tagName(\"%s\")", escape(s));
        }

        return null;
    }

    private boolean looksLikeDomPath(String css) {
        if (css == null) return true;
        String c = css.toLowerCase(Locale.ROOT);

        // nth-of-type => fragile
        if (c.contains("nth-of-type")) return true;

        // trop profond (espaces ou >)
        int depth = c.split("\\s+|>").length;
        if (depth > 3) return true;

        // bruit Angular / Material
        if (c.contains("ng-star-inserted")) return true;

        // très long => généralement un path
        if (c.length() > 80) return true;

        return false;
    }


    // ------------------ ELEMENT NAME ------------------

    private boolean isBusinessId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }

        String value = id.trim().toLowerCase(Locale.ROOT);

        if (isDynamicValue(value)) {
            return false;
        }

        if (value.matches("^(mat|cdk|ng|ember|react|vue)-.*")) {
            return false;
        }

        if (value.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            return false;
        }

        if (value.matches("^[a-z0-9_-]*\\d{6,}[a-z0-9_-]*$")) {
            return false;
        }

        return true;
    }

    private boolean isBadNameSource(String text) {
        if (text == null) {
            return true;
        }

        String cleaned = cleanText(text);

        if (cleaned.length() > 80) {
            return true;
        }

        if (cleaned.contains("http")) {
            return true;
        }

        if (cleaned.matches(".*\\d{4}/\\d{2}/\\d{2}.*")) {
            return true;
        }

        if (cleaned.matches(".*\\d{5,}.*")) {
            return true;
        }

        if (cleaned.toLowerCase(Locale.ROOT).contains("article réservé")) {
            return true;
        }

        return false;
    }

    private String extractElementTextFromTargets() {
        return extractTargetValue("elementText");
    }

    public String getElementName() {

        String text = extractLinkTextFromTargets();

        if (text != null && !isBadNameSource(text)) {
            text = cleanText(text);
            String[] words = text.split(" ");
            String shortText = String.join(
                    " ",
                    java.util.Arrays.copyOfRange(words, 0, Math.min(words.length, 5))
            );

            String name = toCamelCase(sanitizeForIdentifier(shortText))
                    + suggestSuffixFromType(getElementType());

            if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) {
                name = "_" + name;
            }

            return limitIdentifierLength(name);
        }

        String id = extractIdFromTargets();
        if (!isBusinessId(id)) {
            id = null;
        }

        String base = firstNonEmpty(
                safeNameSource(extractAriaLabelFromTargets()),
                safeNameSource(extractButtonTextFromTargets()),
                safeNameSource(extractLinkTextFromTargets()),
                safeNameSource(extractAdjacentTextFromTargets()),
                safeNameSource(extractElementTextFromTargets()),
                safeNameSource(extractNameFromTargets()),
                safeNameSource(id),
                normalizeCssBasedName(stripGenericTokens(firstTargetRawOrEmpty()))
        );


        String camel = toCamelCase(sanitizeForIdentifier(base));
        if (camel.isEmpty() || !Character.isJavaIdentifierStart(camel.charAt(0))) {
            camel = "_" + camel;
        }

        String suffix = suggestSuffixFromType(getElementType());


        String selector = String.valueOf(getSelector());
        boolean strongSelector =
                selector.startsWith("By.id(")
                        || selector.startsWith("By.name(")
                        || selector.startsWith("By.linkText(")
                        || selector.startsWith("ByC.attribute(")
                        || selector.startsWith("ByC.and(")
                        || selector.startsWith("ByC.text(")
                        || selector.contains("[data-testid=")
                        || selector.contains("[aria-label=")
                        || selector.contains("href=");

        if (strongSelector) {

            return limitIdentifierLength(camel + suffix);
        }

        String uniq = shortHash(selector);
        return limitIdentifierLength(camel + suffix + "_" + uniq);
    }

    private String safeNameSource(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = cleanText(value);

        if (isBadNameSource(cleaned)) {
            return null;
        }

        return cleaned;
    }

    private String stripGenericTokens(String s) {
        if (s == null) return "element";
        String t = s.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("nth[- ]?of[- ]?type\\s*\\d+", "")
                .replaceAll("\\b(div|span|app|container|row|col|section|article|list|item|cell|td|tr)\\b", " ")
                .replaceAll("[#\\[\\]\\(\\)\\.@=:>'\"/]+", " ")
                .replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? "element" : t;
    }

    private String normalizeCssBasedName(String raw) {
        if (raw == null) {
            return null;
        }

        // supprime les nth-of-type, indices, wrappers Angular
        String s = raw
                .replaceAll("nthOfType\\d+", "")
                .replaceAll("\\d+", "")
                .replaceAll("ngStarInserted", "")
                .replaceAll("ngInserted", "");


        List<String> keywords = List.of(
                "grid", "tree", "row", "cell", "node", "menu", "item", "panel", "list", "tab"
        );

        for (String k : keywords) {
            if (s.toLowerCase().contains(k)) {
                return k;
            }
        }

        // dernier recours très court
        return "element";
    }


    private String sanitizeForIdentifier(String s) {
        if (s == null) return "element";
        String t = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^A-Za-z0-9_ ]", " ")
                .replaceAll("\\s+", "_").replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "").toLowerCase(java.util.Locale.ROOT);
        return t.isEmpty() ? "element" : t;
    }

    private String shortHash(String data) {
        int h = (data == null ? 0 : data.hashCode());
        String hex = Integer.toHexString(h);
        return (hex.length() > 4) ? hex.substring(hex.length() - 4) : hex;
    }

    private String crop(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
    }
// ------------------ ELEMENT TYPE ------------------


    private String selectedText;

    public String getSelectedTextSafe() {
        return selectedText;
    }


    private boolean containsCssTag(String raw, String tag) {
        if (raw == null || raw.isEmpty()) return false;
        String s = raw.startsWith("css=") ? raw.substring("css=".length()) : raw;
        s = s.replace("\"", "").replace("'", "");
        return s.matches("(?i).*(^|[\\s>+~])" + tag + "(\\b|[\\.#\\[:]).*");
    }

    private boolean containsXpathTag(String raw, String tag) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }

        String xpath = raw.startsWith("xpath=")
                ? raw.substring("xpath=".length())
                : raw;

        xpath = xpath.replace("\"", "").replace("'", "");

        String quotedTag = Pattern.quote(tag);

        return Pattern.compile(
                "(?i)(^|/|::)" + quotedTag + "(\\[|/|$)"
        ).matcher(xpath).find();
    }

    private boolean isSelectFromTargets() {
        for (SeleniumTarget target : getTargets()) {
            String targetType =
                    target.getTargetType() == null
                            ? ""
                            : target.getTargetType()
                            .trim()
                            .toLowerCase(Locale.ROOT);

            String raw =
                    target.getTargetSelector() == null
                            ? ""
                            : target.getTargetSelector()
                            .trim()
                            .toLowerCase(Locale.ROOT);

            if (
                    ("css".equals(targetType)
                            || "css:finder".equals(targetType))
                            && containsCssTag(raw, "select")
            ) {
                return true;
            }

            if (
                    targetType.startsWith("xpath")
                            && containsXpathTag(raw, "select")
            ) {
                return true;
            }
        }

        return false;
    }

    private boolean containsCssInputType(String raw, String... types) {
        if (raw == null || raw.isEmpty()) return false;
        String s = raw.startsWith("css=") ? raw.substring("css=".length()) : raw;
        s = s.replace("\"", "").replace("'", "").toLowerCase();
        if (!s.contains("input")) return false;
        for (String t : types) {
            String tt = t.toLowerCase();
            if (s.contains("input[type=" + tt + "]")
                    || s.contains("input[type='" + tt + "']")
                    || s.contains("input[type=\"" + tt + "\"]")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsXpathInputType(String raw, String... types) {
        if (raw == null || raw.isEmpty()) return false;
        String s = raw.startsWith("xpath=") ? raw.substring("xpath=".length()) : raw;
        s = s.replace("\"", "").replace("'", "").toLowerCase();
        if (!s.contains("//input")) return false;
        for (String t : types) {
            String tt = t.toLowerCase();
            if (s.contains("@type=" + tt) || s.contains("@type='" + tt + "'") || s.contains("@type=\"" + tt + "\"")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAttribute(String raw, String attr) {
        if (raw == null) return false;
        String s = raw.toLowerCase();
        return s.contains("[" + attr.toLowerCase() + "=") || s.contains("@" + attr.toLowerCase() + "=");
    }

    private boolean containsAriaRole(String raw, String role) {
        if (raw == null) return false;
        String s = raw.toLowerCase();
        String r = role.toLowerCase();
        return s.contains("[role=" + r + "]")
                || s.contains("[role='" + r + "']")
                || s.contains("[role=\"" + r + "\"]")
                || s.contains("@role=" + r)
                || s.contains("@role='" + r + "'")
                || s.contains("@role=\"" + r + "\"");
    }


    private int scoreTarget(SeleniumTarget target) {
        if (target == null) {
            return -1000;
        }

        String type = target.getTargetType() == null ? "" : target.getTargetType().trim();
        String raw = target.getTargetSelector() == null ? "" : target.getTargetSelector().trim();

        if (raw.isEmpty()) {
            return -1000;
        }

        int score = scoreType(type);

        // Pénalités universelles
        if (isDynamicValue(raw)) {
            score -= 50;
        }

        if (isTextTooLong(raw)) {
            score -= 35;
        }

        if (isPositionBasedXPath(type, raw)) {
            score -= 70;
        }

        if (isGenericCss(raw)) {
            score -= 60;
        }

        if (looksLikeDomPath(raw)) {
            score -= 60;
        }

        if (isTechnicalFrameworkValue(raw)) {
            score -= 40;
        }


        if (isStableBusinessAttribute(type, raw)) {
            score += 30;
        }

        if (isShortReadableText(type, raw)) {
            score += 15;
        }

        return score;
    }

    private String limitIdentifierLength(String name) {
        if (name == null || name.isBlank()) {
            return "element";
        }

        final int maxLength = 45;

        if (name.length() <= maxLength) {
            return name;
        }

        String suffix = shortHash(name);
        int availableLength = maxLength - suffix.length() - 1;

        return name.substring(0, availableLength) + "_" + suffix;
    }

    private SeleniumTarget chooseBestTarget() {
        List<SeleniumTarget> targets = getTargets();

        if (targets == null || targets.isEmpty()) {
            return null;
        }

        SeleniumTarget best = null;
        int bestScore = Integer.MIN_VALUE;

        for (SeleniumTarget target : targets) {
            int score = scoreTarget(target);

            if (score > bestScore) {
                best = target;
                bestScore = score;
            }
        }

        return best;
    }

    private boolean isDynamicValue(String value) {
        if (value == null) {
            return false;
        }

        String v = value.toLowerCase(Locale.ROOT);

        // IDs techniques fréquents
        if (v.matches(".*(mat-|cdk-|ng-|ember|react|vue).*")) {
            return true;
        }

        // UUID
        if (v.matches(".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*")) {
            return true;
        }

        // Beaucoup de chiffres = souvent dynamique
        if (v.matches(".*\\d{5,}.*")) {
            return true;
        }

        // Timestamp / date probable
        if (v.matches(".*\\d{4}[-/]\\d{2}[-/]\\d{2}.*")) {
            return true;
        }

        return false;
    }

    private int scoreType(String type) {
        if (type == null) return -100;
        switch (type) {
            case "data-testid":
            case "dataTestid":
                return 110;
            case "data-selenium-id":
                return 115;
            case "data-test":
            case "data-cy":
            case "data-css":
            case "data-qa":
            case "data-tid":
            case "data-auto":
            case "data-test-id":
            case "data-test-selector":
                return 108;
            case "id":
                return 150;
            case "formControlName":
            case "formcontrolname":
                return 95;
            case "checkboxNameValue":
                return 90;
            case "name":
                return 80;
            case "ariaLabel":
            case "aria-label":
                return 75;

            case "placeholder":
                return 74;

            case "ariaLabelledBy":
            case "aria-labelledby":
                return 72;

            case "buttonText":
                return 70;
            case "uniqueElementText":
                return 65;

            case "linkText":
                return 60;
            case "css":
            case "css:finder":
                return 50;

            case "xpath:scopedCheckbox":
                return 45;

            case "xpath":
            case "xpath:attributes":
                return 40;
            default:
                return 0;
        }
    }

    private boolean isTextTooLong(String value) {
        if (value == null) {
            return false;
        }

        return cleanText(value).length() > 60;
    }

    private boolean isPositionBasedXPath(String type, String raw) {
        if (raw == null) {
            return false;
        }

        String t = type == null ? "" : type.toLowerCase(Locale.ROOT);
        String r = raw.toLowerCase(Locale.ROOT);

        return t.contains("position")
                || r.contains("nth-of-type")
                || r.matches(".*/[a-z]+\\[\\d+\\].*")
                || r.matches(".*\\([0-9]+\\).*");
    }

    private boolean isGenericCss(String raw) {
        if (raw == null) {
            return false;
        }

        String r = raw.trim().toLowerCase(Locale.ROOT);

        return r.equals("div")
                || r.equals("span")
                || r.equals("input")
                || r.equals("button")
                || r.equals("*")
                || r.equals("css")
                || r.equals("a");
    }

    private boolean isTechnicalFrameworkValue(String raw) {
        if (raw == null) {
            return false;
        }

        String r = raw.toLowerCase(Locale.ROOT);

        return r.contains("ng-star-inserted")
                || r.contains("mat-")
                || r.contains("cdk-")
                || r.contains("css-")
                || r.contains("sc-")
                || r.contains("chakra-")
                || r.contains("Mui");
    }

    private boolean isStableBusinessAttribute(String type, String raw) {
        if (type == null || raw == null) {
            return false;
        }

        String t = type.toLowerCase(Locale.ROOT);
        String r = raw.toLowerCase(Locale.ROOT);

        if (t.contains("data-testid") || t.contains("datatestid")) {
            return true;
        }

        if (t.equals("id") && isBusinessId(raw)) {
            return true;
        }
        if (t.equals("checkboxnamevalue")) {
            return true;
        }

        if (t.equals("name")) {
            return true;
        }

        if (t.contains("aria")) {
            return true;
        }

        return r.contains("data-testid")
                || r.contains("data-test")
                || r.contains("data-cy")
                || r.contains("aria-label");
    }


    private String firstTargetRawOrEmpty() {
        List<SeleniumTarget> t = getTargets();
        return (t != null && !t.isEmpty()) ? t.get(0).getTargetSelector() : "";
    }

    private boolean isShortReadableText(String type, String raw) {
        if (type == null || raw == null) {
            return false;
        }

        String t = type.toLowerCase(Locale.ROOT);
        String cleaned = cleanText(raw);

        if (!(t.contains("text") || t.contains("button") || t.contains("link"))) {
            return false;
        }

        return cleaned.length() > 1
                && cleaned.length() <= 40
                && !isDynamicValue(cleaned);
    }


    private String normalizeLabel(String label) {
        if (label == null) {
            return "element";
        }

        String cleaned = cleanText(label)
                .replaceAll("<[^>]+>", " ")
                .replaceAll("https?://\\S+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty()) {
            return "element";
        }
        return cleaned;
    }

    private String extractAriaLabel(String sOrSelector) {
        if (sOrSelector == null) return null;
        Matcher m1 = Pattern.compile("aria-label='\\\"['\\\"]").matcher(sOrSelector);
        if (m1.find()) return m1.group(1);
        Matcher m2 = Pattern.compile("\\[aria-label=([^\\]]+)\\]").matcher(sOrSelector);
        if (m2.find()) return m2.group(1).replace("'", "").replace("\"", "");
        return null;
    }

    private String extractPlaceholder(String selector) {
        if (selector == null) return null;
        Matcher m = Pattern.compile("placeholder='\\\"['\\\"]").matcher(selector);
        return m.find() ? m.group(1) : null;
    }

    private String extractTargetValue(String wantedType) {
        for (SeleniumTarget t : getTargets()) {
            if (t.getTargetType() != null && t.getTargetType().equals(wantedType)) {
                return t.getTargetSelector();
            }
        }
        return null;
    }

    private String extractIdFromTargets() {
        return extractTargetValue("id");
    }

    private String extractNameFromTargets() {
        return extractTargetValue("name");
    }

    private String extractLinkTextFromTargets() {
        return extractTargetValue("linkText");
    }

    private String extractAriaLabelFromTargets() {
        String v = extractTargetValue("ariaLabel");
        if (v == null) v = extractTargetValue("aria-label");
        return v;
    }

    private String extractDataTestidFromTargets() {
        String v = extractTargetValue("data-testid");
        if (v == null) v = extractTargetValue("dataTestid");
        return v;
    }

    private String extractButtonTextFromTargets() {
        return extractTargetValue("buttonText");
    }

    private String extractAdjacentTextFromTargets() {
        return extractTargetValue("adjacentText");
    }


    // --- noms & formatage ---


    private String toCamelCase(String s) {
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            if (i == 0) sb.append(p);
            else sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private String suggestSuffixFromType(String elementType) {
        if (elementType == null) return "";
        switch (elementType) {
            case "TextFieldElement":
                return "Field";
            case "SelectList":
                return "Select";
            case "ButtonElement":
                return "Button";
            case "LinkElement":
                return "Link";
            case "CheckBoxElement":
                return "Checkbox";
            default:
                return "Element";
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String formatKeyValue(String v) {
        if (v == null) return "\"\"";
        if (v.startsWith("${KEY_") && v.endsWith("}")) {
            String key = v.substring("${KEY_".length(), v.length() - 1);
            return "Keys." + key;
        }
        return "\"" + escape(v) + "\"";
    }

    private String firstNonEmpty(String... vals) {
        for (String s : vals) if (s != null && !s.trim().isEmpty()) return s.trim();
        return "element";
    }

    public String getElementType() {

        final boolean isTypingCommand = "sendKeys".equals(command)
                || "keydown".equals(command) || "keyup".equals(command) || "type".equals(command);
        if ("check".equals(command) || "uncheck".equals(command)) {
            return "CheckBoxElement";
        }
        if (isTypingCommand) {
            return "TextFieldElement";
        }
        String normalizedTagName = tagName == null ? "" : tagName.trim().toLowerCase(Locale.ROOT);

        switch (normalizedTagName) {
            case "button":
                return "ButtonElement";

            case "a":
                return "LinkElement";

            case "select":
                return "SelectList";

            case "textarea":
                return "TextFieldElement";

            case "iframe":
            case "frame":
                return "FrameElement";

            case "img":
                return "ImageElement";
            case "input":
                return "TextFieldElement";

            default:
                break;
        }

        for (SeleniumTarget target : getTargets()) {
            String targetType =
                    target.getTargetType() == null
                            ? ""
                            : target.getTargetType()
                            .trim()
                            .toLowerCase(Locale.ROOT);

            String raw =
                    target.getTargetSelector() == null
                            ? ""
                            : target.getTargetSelector()
                            .trim()
                            .toLowerCase(Locale.ROOT);

            if (
                    "role".equals(targetType)
                            && "checkbox".equals(raw)
            ) {
                return "CheckBoxElement";
            }
        }


        for (SeleniumTarget t : getTargets()) {
            String raw = t.getTargetSelector() == null ? "" : t.getTargetSelector().toLowerCase();

            if (containsCssInputType(raw, "checkbox") || containsXpathInputType(raw, "checkbox")) {
                return "CheckBoxElement";
            }
        }


        if ("select".equals(command)) {
            return "SelectList";
        }


        if ("change".equals(command) && isSelectFromTargets()) {
            return "SelectList";
        }


        if (isSelectFromTargets()) {
            return "SelectList";
        }

        if ("linktext".equals(command)) {
            return "LinkTextElement";
        }
        if ("image".equals(command)) {
            return "ImageElement";
        }


        boolean isLink = false, isButton = false, isCheckbox = false, isSelect = false, isFrame = false, isText = false, isImage = false;

        for (SeleniumTarget t : getTargets()) {
            String raw = (t.getTargetSelector() == null) ? "" : t.getTargetSelector().toLowerCase();
            String type = (t.getTargetType() == null) ? "" : t.getTargetType().toLowerCase();


            if ("linktext".equals(type) || containsCssTag(raw, "a") || containsXpathTag(raw, "a") || containsAttribute(raw, "href")) {
                isLink = true;
            }


            if ("button".equals(type)
                    || containsCssTag(raw, "button")
                    || containsXpathTag(raw, "button")
                    || containsCssInputType(raw, "submit", "button", "reset")
                    || containsXpathInputType(raw, "submit", "button", "reset")
                    || containsAriaRole(raw, "button")) {
                isButton = true;
            }


            if (containsCssInputType(raw, "checkbox") || containsXpathInputType(raw, "checkbox") || containsAriaRole(raw, "checkbox")) {
                isCheckbox = true;
            }


            if (containsCssTag(raw, "select") || containsXpathTag(raw, "select")) {
                isSelect = true;
            }


            if (containsCssTag(raw, "iframe") || containsCssTag(raw, "frame")
                    || containsXpathTag(raw, "iframe") || containsXpathTag(raw, "frame")) {
                isFrame = true;
            }


            if (containsCssInputType(raw, "text", "email", "password", "url", "tel", "number", "search")
                    || containsXpathInputType(raw, "text", "email", "password", "url", "tel", "number", "search")
                    || containsCssTag(raw, "textarea")
                    || containsXpathTag(raw, "textarea")) {
                isText = true;
            }


            if (containsCssTag(raw, "img") || containsXpathTag(raw, "img")) {
                isImage = true;
            }
        }

        if (isLink) return "LinkElement";
        if (isButton) return "ButtonElement";
        if (isCheckbox) return "CheckBoxElement";
        if (isSelect) return "SelectList";
        if (isFrame) return "FrameElement";
        if (isText || isTypingCommand) return "TextFieldElement";
        if (isImage) return "ImageElement";


        return "HtmlElement";
    }


}
