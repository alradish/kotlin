// FILE: MyList.kt
package shadowed

class MyList<T> {
    companion object {
        fun <T> buildList(size: Int, conf: ListCollectionLiteralBuilder<MyList<T>, T>.() -> Unit = {}): MyList<T> {
            return TODO()
        }
    }
}

// FILE: utils.kt
package shadowed

fun MyList.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<MyList<Int>, Int>.() -> Unit = {}): MyList<Int> {
    return TODO()
}


// FILE: main.kt
package shadowed

import shadowed.MyList
import shadowed.MyList.Companion.buildList
import shadowed.buildList

fun f() {
    val b = MyList<Int> [1, 2, 3]
}

