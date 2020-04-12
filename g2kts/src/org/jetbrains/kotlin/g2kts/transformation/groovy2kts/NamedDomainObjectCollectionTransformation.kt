/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation.groovy2kts

import org.jetbrains.kotlin.g2kts.GExtensionAccess
import org.jetbrains.kotlin.g2kts.GIdentifier
import org.jetbrains.kotlin.g2kts.GNode
import org.jetbrains.kotlin.g2kts.GPropertyAccess
import org.jetbrains.kotlin.g2kts.transformation.GradleBuildContext
import org.jetbrains.kotlin.g2kts.transformation.Transformation

class NamedDomainObjectCollectionTransformation(override val context: GradleBuildContext) : Transformation() {
    //    val NAMED_DOMAIN_OBJECT_COLLECTION = org.gradle.api.NamedDomainObjectCollection::class
    override fun runTransformation(node: GNode): GNode {
        if (node !is GPropertyAccess) return recurse(node)
        val obj = node.obj as? GIdentifier ?: return recurse(node)
        if (context.containerElements.find { it.name == obj.name } != null) {
            return recurse(
                GExtensionAccess(
                    obj,
                    node.property
                )
            )
        }
        return recurse(node)
    }
}
