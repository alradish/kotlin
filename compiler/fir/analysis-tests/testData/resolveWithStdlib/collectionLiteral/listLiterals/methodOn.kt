package main

fun <T> List.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun main() {
    val i: Int = [1, 2].size
}