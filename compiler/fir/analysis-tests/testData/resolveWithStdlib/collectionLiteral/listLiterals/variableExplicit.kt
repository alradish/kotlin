
fun <T> Int.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}


fun main() {
    val a: Int = [1, 2, 3]
    val b: Set<Int> = <!NO_BUILDER_FOR_COLLECTION_LITERAL_OF_TYPE!>[1, 2, 3]<!>
}