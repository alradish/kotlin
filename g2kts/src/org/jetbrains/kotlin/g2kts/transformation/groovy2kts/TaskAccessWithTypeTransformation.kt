/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class TaskAccessWithTypeTransformation(scopeContext: GradleScopeContext) : Transformation(scopeContext) {
    override fun runTransformation(node: GNode): GNode {
        if (check(node) == -1) return node
        node as GSimpleMethodCall

        return GSimpleMethodCall(
            node.obj?.detached(),
            node.method.detached(),
            listOf((node.arguments.args.first().expr as GIdentifier).name),
            GArgumentsList(emptyList(), null),
            node.closure?.detached(),
            node.psi
        )
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        if (scope !is GProject) return false
        if (node !is GSimpleMethodCall) return false

        if (node.obj !is GIdentifier || node.method !is GIdentifier) return false
        val obj = node.obj as GIdentifier
        val method = node.method as GIdentifier

        if (obj.name != "tasks" || method.name != "withType") return false
        if (node.arguments.args.size != 1) return false

        return true
    }
}