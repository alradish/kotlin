/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiManager
import kastree.ast.Writer
import org.jetbrains.kotlin.g2kts.gradleAstBuilder.buildTree
import org.jetbrains.kotlin.g2kts.toKotlin
import org.jetbrains.kotlin.g2kts.transformation.GradleTransformer
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase


class G2KtsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val manager = PsiManager.getInstance(e.project!!)

        virtualFiles?.forEach { file ->
            val groovyFileBase = manager.findFile(file) as? GroovyFileBase ?: return
            val groovyGradleTree = buildTree(groovyFileBase)
            val gradleTree = GradleTransformer.doApply(listOf(groovyGradleTree)).first()
            val kotlin = Writer.write(gradleTree.toKotlin())

            /**
             * val psiDocumentManager = PsiDocumentManager.getInstance(project)
            psiDocumentManager.commitDocument(editor.document)
            val targetFile = psiDocumentManager.getPsiFile(editor.document) as? KtFile ?: return
            val targetModule = targetFile.module

            val file = PsiFileFactory.getInstance(project).createFileFromText(GroovyLanguage, text) as GroovyFileBase
            val converted = ((buildTree(file) as GProject).toKotlin() as Node.Block).stmts.joinToString(separator = "\n") { Writer.write(it) }
            runWriteAction {
            editor.document.replaceString(bounds.startOffset, bounds.endOffset, converted)
            editor.caretModel.moveToOffset(bounds.startOffset + converted.length)
            }
            psiDocumentManager.commitAllDocuments()
             */
//            val psiDocumentManager = PsiDocumentManager.getInstance(project)
//            psiDocumentManager.commitDocument(edito)

            println(kotlin)
        }
    }
}