/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.GradleScopeContext
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class JavaSourceCompatibilityTransformation(scopeContext: GradleScopeContext) : Transformation(scopeContext) {
    override fun runTransformation(node: GNode): GNode {
        if (!can(node, scopeContext.getCurrentScope())) return node
        node as GProject
        val jStatements = node.statements.mapNotNull {
            it.javaBlockPropertyOrNull()?.detached()
        }
        val javaBlock = (node.statements.find {
            if (!it.isConfigurationBlock()) return@find false
            val expr = (it as GStatement.GExpr).expr as GMethodCall
            (expr.method as GIdentifier).name == "java"
        } as? GStatement.GExpr)?.expr as? GMethodCall


        return if (javaBlock != null) {
            val statements = javaBlock.closure!!.statements.statements.detached()
            javaBlock.closure!!.statements = GBlock(statements + jStatements)
            GProject(
                node.statements.filter { !jStatements.contains(it) }.detached(),
                node.psi
            )
        } else {
            val newJavaBlock = GSimpleMethodCall(
                null,
                GIdentifier("java"),
                emptyList(),
                GArgumentsList(emptyList()),
                GClosure(emptyList(), GBlock(jStatements))
            )
            // FIXME should check for more than one build script block
            if (node.firstIsBuildScriptBlock()) {
                val s = node.statements.filter { !jStatements.contains(it) }.detached()
                GProject(
                    s.subList(0, 1).detached()
                            + listOf(newJavaBlock.toStatement())
                            + s.subList(1, s.size).detached()
                )
            } else {
                val s = node.statements.filter { !jStatements.contains(it) }.detached()
                GProject(
                    listOf(newJavaBlock.toStatement()) + s
                )
            }
        }
    }

    private fun GNode.javaBlockPropertyOrNull(): GStatement? {
        if (this !is GStatement.GExpr) return null
        val expr = expr as? GBinaryExpression ?: return null
        val left = expr.left as? GIdentifier ?: return null
        val operator = expr.operator
        if (JAVA_BLOCK.contains(left.name) && operator is GBinaryOperator.Common && operator.token == GBinaryOperator.Token.ASSN) {
            return this
        }
        return null
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        return node is GProject && scope == null
    }

    companion object {
        val JAVA_BLOCK = listOf(
            "sourceCompatibility",
            "targetCompatibility"
        )
    }
}