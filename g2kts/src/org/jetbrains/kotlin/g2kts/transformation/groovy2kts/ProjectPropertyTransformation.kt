/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.GradleScopeContext
import org.jetbrains.kotlin.g2kts.transformation.Transformation
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall

class ProjectPropertyTransformation(scopeContext: GradleScopeContext) : Transformation(scopeContext) {
    override fun runTransformation(node: GNode): GNode {
        return if (check(node) >= 0) {
            node as GMethodCall
            GBinaryExpression(
                GSimplePropertyAccess(node.obj?.detached(), node.method.detached()),
                GBinaryOperator.byValue("="),
                node.arguments.args.first().expr.detached()
            )
        } else node
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        val gMethodCall = node as? GMethodCall ?: return false
        val grMethodCall = node.psi as? GrMethodCall ?: return false
        val containingClassIsProject = grMethodCall.resolveMethod()?.containingClass?.qualifiedName == "org.gradle.api.Project"
        val inVars = grMethodCall.invokedExpression.text in vars
        val inScope = scope?.topParent(GProject::class)?.let { true } ?: false
        return containingClassIsProject && inVars && inScope
    }
}