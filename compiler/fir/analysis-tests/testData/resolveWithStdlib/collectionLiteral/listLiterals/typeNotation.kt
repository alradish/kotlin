package main

class A {
    companion object
}

operator fun <T> A.Companion.get(a: T, b: T, c: T): List<T> {
    return listOf(a, b, c)
}

class B {
    companion object
}

fun <T> B.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<B, T>.() -> Unit = {}): B {
    return TODO()
}


class C {
    companion object
}
operator fun <T> C.Companion.get(a: T, b: T, c: T): List<T> {
    return listOf(a, b, c)
}

fun <T> C.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<C, T>.() -> Unit = {}): C {
    return TODO()
}

fun main() {
    val a = A [1, 2, 3]
    val b = mutableListOf<Int>()
    val c = b[0]
    b[0] = 1
    val d = B [1, 2, 3]
    val e = C [1, 2, 3]
    val f = C [1, 2, 3, 4]
}
