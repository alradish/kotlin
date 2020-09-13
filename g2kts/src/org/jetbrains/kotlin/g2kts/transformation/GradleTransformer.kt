/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.g2kts.GNode
import org.jetbrains.kotlin.g2kts.GradleBuildContext
import org.jetbrains.kotlin.g2kts.GradleScopeContext
import org.jetbrains.kotlin.g2kts.transformation.groovy2kts.*

class GradleTransformer(val context: GradleBuildContext) {
    private val scope = GradleScopeContext()

    // FIXME maybe should run transformation if its CHECK number equal
    private val transformationOrder: TransformationsOrder = TransformationsOrder(TransformationsBuilder<Transformation>().apply {
        +JavaSourceCompatibilityTransformation(scope)
        +MoveApplyPluginTransformation(scope)
    }.transformations, scope)

    private val transformations: TransformationsSet = TransformationsSet(TransformationsBuilder<Transformation>().apply {
        +BuildScriptBlockTransformation(scope)
        +ProjectPropertyTransformation(scope)
        +SourceSetsConfigureTransformation(scope)
        +TaskCreationTransformation(scope)
        +TaskAccessWithTypeTransformation(scope)
        +TaskConfigureTransformation(context, scope)
        +TaskAccessTransformation(context, scope)
    }.transformations, scope)


    fun doApply(node: GNode): GNode {
        return transformationOrder.runTransformation(node).let(transformations::runTransformation)
    }

    fun doApply(code: List<GNode>): List<GNode> {
        return transformationOrder.runTransformation(code).let(transformations::runTransformation)
    }
}

class TransformationsBuilder<T : Transformation> {
    val transformations = mutableListOf<T>()

    operator fun T.unaryPlus() {
        transformations.add(this)
    }
}