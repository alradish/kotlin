// FILE: builders.kt

package builders

fun <T> List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

class B {
    companion object {
        fun <T> buildList(size: Int, init: ListCollectionLiteralBuilder<B, T>.() -> Unit = {}): B {
            return TODO()
        }
    }
}

// FILE: main.kt

package main

import main.A.Companion.buildList
import main.buildList
import builders.buildList
import builders.B
import builders.B.Companion.buildList

fun <T> Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

class A {
    companion object {
        fun <T> buildList(size: Int, init: ListCollectionLiteralBuilder<A, T>.() -> Unit = {}): A {
            return TODO()
        }
    }
}

fun f() {
    val a: Set<Int> = [1, 2, 3]
    val b: List<Int> = [1, 2, 3]
    val c: A = [1, 2, 3]
    val d: B = [1, 2, 3]
}

