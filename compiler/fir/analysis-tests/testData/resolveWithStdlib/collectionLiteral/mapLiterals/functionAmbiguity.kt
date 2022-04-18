

fun <K, V> Int.Companion.buildMap(size: Int, init: MapCollectionLiteralBuilder<Int, K, V>.() -> Unit = {}) : Int {
    return TODO()
}

fun <K, V> Double.Companion.buildMap(size: Int, init: MapCollectionLiteralBuilder<Double, K, V>.() -> Unit = {}) : Double {
    return TODO()
}

fun fOver(a: Int) {}
fun fOver(a: Double) {}

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>fOver<!>(["1": 1, "2": 2, "3": 3])
}
