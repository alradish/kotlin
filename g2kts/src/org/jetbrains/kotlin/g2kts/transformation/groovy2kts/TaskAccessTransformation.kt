/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.GIdentifier
import org.jetbrains.kotlin.g2kts.GNode
import org.jetbrains.kotlin.g2kts.GSimpleTaskAccess
import org.jetbrains.kotlin.g2kts.transformation.GradleBuildContext
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class TaskAccessTransformation(override val context: GradleBuildContext) : Transformation(context) {
    override fun runTransformation(node: GNode): GNode {
        if (node !is GIdentifier) return recurse(node)
        val task = context.getTaskByName(node.name)
        return if(task != null) {
            recurse(GSimpleTaskAccess(task.name, task.type, node.psi))
        } else {
            recurse(node)
        }
    }
}