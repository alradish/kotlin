package main

fun <T> List.Companion.buildList(size: Int, conf: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    TODO()
}

fun <T> foo(a: T, b: List<T>) {}

fun main() {
    foo(1, [1])
    foo(1, [])
    foo("", [1])
    foo(1.0, [1, "2"])
}