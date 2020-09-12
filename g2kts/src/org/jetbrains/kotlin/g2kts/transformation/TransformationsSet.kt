/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.g2kts.GNode
import org.jetbrains.kotlin.g2kts.GradleScopeContext

class TransformationsSet(private val transformations: List<Transformation>, scopeContext: GradleScopeContext) :
    Transformation(scopeContext) {

    override fun runTransformation(node: GNode): GNode {
        val (i, transformation) = transformations
            .map { it.check(node) to it }
            .filter { it.first >= 0 }
            .minBy { it.first } ?: return recurse(node)
        val newNode = transformation.runTransformation(node)
        return recurse(newNode)
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        return true
    }

    override fun <T : GNode> recurse(element: T): T {
        scopeContext.newScope(element)
        val node = applyRecursive(element, this::runTransformation)
        scopeContext.leaveScope()
        return node
    }
}