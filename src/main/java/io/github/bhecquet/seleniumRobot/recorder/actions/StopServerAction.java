package io.github.bhecquet.seleniumRobot.recorder.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import io.github.bhecquet.seleniumRobot.recorder.ChromeStarter;
import io.github.bhecquet.seleniumRobot.recorder.SeleniumActionServer;
import org.jetbrains.annotations.NotNull;


public class StopServerAction extends AnAction {

    /**
     * Menu enabled when server is not started
     * @param event
     */
    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null && SeleniumActionServer.isStarted());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        if (SeleniumActionServer.isStarted()) {
            SeleniumActionServer.stopServletServer();
            event.getPresentation().setEnabledAndVisible(false);
            Notification notification = new Notification("io.github.bhecquet.seleniumRobot.recorder", "Selenium record stopped", NotificationType.INFORMATION);
            Notifications.Bus.notify(notification);
        }
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
