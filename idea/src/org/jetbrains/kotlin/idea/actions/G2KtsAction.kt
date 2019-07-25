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
import org.jetbrains.kotlin.g2kts.G2KtsConverterWithoutVisitor
import org.jetbrains.kotlin.g2kts.G2KtsVisitor
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase

class G2KtsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("G2KtsAction.actionPerformed(e = [${e}])")
        val visitor = G2KtsVisitor()
        val converter = G2KtsConverterWithoutVisitor()
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val manager = PsiManager.getInstance(e.project!!)
        virtualFiles?.forEach { file ->

            val groovyFileBase = manager.findFile(file) as? GroovyFileBase
            GradleCompile
            groovyFileBase?.let(converter::convert)?.let {
                Writer.write(it)
            }?.let {
                println("WRITE $it")
            }
//            (manager.findFile(file) as? GroovyFileBase)?.acceptChildren(visitor)
//            manager.findFile(file)?.firstChild?.accept(visitor)
//            val node = (manager.findFile(file)?.children?.get(2) as? GroovyPsiElement)?.let(visitor::convert) ?: return
//            println(Writer.write(node))
        }
    }
}