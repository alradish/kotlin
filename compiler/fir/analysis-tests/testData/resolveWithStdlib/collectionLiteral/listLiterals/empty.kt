import List.Companion.buildList
import Set.Companion.buildList

fun <T> Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<Int>, Int>.() -> Unit = {}): List<Int> {
    return TODO()
}

//class A {
//    companion object {
//        fun <T> buildList(size: Int, init: ListCollectionLiteralBuilder<A, T>.() -> Unit = {}): A {
//            return TODO()
//        }
//    }
//}

fun foo(a: Any) {}
fun foo(a: List<Int>) {}
fun <T> bar(a: List<T>) {}
fun <T> baz(a: List<List<T>>) {}


fun f() {
    val a = []
    val b = List<String> [<!SYNTAX!><!>]
//    val bb = List<Int> [<!SYNTAX!><!>]
//    val c: Set<Any> = []
//    foo([])
//    bar([])
//    baz([])
}

