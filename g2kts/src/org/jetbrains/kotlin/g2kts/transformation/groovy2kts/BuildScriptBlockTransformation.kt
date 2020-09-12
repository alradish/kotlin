/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.GradleScopeContext
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class BuildScriptBlockTransformation(scopeContext: GradleScopeContext) : Transformation(scopeContext) {
    override fun runTransformation(node: GNode): GNode {
        return if (can(node, scopeContext.getCurrentScope())) {
            val name = ((node as GMethodCall).method as GIdentifier).name
            GBuildScriptBlock(
                GBuildScriptBlock.BuildScriptBlockType.byName(name)!!,
                node.closure!!.detached()
            )
        } else node
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        return node.isBuildScriptBlock() && scope is GStatement.GExpr && scope.parent is GProject
    }

    private fun GNode.isBuildScriptBlock(): Boolean {
        if (!(this is GMethodCall && obj == null)) return false
        val name = (method as? GIdentifier)?.name
        if (GBuildScriptBlock.BuildScriptBlockType.values().find { it.text == name } == null) return false
        if (arguments.args.isNotEmpty() || closure == null) return false
        return true
    }
}


