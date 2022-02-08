import A.Companion.buildList
import List.Companion.buildList
import Set.Companion.buildList

fun <T> Set.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

class A {
    companion object {
        fun <T> buildList(size: Int, init: ListCollectionLiteralBuilder<A, T>.() -> Unit = {}): A {
            return TODO()
        }
    }
}

fun foo() = [1, 2, 3]

fun bar(): List<String> = [1, 2]

fun <T> bar(): Set<T> {
    return <!NO_BUILDER_FOR_COLLECTION_LITERAL_OF_TYPE!>[1, 2, 3]<!>
}

