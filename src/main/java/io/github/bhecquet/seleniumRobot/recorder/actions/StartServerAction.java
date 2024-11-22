package io.github.bhecquet.seleniumRobot.recorder.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import io.github.bhecquet.seleniumRobot.recorder.ChromeStarter;
import io.github.bhecquet.seleniumRobot.recorder.SeleniumActionServer;


public class StartServerAction extends AnAction {

    /**
     * Menu enabled when server is not started
     * @param event
     */
    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null && !SeleniumActionServer.isStarted());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        String groupId = "io.github.bhecquet.seleniumRobot.recorder";

        // Using the event, create and show a dialog
        Project currentProject = event.getProject();

        if (!SeleniumActionServer.isStarted()) {

            try {
                SeleniumActionServer.startervletServer(currentProject, 5222);

                // start chrome with Selenium IDE extension
                ChromeStarter chromeStarter = new ChromeStarter();
                boolean chromeAlreadyStarted = chromeStarter.startChrome(chromeStarter.getChromePath());
                if (chromeAlreadyStarted) {
                    Notification notification = new Notification(groupId, "Chrome already present, load extension manually from " + chromeStarter.getExtensionPath(), NotificationType.WARNING);
                    Notifications.Bus.notify(notification);
                }
            } catch (Exception e) {
                Notification notification = new Notification(groupId, "Cannot start Selenium record " + e.getMessage(), NotificationType.ERROR);
                Notifications.Bus.notify(notification);
                throw new RuntimeException(e);
            }
            event.getPresentation().setEnabledAndVisible(false);
            Notification notification = new Notification(groupId, "Selenium record started", NotificationType.INFORMATION);
            Notifications.Bus.notify(notification);
        }
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
