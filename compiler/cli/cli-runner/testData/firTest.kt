/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner

import List.Companion.buildList
import Set.Companion.buildList

fun <T> Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<Int>, Int>.() -> Unit = {}): List<Int> {
    return TODO()
}


fun main() {
    val a = List<Int> []
}
