
fun <T> Int.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}

fun <T> fVariable(a: T) {}

fun main() {
    fVariable(<!CANT_CHOOSE_BUILDER!>[1, 2, 3]<!>)
}