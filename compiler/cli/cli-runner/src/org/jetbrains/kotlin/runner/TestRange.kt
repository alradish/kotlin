/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner

import java.io.File
import java.net.URL

private val KOTLIN_HOME = File("/home/alrai/projects/kotlin/dist/kotlinc")

private val RANGE_SIMPLE = File("/home/alrai/projects/kotlin/compiler/testData/ir/irText/range/simpleRange.kt")
    .readText()

private val RANGE_CODE_ARRAY = File("/home/alrai/projects/kotlin/compiler/testData/ir/irText/range/simpleArrayRange.kt")
    .readText()

private val RANGE_CODE_LIST = File("/home/alrai/projects/kotlin/compiler/testData/ir/irText/range/simpleListRange.kt")
    .readText()

fun main() {
    val cp = listOf(
        "lib/kotlin-stdlib.jar",
        "lib/kotlin-reflect.jar"
    ).map(::resolveKt)

    val compilerCp = listOf(
        "lib/kotlin-compiler.jar"
    ).map(::resolveKt)


//    val runner = ExpressionRunner(RANGE_SIMPLE)
//    val runner = ExpressionRunner(RANGE_CODE_ARRAY)
    val runner = ExpressionRunner(RANGE_CODE_LIST)
    runner.run(cp, listOf("-include-runtime", "-Xuse-ir"), emptyList(), compilerCp)
}

private fun File.toURL2(): URL {
    return absoluteFile.toURI().toURL()
}

private fun resolveKt(s: String): URL {
    return KOTLIN_HOME.resolve(s).toURL2()
}