// FILE: main.kt
package main

import mylist.MyList
import mylist.MyList.Companion.buildMap

fun main() {
    val a: MyList<Int> = [1:1, 2:2, 3:3]

}


// FILE: MyList.kt
package mylist

class MyList<T> {
    private val content = mutableListOf<T>()

    val size: Int
        get() = content.size

    fun add(element: T) {
        content.add(element)
    }

    companion object {
        fun <T> buildMap(size: Int, conf:MapCollectionLiteralBuilder<MyList<T>, T, T>.() -> Unit = {}): MyList<T> {
            return object : MapCollectionLiteralBuilder<MyList<T>, T, T> {
                private val buf = MyList<T>()

                override fun add(element: T, value: T) {
                    buf.add(element)
                    buf.add(value)
                }

                override fun build(): MyList<T> {
                    return buf
                }

            }.apply(conf).build()
        }

    }
}
