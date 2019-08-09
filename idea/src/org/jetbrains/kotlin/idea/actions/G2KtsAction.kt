/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiManager
import kastree.ast.Node
import kastree.ast.Writer
import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase


class G2KtsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val converter = G2KtsConverter()
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val manager = PsiManager.getInstance(e.project!!)
        virtualFiles?.forEach { file ->
            val groovyFileBase = manager.findFile(file) as? GroovyFileBase ?: return
            println(Writer.write(buildTree(groovyFileBase).toKotlin()))
//            val canon = groovyFileBase.text.canonicalization()
//            canon.toGNode()
//            val converted = converter.convert(canon) as Node.Block
//            println("res:\n${converted.stmts.joinToString(separator = "\n") { Writer.write(it) }}")
//            println("transform:\n${Writer.write(transform(converted))}")
        }
    }
}