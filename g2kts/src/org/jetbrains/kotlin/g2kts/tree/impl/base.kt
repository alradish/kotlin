/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.tree.impl

import org.jetbrains.kotlin.g2kts.tree.*
import kotlin.properties.Delegates

abstract class GradleElementBase : GradleTreeElement {

    protected fun <T : GradleTreeElement, U : T> child(v: U) = Delegates.observable(v) { _, old, new ->
        old.detach(this)
        new.attach(this)
    }

    protected fun <T : GradleTreeElement> children(v: List<T>) = Delegates.observable(v) { _, old, new ->
        old.forEach { it.detach(this) }
        new.forEach { it.attach(this) }
    }

    override var parent: GradleElement? = null

    override fun detach(from: GradleElement) {
        val prevParent = parent
        require(from == prevParent)
        parent = null
    }

    override fun attach(to: GradleElement) {
        check(parent == null)
        parent = to
    }
}

class GradleFileImpl(statements: List<GradleStatement>) : GradleFile, GradleElementBase() {
    override var statements: List<GradleStatement> by children(statements)
}

class GradleLambdaExpressionImpl(
    block: GradleBlock
) : GradleLambdaExpression, GradleElementBase() {
    override var block: GradleBlock by child(block)
}

class GradleBlockImpl(
    statements: List<GradleStatement>
) : GradleBlock, GradleElementBase() {
    override var statements: List<GradleStatement> by children(statements)

}