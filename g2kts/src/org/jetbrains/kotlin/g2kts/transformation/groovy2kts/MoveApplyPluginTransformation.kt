/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class MoveApplyPluginTransformation(scopeContext: GradleScopeContext) : Transformation(scopeContext) {
    override fun runTransformation(node: GNode): GNode {
        if (!can(node, scopeContext.getCurrentScope())) return node
        node as GProject
        // find apply plugin in script
        val applyPlugins = node.statements.mapNotNull {
            it.applyPluginOrNull()
        }
        val plugins = applyPlugins.map {
            (((it as GStatement.GExpr).expr as GSimpleMethodCall).arguments.args.first().expr as GString).detached()
        }
        val ids = plugins.map {
            if (COMMON_PLUGINS.containsKey(it.str))
                (COMMON_PLUGINS.getValue(it.str).copy() as GExpression).toStatement()
            else {
                GSimpleMethodCall(
                    null,
                    GIdentifier("id"),
                    emptyList(),
                    GArgumentsList(listOf(GArgument(null, it))),
                    null,
                    null
                ).toStatement()
            }
        }
        // find existing plugins configuration block or create one
        val pluginsBlock = (node.statements.find {
            if (!it.isConfigurationBlock()) return@find false
            val expr = (it as GStatement.GExpr).expr as GMethodCall
            (expr.method as GIdentifier).name == "plugins"
        } as? GStatement.GExpr)?.expr as? GMethodCall
        return if (pluginsBlock != null) {
            val statements = pluginsBlock.closure!!.statements.statements.detached()
            pluginsBlock.closure!!.statements = GBlock(statements + ids)
            GProject(
                node.statements.filter { !applyPlugins.contains(it) }.detached(),
                node.psi
            )
        } else {
            val newPluginsBlock = GSimpleMethodCall(
                null,
                GIdentifier("plugins"),
                emptyList(),
                GArgumentsList(emptyList()),
                GClosure(emptyList(), GBlock(ids))
            )
            // FIXME should check for more than one build script block
            if (node.firstIsBuildScriptBlock()) {
                GProject(
                    node.statements.subList(0, 1).detached()
                            + listOf(newPluginsBlock.toStatement())
                            + node.statements.subList(1, node.statements.size).detached()
                )
            } else {
                GProject(
                    listOf(newPluginsBlock.toStatement()) + node.statements.detached()
                )
            }
        }
    }

    private fun GProject.firstIsBuildScriptBlock(): Boolean {
        return if (statements.first() is GStatement.GExpr) {
            val s = (statements.first() as GStatement.GExpr).expr
            s is GBuildScriptBlock
        } else {
            false
        }
    }

    private fun GStatement.applyPluginOrNull(): GStatement? {
        if (this !is GStatement.GExpr) return null
        val expr = expr
        if (expr !is GSimpleMethodCall) return null
        if (expr.obj != null || expr.method !is GIdentifier && (expr.method as GIdentifier).name != "apply") return null
        if (expr.arguments.args.size != 1) return null
        val arg = expr.arguments.args.first()
        if (arg.name != "plugin") return null
        return this
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        return node is GProject && scope == null
    }

    companion object {
        val COMMON_PLUGINS = mapOf(
            "java" to GIdentifier("java"),
            "kotlin" to GSimpleMethodCall(
                GSimpleMethodCall(
                    null,
                    GIdentifier("kotlin"),
                    emptyList(),
                    GArgumentsList(listOf(GArgument(null, GString("jvm")))),
                    null,
                    null
                ),
                GIdentifier("version"),
                emptyList(),
                GArgumentsList(listOf(GArgument(null, GString("\$kotlinVersion")))),
                null,
                null
            )
        )
    }
}