/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.g2kts.GNode

class TransformationsSet(private val transformations: List<Transformation>) : Transformation() {
    override fun runTransformation(node: GNode): GNode {
        return transformations.fold(node) { acc: GNode, transformation: Transformation ->
            transformation.runTransformation(acc)
        }
    }
}