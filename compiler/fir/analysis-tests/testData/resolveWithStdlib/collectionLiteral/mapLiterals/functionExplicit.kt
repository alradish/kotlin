
fun <K, V> Set.Companion.buildMap(size: Int, init: MapCollectionLiteralBuilder<Set<V>, K, V>.() -> Unit = {}): Set<V> {
    return TODO()
}

fun <K, V> List.Companion.buildMap(size: Int, init: MapCollectionLiteralBuilder<List<V>, K, V>.() -> Unit = {}): List<V> {
    return TODO()
}

fun <K, V> Int.Companion.buildMap(size: Int, init: MapCollectionLiteralBuilder<Int, K, V>.() -> Unit = {}) : Int {
    return TODO()
}

fun <K, V> Double.Companion.buildMap(size: Int, init: MapCollectionLiteralBuilder<Double, K, V>.() -> Unit = {}) : Double {
    return TODO()
}

fun f(set: Set<Int>) {}
fun b(a: Int) {}

fun main() {
    f(["1": 1, "2": 2, "3": 3])
    b(["1": 1, "2": 2, "3": 3])
}