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
}