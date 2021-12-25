/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner

class A {
    companion object
}

//fun <T> List.Companion.buildList(size: Int, conf: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
//    TODO()
//}

operator fun <T> A.Companion.get(a: T): List<T> {
    return listOf(a)
}

operator fun <T> A.Companion.get(a: T, b: T, c: T): List<T> {
    return listOf(a, b, c)
}


fun main() {
    val a = A [1]
}
