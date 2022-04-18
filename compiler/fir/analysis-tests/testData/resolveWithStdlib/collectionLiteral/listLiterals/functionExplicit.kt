
fun <T> Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun <T> Int.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}

fun f(set: Set<Int>) {}
fun b(a: Int) {}
fun z(l: Int.() -> Int) {}
fun main() {
//    z({ 2*this })
    f([1, 2, 3])
//    b([1, 2, 3])
}