package com.github.leoliudong.pojoeach.action

import ai.grazie.utils.capitalize
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.liliudong.dynamicmybatis.Notification
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor


class CodeGenerationButtonAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // 获取当前的项目和编辑器
        val project: Project? = e.project
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)

        if (project == null || editor == null) {
            return
        }
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val importStatements = PsiTreeUtil.findChildrenOfType(file, PsiImportStatement::class.java)
        // 提取 import 信息
        val importList = mutableListOf<String>()

        for (importStatement in importStatements) {
            importList.add(importStatement.qualifiedName ?: "")
        }
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        // 获取剪贴板中的文本数据
        val clipboardText = getClipboardText(project, clipboard)
        // 匹配剪切板的类
        val fromPojoClassName = importList.first { item -> item.split(".").contains(clipboardText) }
        if (fromPojoClassName.isEmpty()) {
            Notification.showWarningNotification(project, "剪切板中没有内容")
        }
        // 匹配选中的类
        val selectedText: String? = editor.selectionModel.selectedText
        if (selectedText.isNullOrEmpty()) {
            Notification.showWarningNotification(project, "请选择正确的类名")
        }
        val targetPojoClassName = importList.first { item -> item.split(".").contains(selectedText!!) }
        if (targetPojoClassName.isEmpty()) {
            Notification.showWarningNotification(project, "请选择正确的类名")
        }

        val fromPsiClass = JavaPsiFacade.getInstance(project)
            .findClass(fromPojoClassName, GlobalSearchScope.allScope(project))

        val targetPsiClass = JavaPsiFacade.getInstance(project)
            .findClass(targetPojoClassName, GlobalSearchScope.allScope(project))

        if (fromPsiClass == null || targetPsiClass == null) {
            Notification.showErrorNotification(project, "选中的文本无法获取到其对象信息")
        }

        val fromFields = fromPsiClass!!.fields
            .map { field -> field.name }
            .toTypedArray()

        val targetFields = targetPsiClass!!.fields
            .map { field -> field.name }
            .toTypedArray()

        // 获取选中行文本
        val currentLineText = getLineText(editor.document, editor.caretModel.logicalPosition.line)
        // 提取对象名称
        val fromObjectName = clipboardText.replaceFirstChar { if (it.isUpperCase()) it.lowercaseChar() else it }
        val objectName = getObjectName(currentLineText)

        WriteCommandAction.runWriteCommandAction(project) {
            targetFields.map { name ->
                {
                    val capitalize = name.capitalize()
                    if (fromFields.contains(name)) {
                        "$objectName.set$capitalize($fromObjectName.get$capitalize());"
                    } else {
                        "$objectName.set$capitalize();"
                    }
                }
            }.forEach { text ->
                val document: Document = editor.document
                val caretModel = editor.caretModel
                val currentLine = caretModel.logicalPosition.line
                // 获取当前行的空白字符
                val lineText = getLineText(document, currentLine)
                val indentString = getIndentation(lineText)
                val nextLineStartOffset = document.getLineEndOffset(currentLine) + 1
                // 在下一行插入文本，并使用相同的空白字符对齐
                document.insertString(nextLineStartOffset, "$indentString${text()}\n")
                // 移动光标到插入文本的末尾
                caretModel.moveToOffset(nextLineStartOffset)
                // 通知 PsiDocumentManager 文档已被修改
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
        }
    }

    /**
     * 获取剪贴板文本
     * @param [project] 项目
     * @param [clipboard] 剪贴板
     * @return [String]
     */
    private fun getClipboardText(project: Project?, clipboard: Clipboard): String {
        return try {
            clipboard.getData(DataFlavor.stringFlavor)?.toString() ?: ""
        } catch (e: Exception) {
            Notification.showErrorNotification(project, "Error: ${e.message}")
            ""
        }
    }


    /**
     * 获取缩进
     * @param [lineText] 行文本
     * @return [String]
     */
    private fun getIndentation(lineText: String): String {
        // 获取当前行的空白字符
        val trimmedLineText = lineText.trimStart()
        val indentationLength = lineText.length - trimmedLineText.length

        // 返回当前行的空白字符
        return if (indentationLength > 0) {
            lineText.substring(0, indentationLength)
        } else {
            ""
        }
    }

    /**
     * 获取行文本
     * @param [document] 文件
     * @param [line] 线
     * @return [@NlsSafe String]
     */
    private fun getLineText(
        document: Document,
        line: Int
    ): String {
        val lineStartOffset = document.getLineStartOffset(line)
        val lineEndOffset = document.getLineEndOffset(line)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        return lineText
    }

    /**
     * 获取对象名称
     * @param [input] 输入
     * @return [String?]
     */
    private fun getObjectName(input: String): String? {
        // 定义正则表达式
        val regex = Regex("""\b(\w+)\s*=""")
        // 匹配字符串
        val matchResult = regex.find(input)
        // 提取匹配结果中的对象名
        return matchResult?.groupValues?.get(1)
    }
}
