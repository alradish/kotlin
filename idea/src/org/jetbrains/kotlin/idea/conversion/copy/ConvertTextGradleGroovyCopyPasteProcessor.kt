/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import kastree.ast.Node
import kastree.ast.Writer
import org.jetbrains.kotlin.g2kts.GProject
import org.jetbrains.kotlin.g2kts.gradleAstBuilder.buildTree
import org.jetbrains.kotlin.g2kts.toKotlin
import org.jetbrains.kotlin.idea.editor.KotlinEditorOptions
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.GroovyLanguage
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

class ConvertTextGradleGroovyCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance(ConvertTextGradleGroovyCopyPasteProcessor::class.java)

    private class MyTransferableData(val text: String) : TextBlockTransferableData {

        override fun getFlavor() = DATA_FLAVOR
        override fun getOffsetCount() = 0

        override fun getOffsets(offsets: IntArray?, index: Int) = index
        override fun setOffsets(offsets: IntArray?, index: Int) = index

        companion object {
            val DATA_FLAVOR: DataFlavor = DataFlavor(
                ConvertTextGradleGroovyCopyPasteProcessor::class.java,
                "class: ConvertTextGradleGroovyCopyPasteProcessor"
            )
        }
    }

    override fun collectTransferableData(
        file: PsiFile?,
        editor: Editor?,
        startOffsets: IntArray?,
        endOffsets: IntArray?
    ): List<TextBlockTransferableData> {
        return emptyList()
    }

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val text = content.getTransferData(DataFlavor.stringFlavor) as String
                return listOf(MyTransferableData(text))
            }
        } catch (e: Throwable) {
            LOG.error(e)
        }
        return emptyList()
    }

    override fun processTransferableData(
        project: Project,
        editor: Editor,
        bounds: RangeMarker,
        caretOffset: Int,
        indented: Ref<Boolean>,
        values: List<TextBlockTransferableData>
    ) {
        if (DumbService.getInstance(project).isDumb) return

        val text = (values.single() as MyTransferableData).text

        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        psiDocumentManager.commitDocument(editor.document)
        val targetFile = psiDocumentManager.getPsiFile(editor.document) as? KtFile ?: return
        val targetModule = targetFile.module

        if (confirmConvertGroovyOnPaste(project, isPlainText = false)) {
            val file = PsiFileFactory.getInstance(project).createFileFromText(GroovyLanguage, text) as GroovyFileBase
            val converted = ((buildTree(file) as GProject).toKotlin() as Node.Block).stmts.joinToString(separator = "\n") { Writer.write(it) }
            runWriteAction {
                editor.document.replaceString(bounds.startOffset, bounds.endOffset, converted)
                editor.caretModel.moveToOffset(bounds.startOffset + converted.length)
            }
            psiDocumentManager.commitAllDocuments()
        }


    }
}

internal fun confirmConvertGroovyOnPaste(project: Project, isPlainText: Boolean): Boolean {
    if (KotlinEditorOptions.getInstance().isDonTShowConversionDialogKts2) return true
    val dialog = KtsPasteFromGroovyDialog(project, isPlainText)
    dialog.show()
    return dialog.isOK
}