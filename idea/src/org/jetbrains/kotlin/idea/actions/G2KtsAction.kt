/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiManager
import kastree.ast.Node
import kastree.ast.Writer
import org.codehaus.groovy.ast.AstToTextHelper
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import org.gradle.api.internal.project.ProjectScript
import org.gradle.groovy.scripts.internal.TaskDefinitionScriptTransformer
import org.jetbrains.groovy.compiler.rt.GroovyCompilerWrapper
import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.idea.refactoring.changeSignature.isCaller
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase

fun <T> Array<T>.prettyString(name: String): String =
    joinToString(prefix = "$name:\n\t", separator = "\n\t", postfix = "\n") {
        it.toString()
    }


class G2KtsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val manager = PsiManager.getInstance(e.project!!)
        virtualFiles?.forEach { file ->
            val groovyFileBase = manager.findFile(file) as? GroovyFile ?: return
            buildTree(groovyFileBase)
            println(groovyFileBase.text)
            println("*********")
            println(groovyFileBase.typeDefinitions.prettyString("typeDef"))
            println("*********")
            println(groovyFileBase.methods.prettyString("methods"))
            println("*********")
            println(groovyFileBase.topStatements.prettyString("topSt"))
            println("*********")
            println(groovyFileBase.scriptClass)
            println("*********")
            println(groovyFileBase.imports)
            println("*********")
        }

//        val converter = G2KtsConverter()
//        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
//        val manager = PsiManager.getInstance(e.project!!)
//        virtualFiles?.forEach { file ->
//            val groovyFileBase = manager.findFile(file) as? GroovyFileBase ?: return
//            val canon = groovyFileBase.text.canonicalization()
//            val converted = converter.convert(canon) as Node.Block
//            println("res:\n${converted.stmts.joinToString(separator = "\n") { Writer.write(it) }}")
//            println("transform:\n${Writer.write(transform(converted))}")
//        }
    }
}