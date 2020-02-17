/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

class GradleBuildContext(
    val tasks: List<Task>
) {
    fun getTaskByName(name: String): Task? = tasks.find { it.name == name }
}

data class Task(
    val name: String,
    val type: String,
    val path: String
)