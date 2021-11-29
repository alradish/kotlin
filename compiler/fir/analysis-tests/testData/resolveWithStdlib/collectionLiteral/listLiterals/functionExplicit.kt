
fun <T> Set.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

fun <T> Int.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}

fun f(set: Set<Int>) {}
fun b(a: Int) {}

fun main() {
    f([1, 2, 3])
    b([1, 2, 3])
}