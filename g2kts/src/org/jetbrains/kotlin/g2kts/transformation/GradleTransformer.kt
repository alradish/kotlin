/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.g2kts.GNode

object GradleTransformer {
    private fun createTransformationsList(): TransformationsSet {
        return TransformationsSet(TransformationsBuilder<Transformation>().apply {
            +object : Transformation {
                override fun runTransformation(node: List<GNode>) {
                    println("heh")
                }
            }
        }.transformations)
    }

    fun doApply(code: List<GNode>) {
        val transformations = createTransformationsList()
        transformations.runTransformation(code)
    }
}

class TransformationsBuilder<T : Transformation> {
    val transformations = mutableListOf<T>()

    operator fun T.unaryPlus() {
        transformations.add(this)
    }
}