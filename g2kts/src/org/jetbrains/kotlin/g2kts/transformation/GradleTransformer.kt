/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.g2kts.GNode
import org.jetbrains.kotlin.g2kts.transformation.groovy2kts.*

object GradleTransformer {
    private fun createTransformationsList(context: GradleBuildContext): TransformationsSet {
        return TransformationsSet(TransformationsBuilder<Transformation>().apply {
//            +JavaSourceCompatibilityTransformation()
            +BuildScriptBlockTransformation()
            +TaskCreationTransformation()
            +ConfigurationBlockTransformation()
            +ProjectPropertyTransformation()
            +TaskConfigureTransformation(context)
            +TaskAccessTransformation(context)
            +NamedDomainObjectCollectionTransformation(context)
        }.transformations)
    }

    fun doApply(code: List<GNode>, context: GradleBuildContext) : List<GNode> {
        val transformations = createTransformationsList(context)
        return transformations.runTransformation(code)
    }
}

class TransformationsBuilder<T : Transformation> {
    val transformations = mutableListOf<T>()

    operator fun T.unaryPlus() {
        transformations.add(this)
    }
}