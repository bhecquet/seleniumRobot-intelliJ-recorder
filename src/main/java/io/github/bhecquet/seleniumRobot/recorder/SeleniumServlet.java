package io.github.bhecquet.seleniumRobot.recorder;

import com.google.gson.Gson;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SeleniumServlet  extends HttpServlet {

    private Project project;

    public SeleniumServlet(Project project) {
        this.project = project;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {

            try {
                SeleniumAction action = new Gson().fromJson(request.getReader(), SeleniumAction.class);

                insertElement(editor, action);
                insertElementAction(editor, action);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        response.setStatus(200);
        try {
            response.getOutputStream().print("OK");
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Insert element into class if it does not exist
     * @param editor
     * @param seleniumAction
     */
    private String insertElement(Editor editor, SeleniumAction seleniumAction) {

        String code = seleniumAction.getWebElementString();

        // get position right after class declaration
        String content = editor.getDocument().getText();

        // do not recreate element if it already exist
        if (!content.contains(seleniumAction.getElementName())) {
            int firstElementPosition = Math.max(0, content.indexOf("{")) + 1;

            WriteCommandAction.runWriteCommandAction(project, () ->
                    editor.getDocument().insertString(firstElementPosition, code)
            );
        }
        return null;
    }

    private void insertElementAction(Editor editor, SeleniumAction seleniumAction) {

        String code = seleniumAction.getFormattedCommand();

        WriteCommandAction.runWriteCommandAction(project, () -> {
                    int caretOffset = editor.getCaretModel().getCurrentCaret().getOffset();
                    editor.getDocument().insertString(caretOffset, code);
                    editor.getCaretModel().getCurrentCaret().moveToOffset(caretOffset + code.length());
                }
        );
    }


}
