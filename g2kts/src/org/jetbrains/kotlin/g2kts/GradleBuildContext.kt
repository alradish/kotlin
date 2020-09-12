/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import org.jetbrains.kotlin.gradle.provider.InternalProjectSchemaEntry
import org.jetbrains.kotlin.gradle.provider.InternalSchemaType
import org.jetbrains.kotlin.gradle.provider.InternalTypedProjectSchema

class GradleBuildContext(
    val internalTypedProjectSchema: InternalTypedProjectSchema,
) {
    fun getTaskByName(name: String): InternalProjectSchemaEntry<InternalSchemaType>? {
        return internalTypedProjectSchema.tasks.find { it.name == name }
    }

}

class GradleScopeContext {
    private val _scope: MutableList<GNode> = mutableListOf()
    operator fun get(i: Int): GNode {
        val size = _scope.size
        if (i !in 0 until size)
            throw IndexOutOfBoundsException("$i !in 0..$size")
        return _scope[size - i - 1]
    }

    fun isEmpty(): Boolean = _scope.isEmpty()

    val size: Int
        get() = _scope.size

    fun getCurrentScope(): GNode? = _scope.lastOrNull()
    fun newScope(newScope: GNode) {
        _scope.add(newScope)
    }

    fun leaveScope(): GNode {
        return _scope.removeAt(_scope.size - 1) //removeLast -- unresolved reference
    }

}