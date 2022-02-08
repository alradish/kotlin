package main

fun <T> List.Companion.buildList(size: Int, conf: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    TODO()
}

fun main() {
//    val a: List<Int> = [1, 2]
//    val b = a + [3, 4]
//    val c = [3, 4] + a
//    val d: List<Short> = if (true) [1, 2] else [2, 3]
    val d: List<Short> = if (true) listOf(1, 2) else listOf(2, 3)
}