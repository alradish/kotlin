/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.g2kts.GNode
import org.jetbrains.kotlin.g2kts.transformation.groovy2kts.*

class GradleTransformer(val context: GradleBuildContext) {
    private val scope = GradleScopeContext()

    private val transformations: TransformationsSet = TransformationsSet(TransformationsBuilder<Transformation>().apply {
        +BuildScriptBlockTransformation(scope)
        +ProjectPropertyTransformation(scope)
//            +TaskCreationTransformation()
//            +ConfigurationBlockTransformation()
//            +TaskConfigureTransformation(context)
//            +TaskAccessTransformation(context)
//            +NamedDomainObjectCollectionTransformation(context)
    }.transformations, scope)


    fun doApply(node: GNode): GNode {
        return transformations.runTransformation(node)
    }

    fun doApply(code: List<GNode>): List<GNode> {
        return transformations.runTransformation(code)
    }
}

class TransformationsBuilder<T : Transformation> {
    val transformations = mutableListOf<T>()

    operator fun T.unaryPlus() {
        transformations.add(this)
    }
}