fun <T> Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun <T> f(a: Iterable<T>) {}
//fun <T> f(a: Collection<T>) {}
fun <T> f(a: Set<T>) {}

fun main() {
    f([1, 2, 3])
}