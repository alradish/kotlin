package main

fun <T> List.Companion.buildList(size: Int, conf: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    TODO()
}

fun List.Companion.buildList(size: Int, conf: ListCollectionLiteralBuilder<List<Int>, Int>.() -> Unit = {}): List<Int> {
    TODO()
}

fun foo(a: List<Int>) {}
fun <T> foo(a: List<T>) {}

fun main() {
    foo([1])
}