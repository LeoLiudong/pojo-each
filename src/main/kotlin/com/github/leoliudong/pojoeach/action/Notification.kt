package com.liliudong.dynamicmybatis

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId

object Notification {
    /**
     * 显示通知
     * @param [project] 项目
     * @param [content] 所容纳之物
     * @param [title] 标题
     * @param [notificationType] 通知类型
     */
    fun showNotification(project: Project?, content: String, title: String, notificationType: NotificationType) {
        val notification = Notification(
            ToolWindowId.PROJECT_VIEW,
            title,
            content,
            notificationType
        )
        Notifications.Bus.notify(notification, project)
    }

    /**
     * 显示成功通知
     * @param [project] 项目
     * @param [content] 所容纳之物
     */
    fun showSuccessNotification(project: Project?, content: String) {
        showNotification(project, content, "Tips", NotificationType.INFORMATION)
    }

    /**
     * 显示错误通知
     * @param [project] 项目
     * @param [content] 所容纳之物
     */
    fun showErrorNotification(project: Project?, content: String) {
        showNotification(project, content, "Error", NotificationType.ERROR)
    }

    /**
     * 显示警告通知
     * @param [project] 项目
     * @param [content] 所容纳之物
     */
    fun showWarningNotification(project: Project?, content: String) {
        showNotification(project, content, "Warn", NotificationType.WARNING)
    }
}
