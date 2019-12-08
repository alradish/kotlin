/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class BuildScriptBlockTransformation : Transformation {
    override fun runTransformation(node: GNode): GNode {
        return if (node.isBuildScriptBlock()) {
            recurse(
                GBuildScriptBlock(
                    GBuildScriptBlock.BuildScriptBlockType.byName(((node as GMethodCall).method as GIdentifier).name)!!,
                    node.closure!!.detached()
                )
            )
        } else recurse(node)
    }

    private fun GNode.isBuildScriptBlock(): Boolean {
        if (this !is GMethodCall) return false
        if (obj != null) return false
        if (GBuildScriptBlock.BuildScriptBlockType.values().find { it.text == (method as? GIdentifier)?.name } == null) return false
        if (arguments.args.isNotEmpty()) return false
        if (closure == null) return false
        return true
    }
}