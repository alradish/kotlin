/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class TaskConfigureTransformation : Transformation {
    override fun runTransformation(node: GNode): GNode {
        /*
        gobj == null && gmethod.name in tasks.keys && hasClosureArguments() && argumentList.isEmpty -> {
            GTaskConfigure(
                gmethod.name,
                tasks.getValue(gmethod.name),
                closureArguments.last().toGradleAst()
            )
        }
         */
        if (node !is GMethodCall) return recurse(node)
        return if (node.obj == null && (node.method as? GIdentifier)?.name in tasks.keys && node.closure != null && node.arguments.args.isEmpty()) {
            val name = (node.method as GIdentifier).name
            recurse(
                GTaskConfigure(
                    name,
                    tasks.getValue(name),
                    node.closure!!.detached()
                )
            )
        } else recurse(node)

    }
}