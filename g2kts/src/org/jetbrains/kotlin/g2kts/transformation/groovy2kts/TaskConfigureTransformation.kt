/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.GradleBuildContext
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class TaskConfigureTransformation(override val context: GradleBuildContext) : Transformation(context) {
    override fun runTransformation(node: GNode): GNode {
        if (node !is GMethodCall) return recurse(node)
        return if (isTaskConfigure(node)) {
            val name = (node.method as GIdentifier).name
            recurse(
                GTaskConfigure(
                    name,
                    context.getTaskByName(name)?.type?.kotlinString,
                    node.closure!!.detached()
                )
            )
        } else recurse(node)

    }

    private fun isTaskConfigure(node: GMethodCall): Boolean {
        val name = (node.method as? GIdentifier)?.name
        val tasks = context.internalTypedProjectSchema.tasks.map { it.name }
        return node.obj == null && name in tasks && node.closure != null && node.arguments.args.isEmpty()
    }
}