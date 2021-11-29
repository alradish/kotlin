

fun <T> Int.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}

fun fOver(a: Int) {}
fun fOver(a: Double) {}

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>fOver<!>([1, 2, 3])
}