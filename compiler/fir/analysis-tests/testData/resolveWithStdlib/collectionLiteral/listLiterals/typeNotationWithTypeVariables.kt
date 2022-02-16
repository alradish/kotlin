package main

fun <T> Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<Int>, Int>.() -> Unit = {}): Set<Int> {
    return TODO()
}

operator fun <T> List.Companion.get(a: T, b: T, c: T): List<T> {
    return listOf(a, b, c)
}

fun <T> List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun main() {
//    val a = List<Int> [1, 2, 3]
//    val aa = List<Int> [1, 2, 3, 4]
    val b = <!NO_BUILDER_FOR_COLLECTION_LITERAL_OF_TYPE!>List<String><!> [1, 2, 3]
//    val bb = <!NO_BUILDER_FOR_COLLECTION_LITERAL_OF_TYPE!>List<String><!> [1, 2, 3, 4]
//    val c = List<Short> []
//
//    val d = Set<String> ["1"]
//    val e = Set<Int> [1]
//    val f = Set<Int> []
//    val g = Set<String> []
}
