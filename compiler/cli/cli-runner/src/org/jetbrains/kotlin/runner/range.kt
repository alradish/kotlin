/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner

fun optimized(from: Int, to: Int): Int {
    var sum = 0
    for (i in from until to) {
        sum += i
    }
    return sum
}

//inline fun <T, R> Iterable<T>.myFold(initial: R, operation: (acc: R, T) -> R): R {
//    var accumulator = initial
//    for (element in this) accumulator = operation(accumulator, element)
//    return accumulator
//}

//fun nonOptimized(from: Int, to: Int): Int = (from..to).myFold(0) { acc, i -> acc + i }
//fun nonOptimized(from: Int, to: Int): Int = (from..to).fold(0) { acc, i -> acc + i }

//fun reversedRange(range: IntRange): Int {
//    var sum = 0
//    for (x in range.reversed()) {
//        sum += x
//
//    }
//    return sum
//}

//fun reversedArray(array: Array<Int>) : Int {
//    var sum = 0
//    for (x in array.reversed()) {
//        sum += x
//    }
//    return sum
//}


//fun forWithUntil(from: Int, to: Int): Int {
//    var sum = 0
//    for (i in from until to) {
//        sum += i
//    }
//    return sum
//}