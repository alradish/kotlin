/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class SourceSetsConfigureTransformation(scopeContext: GradleScopeContext) : Transformation(scopeContext) {
    override fun runTransformation(node: GNode): GNode {
        // Just prevent taskConfiguration to transform this because 'test' task and source set exist
        return node
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        return if (scope?.isConfigurationBlock() == true) {
            val method = ((when(scope) {
                is GStatement.GExpr -> scope.expr
                is GExpression -> scope
                else -> unreachable()
            } as GMethodCall).method as GIdentifier).name
            method == "sourceSets" && node.isConfigurationBlock()
        } else {
            false
        }

    }
}