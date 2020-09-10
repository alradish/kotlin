/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.GConfigurationBlock
import org.jetbrains.kotlin.g2kts.GMethodCall
import org.jetbrains.kotlin.g2kts.GNode
import org.jetbrains.kotlin.g2kts.detached
import org.jetbrains.kotlin.g2kts.transformation.GradleScopeContext
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class ConfigurationBlockTransformation(scopeContext: GradleScopeContext) : Transformation(scopeContext) {
    override fun runTransformation(node: GNode): GNode {
        return if (node is GMethodCall && node.arguments.args.isEmpty() && node.closure != null) {
            recurse(
                GConfigurationBlock(
                    node.obj?.detached(),
                    node.method.detached(),
                    node.arguments.detached(),
                    node.closure!!.detached()
                )
            )
        } else recurse(node)
    }

    override fun can(node: GNode, scope: GNode?): Boolean {
        TODO("Not yet implemented")
    }
}