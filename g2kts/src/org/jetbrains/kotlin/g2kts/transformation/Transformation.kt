/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.g2kts.GNode

interface Transformation {

    fun runTransformation(node: GNode): GNode

    fun runTransformation(nodes: List<GNode>): List<GNode> {
        return nodes.map { runTransformation(it) }
    }

    fun <R : GNode> applyRecursive(
        element: R,
        func: (GNode) -> GNode
    ): R {
        val iter = element.children.listIterator()
        while (iter.hasNext()) {
            val child = iter.next()

            if (child is List<*>) {
                @Suppress("UNCHECKED_CAST")
                iter.set(applyRecursiveToList(element, child as List<GNode>, iter, func))
            } else if (child is GNode) {
                val newChild = func(child)
                if (child !== newChild) {
                    child.detach(element)
                    iter.set(newChild)
                    newChild.attach(element)
//                onElementChanged(newChild, child)
                }
            } else {
                iter.set(null)
//                error("unsupported child type: ${child?.let { it::class }}")
            }
        }
        return element
    }

    private inline fun applyRecursiveToList(
        element: GNode,
        child: List<GNode>,
        iter: MutableListIterator<Any?>,
        func: (GNode) -> GNode
    ): List<GNode> {

        val newChild = child.map {
            func(it)
        }

        child.forEach { it.detach(element) }
        iter.set(child)
        newChild.forEach { it.attach(element) }
        return newChild
    }

    fun <T : GNode> recurse(element: T): T = applyRecursive(element, this::runTransformation)
}

