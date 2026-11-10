package io.github.bhecquet.seleniumRobot.recorder;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SeleniumActionTest {

    private SeleniumAction createAction(String command, String tagName, List<List<String>> targets) {
        SeleniumAction action = new SeleniumAction();
        action.setCommand(command);
        action.setTagName(tagName);
        action.setTargets(targets);
        return action;
    }

    @Test
    public void shouldPreferStableId() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("data-testid", "save-profile-testid"),
                List.of("id", "save-profile-id"),
                List.of("ariaLabel", "Enregistrer le profil"),
                List.of("css", "#save-profile-id"),
                List.of("xpath", "//*[@id='save-profile-id']")
        ));

        assertEquals("By.id(\"save-profile-id\")", action.getSelector());
    }

    @Test
    public void shouldRejectDynamicIdAndUseStableDataAttribute() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("id", "react-1790851234567"),
                List.of("data-qa", "dynamic-id-fallback"),
                List.of("css", "[data-qa=\"dynamic-id-fallback\"]"),
                List.of("xpath", "//*[@data-qa='dynamic-id-fallback']")
        ));

        assertEquals("ByC.attribute(\"data-qa\", \"dynamic-id-fallback\")", action.getSelector());
    }

    @Test
    public void shouldUseDataSeleniumIdWhenNoStableIdExists() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("data-selenium-id", "save-customer"),
                List.of("data-testid", "save-customer-button"),
                List.of("css", "[data-selenium-id=\"save-customer\"]")
        ));

        assertEquals("ByC.attribute(\"data-selenium-id\", \"save-customer\")", action.getSelector());
    }

    @Test
    public void shouldPreferDataSeleniumIdOverDataTestId() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("data-testid", "save-customer-button"),
                List.of("data-selenium-id", "save-customer")
        ));

        assertEquals("ByC.attribute(\"data-selenium-id\", \"save-customer\")", action.getSelector());
    }

    @Test
    public void shouldUseDataTestId() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("data-testid", "favorite-button"),
                List.of("css", "[data-testid=\"favorite-button\"]"),
                List.of("xpath", "//*[@data-testid='favorite-button']")
        ));

        assertEquals("ByC.attribute(\"data-testid\", \"favorite-button\")", action.getSelector());
    }

    @Test
    public void shouldUseDataCss() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("data-css", "target-data-css"),
                List.of("css", "[data-css=\"target-data-css\"]")
        ));

        assertEquals("ByC.attribute(\"data-css\", \"target-data-css\")", action.getSelector());
    }

    @Test
    public void shouldUseFormControlName() {
        SeleniumAction action = createAction("keyup", "input", List.of(
                List.of("formControlName", "customerName"),
                List.of("xpath", "//*[@formcontrolname='customerName']")
        ));

        assertEquals("ByC.attribute(\"formcontrolname\", \"customerName\")", action.getSelector());
    }

    @Test
    public void shouldUseUniqueName() {
        SeleniumAction action = createAction("keyup", "input", List.of(
                List.of("name", "customerName"),
                List.of("css", "input[name=\"customerName\"]")
        ));

        assertEquals("By.name(\"customerName\")", action.getSelector());
    }

    @Test
    public void shouldUseAriaLabel() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("ariaLabel", "Enregistrer le profil"),
                List.of("css", "[aria-label=\"Enregistrer le profil\"]")
        ));

        assertEquals("ByC.attribute(\"aria-label\", \"Enregistrer le profil\")", action.getSelector());
    }

    @Test
    public void shouldUseAriaLabelledBy() {
        SeleniumAction action = createAction("click", "input", List.of(
                List.of("ariaLabelledBy", "customer-label"),
                List.of("xpath", "//*[@aria-labelledby='customer-label']")
        ));

        assertEquals("ByC.attribute(\"aria-labelledby\", \"customer-label\")", action.getSelector());
    }

    @Test
    public void shouldUsePlaceholder() {
        SeleniumAction action = createAction("keyup", "input", List.of(
                List.of("placeholder", "Gare, ville, lieu..."),
                List.of("xpath", "//*[@placeholder='Gare, ville, lieu...']")
        ));

        assertEquals("ByC.attribute(\"placeholder\", \"Gare, ville, lieu...\")", action.getSelector());
    }

    @Test
    public void shouldUseLabelWhenNoStableLocatorExists() {
        SeleniumAction action = createAction("keyup", "input", List.of(
                List.of("labelText", "Nom du client sans ID"),
                List.of("css", "section label input"),
                List.of("xpath", "//main/section/label/input")
        ));

        assertEquals("ByC.label(\"Nom du client sans ID\")", action.getSelector());
    }

    @Test
    public void shouldKeepIdPrioritaryOverLabel() {
        SeleniumAction action = createAction("keyup", "input", List.of(
                List.of("labelText", "Nom du client"),
                List.of("id", "customerName"),
                List.of("xpath", "//*[@id='customerName']")
        ));

        assertEquals("By.id(\"customerName\")", action.getSelector());
    }

    @Test
    public void shouldUseUniqueElementText() {
        SeleniumAction action = createAction("click", "span", List.of(
                List.of("elementText", "ExactTextTarget"),
                List.of("uniqueElementText", "ExactTextTarget"),
                List.of("xpath", "//span[normalize-space(.)='ExactTextTarget']")
        ));

        assertEquals("ByC.text(\"ExactTextTarget\", \"span\")", action.getSelector());
    }

    @Test
    public void shouldUseLinkTextForShortUniqueLink() {
        SeleniumAction action = createAction("click", "a", List.of(
                List.of("linkText", "Contact"),
                List.of("uniqueLinkText", "Contact"),
                List.of("css", "a[href=\"/contact\"]"),
                List.of("xpath", "//a[@href='/contact']")
        ));

        assertEquals("By.linkText(\"Contact\")", action.getSelector());
    }

    @Test
    public void shouldUseHrefForLongLinkText() {
        String longText = "Enquête Article réservé très long texte pour tester les locators générés automatiquement";

        SeleniumAction action = createAction("click", "a", List.of(
                List.of("linkText", longText),
                List.of("css", "a[href=\"/article/nucleaire-iranien\"]"),
                List.of("xpath", "//a[@href='/article/nucleaire-iranien']")
        ));

        assertTrue(action.getSelector().contains("nucleaire-iranien"));
        assertFalse(action.getSelector().contains(longText));
    }

    @Test
    public void shouldKeepIndexedXPathForDuplicatedButtons() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("buttonText", "Action"),
                List.of("css", "div#actions-container button:nth-of-type(2)"),
                List.of("xpath", "//*[@id='actions-container']//button[2]")
        ));

        assertEquals("By.xpath(\"//*[@id='actions-container']//button[2]\")", action.getSelector());
    }

    @Test
    public void shouldUseCheckboxNameAndValue() {
        SeleniumAction action = createAction("check", "input", List.of(
                List.of("checkboxNameValue", "sector[]||VALUE||54"),
                List.of("xpath", "//input[@name='sector[]' and @value='54']")
        ));

        assertEquals(
                "ByC.and(ByC.attribute(\"name\", \"sector[]\"), ByC.attribute(\"value\", \"54\"))",
                action.getSelector()
        );
    }

    @Test
    public void shouldKeepXPathRelativeToStableParent() {
        SeleniumAction action = createAction("click", "strong", List.of(
                List.of("elementText", "Résultat attendu"),
                List.of("xpath", "//*[@id='locators']//article[2]/div[2]/div[2]/strong")
        ));

        assertEquals(
                "By.xpath(\"//*[@id='locators']//article[2]/div[2]/div[2]/strong\")",
                action.getSelector()
        );
    }

    @Test
    public void shouldKeepStructuralXPathAsLastFallback() {
        SeleniumAction action = createAction("click", "span", List.of(
                List.of("elementText", "Élément sans locator stable"),
                List.of("xpath", "//main/div/div/span[2]")
        ));

        assertEquals("By.xpath(\"//main/div/div/span[2]\")", action.getSelector());
    }

    @Test
    public void shouldDetectButtonElement() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("id", "save-button")
        ));

        assertEquals("ButtonElement", action.getElementType());
    }

    @Test
    public void shouldDetectLinkElement() {
        SeleniumAction action = createAction("click", "a", List.of(
                List.of("uniqueLinkText", "Contact")
        ));

        assertEquals("LinkElement", action.getElementType());
    }

    @Test
    public void shouldDetectTextFieldFromInput() {
        SeleniumAction action = createAction("keyup", "input", List.of(
                List.of("id", "usernameInput")
        ));

        assertEquals("TextFieldElement", action.getElementType());
    }

    @Test
    public void shouldDetectTextFieldFromTextarea() {
        SeleniumAction action = createAction("keyup", "textarea", List.of(
                List.of("id", "description")
        ));

        assertEquals("TextFieldElement", action.getElementType());
    }

    @Test
    public void shouldDetectCheckBoxFromCheckCommand() {
        SeleniumAction action = createAction("check", "input", List.of(
                List.of("id", "acceptConditions")
        ));

        assertEquals("CheckBoxElement", action.getElementType());
    }

    @Test
    public void shouldDetectCheckBoxFromUncheckCommand() {
        SeleniumAction action = createAction("uncheck", "span", List.of(
                List.of("role", "checkbox"),
                List.of("ariaLabel", "Accepter les conditions")
        ));

        assertEquals("CheckBoxElement", action.getElementType());
    }

    @Test
    public void shouldDetectSelectList() {
        SeleniumAction action = createAction("select", "select", List.of(
                List.of("id", "countrySelect")
        ));

        assertEquals("SelectList", action.getElementType());
    }

    @Test
    public void shouldDetectFrameElement() {
        SeleniumAction action = createAction("selectFrame", "iframe", List.of(
                List.of("id", "testFrame")
        ));

        assertEquals("FrameElement", action.getElementType());
    }

    @Test
    public void shouldDetectImageElement() {
        SeleniumAction action = createAction("click", "img", List.of(
                List.of("data-testid", "customer-logo")
        ));

        assertEquals("ImageElement", action.getElementType());
    }

    @Test
    public void shouldFallbackToHtmlElement() {
        SeleniumAction action = createAction("click", "div", List.of(
                List.of("id", "customContainer")
        ));

        assertEquals("HtmlElement", action.getElementType());
    }

    @Test
    public void shouldGenerateClickCommand() {
        SeleniumAction action = createAction("click", "button", List.of(
                List.of("id", "save-button")
        ));

        assertTrue(action.getFormattedCommand().trim().endsWith(".click();"));
    }

    @Test
    public void shouldGenerateDoubleClickCommand() {
        SeleniumAction action = createAction("doubleClick", "button", List.of(
                List.of("id", "save-button")
        ));

        assertTrue(action.getFormattedCommand().contains("doubleClickAction()"));
    }

    @Test
    public void shouldGenerateContextMenuCommand() {
        SeleniumAction action = createAction("contextmenu", "button", List.of(
                List.of("id", "context-button")
        ));

        assertTrue(action.getFormattedCommand().trim().endsWith(".rightClickMouse();"));
    }

    @Test
    public void shouldGenerateCheckCommand() {
        SeleniumAction action = createAction("check", "input", List.of(
                List.of("id", "acceptConditions")
        ));

        assertTrue(action.getFormattedCommand().trim().endsWith(".check();"));
    }

    @Test
    public void shouldGenerateUncheckCommand() {
        SeleniumAction action = createAction("uncheck", "input", List.of(
                List.of("id", "acceptConditions")
        ));

        assertTrue(action.getFormattedCommand().trim().endsWith(".uncheck();"));
    }

    @Test
    public void shouldEscapeQuotesInLocator() {
        SeleniumAction action = createAction("keyup", "input", List.of(
                List.of("placeholder", "Saisir \"Jean\"")
        ));

        assertEquals(
                "ByC.attribute(\"placeholder\", \"Saisir \\\"Jean\\\"\")",
                action.getSelector()
        );
    }
}