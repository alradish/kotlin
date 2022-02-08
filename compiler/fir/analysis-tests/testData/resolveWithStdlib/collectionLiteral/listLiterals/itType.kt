package main

fun <T> List.Companion.buildList(size: Int, conf: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    TODO()
}

fun List.Companion.buildList(size: Int, conf: ListCollectionLiteralBuilder<List<Int>, Int>.() -> Unit = {}): List<Int> {
    TODO()
}

fun f(a: List<Number>) {}

fun main() {
    f([1, 1.0])
}