/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.g2kts.GNode
import org.jetbrains.kotlin.g2kts.GradleBuildContext
import org.jetbrains.kotlin.g2kts.GradleScopeContext

abstract class Transformation(
    val scopeContext: GradleScopeContext,
    open val context: GradleBuildContext? = null
) {

    abstract fun runTransformation(node: GNode): GNode

    fun check(node: GNode): Int {
        if (scopeContext.isEmpty()) return -1 // TODO i don't sure about this
        val size = scopeContext.size
        for (i in 0 until size) {
            if (can(node, scopeContext[i])) return i
        }
        return -1
    }


    // FIXME rename
    abstract fun can(node: GNode, scope: GNode?): Boolean

    open fun runTransformation(nodes: List<GNode>): List<GNode> {
        return nodes.map { runTransformation(it) }
    }

    open fun <T : GNode> recurse(element: T): T {
        // TODO add new scope and leave old
        return applyRecursive(element, this::runTransformation)
    }
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