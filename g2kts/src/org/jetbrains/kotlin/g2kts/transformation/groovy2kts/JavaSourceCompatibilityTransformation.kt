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
        if (node !is GBinaryExpression) return recurse(node)
        return if (node.left is GIdentifier && (node.left as GIdentifier).name == "sourceCompatibility" && node.parent?.parent is GProject) {
//            val detached = node.detached()
            val sourceCompatibility = GBinaryExpression(
                GIdentifier("sourceCompatibility", node.left.psi),
                GBinaryOperator.byValue("="),
                node.right.detached()
            )
            GConfigurationBlock(
                null,
                GIdentifier("java"),
                GArgumentsList(emptyList(), null),
                GClosure(emptyList(), GBlock(listOf(sourceCompatibility.toStatement())), null),
                null
            )
        } else {
            recurse(node)
        }
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        TODO("Not yet implemented")
    }
}