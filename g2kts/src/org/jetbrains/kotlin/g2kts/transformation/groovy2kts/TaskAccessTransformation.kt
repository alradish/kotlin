/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.GradleBuildContext
import org.jetbrains.kotlin.g2kts.transformation.GradleScopeContext
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class TaskAccessTransformation(override val context: GradleBuildContext, scopeContext: GradleScopeContext) : Transformation(scopeContext) {
    override fun runTransformation(node: GNode): GNode {
        if (check(node) == -1) return node
        val task = context.getTaskByName((node as GIdentifier).name)!!
        return GSimpleTaskAccess(task.name, task.type.kotlinString, node.psi)
    }


    // FIXME many false positive!
    override fun can(node: GNode, scope: GNode?): Boolean {
        if (node !is GIdentifier) return false
        if (node.parent is GMethodCall) return false
        val task = context.getTaskByName(node.name) ?: return false
        if (scope !is GProject) return false
        return true
    }
}