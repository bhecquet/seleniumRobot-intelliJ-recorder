package io.github.bhecquet.seleniumRobot.recorder;

import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.lang.reflect.Method;

public class SeleniumServletEditorTest extends BasePlatformTestCase {

    public void testShouldCreatePageMethodAndMoveCaretInside() throws Exception {
        myFixture.configureByText(
                "Main.java",
                "public class Main {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        <caret>\n"
                        + "    }\n"
                        + "}\n"
        );

        Editor editor = myFixture.getEditor();
        SeleniumServlet servlet = new SeleniumServlet(getProject());

        SeleniumAction action = new SeleniumAction();
        action.setCommand("pageLoad");
        action.setReason("hashchange");
        action.setUrl("http://localhost/page.html#locators");
        action.setTitle("Locators");

        Method method = SeleniumServlet.class.getDeclaredMethod(
                "insertPageMethod",
                Editor.class,
                SeleniumAction.class
        );

        method.setAccessible(true);
        method.invoke(servlet, editor, action);

        String result = editor.getDocument().getText();

        assertTrue(result.contains("locatorsPage();"));
        assertTrue(result.contains("private static void locatorsPage()"));
        assertTrue(
                result.indexOf("locatorsPage();")
                        < result.indexOf("private static void locatorsPage()")
        );

        int caretOffset = editor.getCaretModel().getOffset();
        int methodStart = result.indexOf("private static void locatorsPage()");
        int methodEnd = result.indexOf("}", methodStart);

        assertTrue(caretOffset > methodStart);
        assertTrue(caretOffset < methodEnd);
    }

    public void testShouldCreatePageObjectFileAndInsertCall() throws Exception {
        myFixture.configureByText(
                "Main.java",
                "package covea.pic.jenkins.tests.selenium;\n\n"
                        + "public class Main {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        <caret>\n"
                        + "    }\n"
                        + "}\n"
        );

        Editor sourceEditor = myFixture.getEditor();
        SeleniumServlet servlet = new SeleniumServlet(getProject());

        SeleniumAction action = new SeleniumAction();
        action.setCommand("pageLoad");
        action.setReason("hashchange");
        action.setUrl("http://localhost/page.html#locators");
        action.setTitle("Locators");

        Method method = SeleniumServlet.class.getDeclaredMethod(
                "insertPageObject",
                Editor.class,
                SeleniumAction.class
        );

        method.setAccessible(true);

        Editor pageEditor = (Editor) method.invoke(
                servlet,
                sourceEditor,
                action
        );

        String mainContent = sourceEditor.getDocument().getText();

        assertTrue(mainContent.contains("LocatorsPage.execute();"));
        assertNotNull(pageEditor);
        assertTrue(pageEditor.getDocument().getText().contains("public class LocatorsPage"));
        assertTrue(pageEditor.getDocument().getText().contains("public static void execute()"));

        int caretOffset = pageEditor.getCaretModel().getOffset();
        int methodStart = pageEditor.getDocument().getText().indexOf("public static void execute()");
        int methodEnd = pageEditor.getDocument().getText().indexOf("}", methodStart);

        assertTrue(caretOffset > methodStart);
        assertTrue(caretOffset < methodEnd);
    }
}