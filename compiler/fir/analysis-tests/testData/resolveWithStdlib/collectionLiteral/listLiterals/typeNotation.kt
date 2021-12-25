package main

class A {
    companion object
}

operator fun <T> A.Companion.get(a: T, b: T, c: T): List<T> {
    return listOf(a, b, c)
}

fun main() {
    val a = A [1, 2, 3]
}
