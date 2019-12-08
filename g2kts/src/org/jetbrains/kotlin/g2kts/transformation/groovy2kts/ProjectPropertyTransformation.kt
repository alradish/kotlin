/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.Transformation
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall

class ProjectPropertyTransformation : Transformation {
    override fun runTransformation(node: GNode): GNode {
        if (node !is GMethodCall) return recurse(node)
        val methodCall = node.psi as? GrMethodCall ?: return recurse(node)
        return if (methodCall.resolveMethod()?.containingClass?.qualifiedName == "org.gradle.api.Project" && methodCall.invokedExpression.text in vars) {
            recurse(
                GBinaryExpression(
                    GSimplePropertyAccess(node.obj?.detached(), node.method.detached()),
                    GOperator.byValue("="),
                    node.arguments.args.first().expr.detached()
                )
            )
        } else recurse(node)

    }
}