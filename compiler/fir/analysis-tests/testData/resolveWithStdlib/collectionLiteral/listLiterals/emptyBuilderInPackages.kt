// FILE: myList.kt
package myList

class MyList<out T> : Iterable<T> {

    companion object {
        fun <T> buildList(size: Int, conf: ListCollectionLiteralBuilder<MyList<T>, T>.() -> Unit = {}): MyList<T> {
            return TODO()
        }
    }

    override fun iterator(): Iterator<T> {
        return TODO()
    }
}

// FILE: utils.kt
package utils

import myList.MyList

fun MyList.Companion.buildList(size: Int, init: ListCollectionLiteralBuilder<MyList<Int>, Int>.() -> Unit = {}): MyList<Int> {
    return TODO()
}

// FILE: main.kt
package main

import myList.MyList
import myList.MyList.Companion.buildList
import utils.buildList


fun f() {
    val bbb: MyList<Int> = []
}

