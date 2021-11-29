fun <K, V> Int.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<Int, K, V>.() -> Unit = {}) : Int {
    return TODO()
}

fun <K, V> Double.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<Double, K, V>.() -> Unit = {}) : Double {
    return TODO()
}


fun main() {
    val a: Int = [1: 1, 2: 2, 3: 3]
    val b: Map<Int, Int> = <!NO_BUILDER_FOR_COLLECTION_LITERAL_OF_TYPE!>[1: 1, 2: 2, 3: 3]<!>
    val c: Double = ["1": 1, "2": 2, "3": 3]
}