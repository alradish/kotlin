/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class TaskCreationTransformation : Transformation() {

    override fun runTransformation(node: GNode): GNode {
        val task = node.taskCreationOrNull() ?: return recurse(node)

        val taskName = (task.method as GIdentifier).name
        val args = task.arguments.args
        val body = task.closure?.detached()

        for (arg in args) {
            when {
                arg.name == "dependsOn" -> {
                    body?.statements?.apply {
                        statements = listOf(moveDependsOn(arg)) + statements
                    }
                }
            }
        }

        return recurse(GTaskCreating(taskName, "", body))
    }

    private fun GNode.taskCreationOrNull(): GMethodCall? {
        if (this !is GMethodCall) return null
        val method = method
        if (!(obj == null && method is GIdentifier && method.name == "task")) return null
        if (arguments.args.size != 1) return null
        val task = arguments.args.first().expr
        if (task !is GMethodCall) return null
        return task
    }

    private fun moveDependsOn(arg: GArgument): GStatement {
        val argumentList = if (arg.expr is GList) {
            GArgumentsList((arg.expr as GList).initializers.map { GArgument(null, it.detached()) })
        } else {
            GArgumentsList(listOf(GArgument(null, arg.expr.detached())))
        }
        return GSimpleMethodCall(null, GIdentifier("dependsOn"), argumentList, null).toStatement()
    }

}