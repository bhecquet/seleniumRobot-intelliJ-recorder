package io.github.bhecquet.seleniumRobot.recorder;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SeleniumAction {
    private String command;


    private String value;

    private List<List<String>> targets;

    public String getCommand() {
        return command;
    }

    public String getFormattedCommand() {
        String action = null;
        String formattedValue = null;
        switch (command) {
            case "type":
                action = "sendKeys";
                formattedValue = "\"" + value + "\"";
                break;
            case "doubleClick":
                action = "doubleClickAction";
                break;
            case "click":
            case "check":
            case "uncheck":
                action = "click";
                break;
            case "dragAndDropToObject":
                action = "dragAndDropToObject-unknown";
                break;
            case "select":
                if (value.startsWith("label=")) {
                    action = "selectByText";
                    formattedValue = "\"" + value.replace("label=", "").trim() + "\"";
                } else if (value.startsWith("value=")) {
                    action = "selectByValue";
                    formattedValue = "\"" + value.replace("value=", "").trim() + "\"";
                } else if (value.startsWith("index=")) {
                    action = "selectByIndex";
                    formattedValue = value.replace("index=", "").trim();
                } else {
                    action = "selectBy-unknown-" + value;
                    formattedValue = value;
                }
                break;
            case "removeSelection":
                if (value.startsWith("label=")) {
                    action = "deselectByText";
                    formattedValue = "\"" + value.replace("label=", "").trim() + "\"";
                } else if (value.startsWith("value=")) {
                    action = "deselectByValue";
                    formattedValue = "\"" + value.replace("value=", "").trim() + "\"";
                } else if (value.startsWith("index=")) {
                    action = "deselectByIndex";
                    formattedValue = value.replace("index=", "").trim();
                } else {
                    action = "selectBy-unknown-" + value;
                    formattedValue = "\"" + value + "\"";
                }
                break;
            case "sendKeys":
                action = command;
                formattedValue = "Keys." + value.replace("${KEY_", "").replace("}", "");
                break;
            case "clickAt": // value is of the form "x,y"
                action = command;
                formattedValue = value;
                break;
            case "selectFrame":
                action = null;
                break;
            case "selectWindow":
                action = "selectNewWindow";
                break;
            default:
                action = command + "unknown";
        }

        return String.format("\t\t%s.%s(%s);\n", getElementName(), action, formattedValue == null ? "" : formattedValue);

    }

    public List<SeleniumTarget> getTargets() {
        return targets.stream().map(target -> new SeleniumTarget(target.get(0), target.get(1))).collect(Collectors.toList());
    }

    public String getValue() {
        return value;
    }

    public String getWebElementString() {
        String elementName = getElementName();
        String elementType = getElementType();
        String code = String.format("\n\tprivate static %s %s = new %s(\"\", %s);\n",
                elementType,
                elementName,
                elementType,
                getSelector());

        return code;
    }

    private String getSelector() {
        SeleniumTarget mainTarget = getTargets().get(0);
        String selector = null;
        switch (mainTarget.getTargetType()) {
            case "id":
                selector = String.format("By.id(\"%s\")", mainTarget.getTargetSelector().replace("id=", ""));
                break;
            case "name":
                selector = String.format("By.name(\"%s\")", mainTarget.getTargetSelector().replace("name=", ""));
                break;
            case "linkText":
                selector = String.format("By.linkText(\"%s\")", mainTarget.getTargetSelector().replace("linkText=", ""));
                break;
            case "css:finder":
                selector = String.format("By.cssSelector(\"%s\")", mainTarget.getTargetSelector().replace("css=", ""));
                break;
            case "css:data-attr":
                Pattern pattern = Pattern.compile("css=\\[(.*)\\]");
                Matcher matcher = pattern.matcher(mainTarget.getTargetSelector());
                if (matcher.find()) {
                    String attributeName = matcher.group(1).split("=")[0];
                    String attributeValue = matcher.group(1).split("=").length > 1 ? matcher.group(1).split("=")[1] : "true";
                    selector = String.format("ByC.attribute(\"%s\", )", attributeName, attributeValue);
                } else {
                    selector = String.format("By.cssSelector(\"%s\")", mainTarget.getTargetSelector().replace("css=", ""));
                }

                break;
            case "xpath:attributes":
                Pattern pattern2 = Pattern.compile(".*\\[@(.*)='(.*)'\\]");
                Matcher matcher2 = pattern2.matcher(mainTarget.getTargetSelector());
                if (matcher2.find()) {
                    String attributeName = matcher2.group(1);
                    String attributeValue = matcher2.group(2);
                    selector = String.format("ByC.attribute(\"%s\", \"%s\")", attributeName, attributeValue);
                } else {
                    selector = String.format("By.cssSelector(\"%s\")", mainTarget.getTargetSelector().replace("css=", ""));
                }

                break;
            case "xpath:idRelative":
            case "xpath:position":
            case "xpath:link":
            case "xpath:href":
            case "xpath:innerText":
            case "xpath:img":
                selector = String.format("By.xpath(\"%s\")", mainTarget.getTargetSelector().replace("xpath=", ""));
                break;

        }
        return selector;
    }

    public String getElementName() {
        String name = getTargets().get(0).getTargetSelector()
                .replace("/", "")
                .replace(":", "_")
                .replace("=", "_")
                .replace("[", "")
                .replace("]", "")
                .replace("(", "")
                .replace(")", "")
                .replace("@", "")
                .replace("'", "")
                .replace("-", "_")
                .replace(">", "_")
                .replace("<", "_")
                .replace(".", "")
                .replace(" ", "_")
                .replace("#", "");
        return name;
    }

    private String getElementType() {
        if ("type".equals(command)
                || "sendKeys".equals(command)) {
            return "TextFieldElement";
        } else if ("linkText".equals(getTargets().get(0).getTargetType())) {
            return "LinkElement";
        } else if ("check".equals(command)
                || "unckeck".equals(command)) {
            return "CheckBoxElement";
        } else if ("selectFrame".equals(command)) {
            return "FrameElement";
        } else if ("select".equals(command)
                || "removeSelection".equals(command)) {
            return "SelectList";
        } else {
            return "HtmlElement";
        }
    }
}
