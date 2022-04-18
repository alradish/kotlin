import List.Companion.buildList
import Set.Companion.buildList

fun <T> Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun foo(a: Any) {}
fun foo(a: List<Int>) {}
fun <T> bar(a: List<T>) {}
fun <T> baz(a: List<List<T>>) {}


fun f() {
    val a = []
    val b = List<String> []
    val bb = List<Int> []
    val c: Set<Any> = []
    foo([])
    bar([])
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>baz<!>([])
}

