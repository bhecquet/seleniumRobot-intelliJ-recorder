package io.github.bhecquet.seleniumRobot.recorder;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class PageClassManager {

    private final Project project;

    public PageClassManager(Project project) {
        this.project = project;
    }

    public String buildClassName(String pageMethodName) {
        if (pageMethodName == null || pageMethodName.isBlank()) {
            return "NewPage";
        }

        String normalizedName = pageMethodName.trim();

        if (!normalizedName.endsWith("Page")) {
            normalizedName += "Page";
        }

        return Character.toUpperCase(normalizedName.charAt(0)) + normalizedName.substring(1);
    }

    public String buildFileName(String pageMethodName) {
        return buildClassName(pageMethodName) + ".java";
    }

    public String buildMethodCall(String pageMethodName) {
        return buildClassName(pageMethodName) + ".execute();";
    }

    public String buildClassContent(String packageName, String pageMethodName) {
        String className = buildClassName(pageMethodName);
        String packageDeclaration = packageName == null || packageName.isBlank()
                ? ""
                : "package " + packageName + ";\n\n";

        return packageDeclaration
                + "public class " + className + " {\n\n"
                + "    public static void execute() {\n\n"
                + "    }\n"
                + "}\n";
    }

    public VirtualFile createOrFindPageFile(VirtualFile sourceDirectory, String packageName, String pageMethodName) {
        if (sourceDirectory == null || !sourceDirectory.isDirectory()) {
            throw new IllegalArgumentException("Le dossier source est invalide");
        }

        String fileName = buildFileName(pageMethodName);
        VirtualFile existingFile = sourceDirectory.findChild(fileName);

        if (existingFile != null) {
            return existingFile;
        }

        AtomicReference<VirtualFile> createdFile = new AtomicReference<>();
        AtomicReference<IOException> creationError = new AtomicReference<>();

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile file = sourceDirectory.createChildData(this, fileName);
                VfsUtil.saveText(file, buildClassContent(packageName, pageMethodName));
                createdFile.set(file);
            } catch (IOException error) {
                creationError.set(error);
            }
        });

        if (creationError.get() != null) {
            throw new IllegalStateException("Impossible de créer le fichier " + fileName, creationError.get());
        }

        return createdFile.get();
    }

    public Editor openPageEditor(VirtualFile pageFile) {
        if (project == null) {
            throw new IllegalStateException("Le projet IntelliJ est obligatoire");
        }

        if (pageFile == null || pageFile.isDirectory()) {
            throw new IllegalArgumentException("Le fichier Page Object est invalide");
        }

        int bodyOffset = findExecuteBodyOffset(readFileContent(pageFile));
        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, pageFile, bodyOffset);
        Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);

        if (editor == null) {
            throw new IllegalStateException("Impossible d’ouvrir " + pageFile.getName());
        }

        editor.getCaretModel().moveToOffset(bodyOffset);
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);

        return editor;
    }

    public int findExecuteBodyOffset(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Le contenu Java est vide");
        }

        String signature = "public static void execute()";
        int methodStart = content.indexOf(signature);

        if (methodStart < 0) {
            throw new IllegalStateException("La méthode execute() est absente");
        }

        int openingBrace = content.indexOf('{', methodStart);

        if (openingBrace < 0) {
            throw new IllegalStateException("Le corps de execute() est invalide");
        }

        int offset = openingBrace + 1;

        while (offset < content.length() && (content.charAt(offset) == '\r' || content.charAt(offset) == '\n')) {
            offset++;
        }

        return offset;
    }

    private String readFileContent(VirtualFile file) {
        try {
            return VfsUtilCore.loadText(file);
        } catch (IOException error) {
            throw new IllegalStateException("Impossible de lire " + file.getName(), error);
        }
    }

    public Project getProject() {
        return project;
    }
}