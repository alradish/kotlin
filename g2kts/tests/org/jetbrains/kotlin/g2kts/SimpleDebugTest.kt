/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import com.intellij.testFramework.UsefulTestCase
import kastree.ast.Node
import kastree.ast.Writer
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectScript
import org.jetbrains.groovy.compiler.rt.GroovyCompilerWrapper

class SimpleDebugTest : UsefulTestCase("debugG2KtsVisitor") {
    fun test() {
        val build = """
plugins {
    id 'java'
}

//task someTask(dependsOn: test) {
//    doLast {
//        println 'someTask'
//    }
//}
//
//test.doLast {
//    println 'test'
//}

group 'test'
version '1.0-SNAPSHOT'

sourceCompatibility = 1.8

repositories {
    mavenCentral()
}

dependencies {
    testCompile group: 'junit', name: 'junit', version: '4.12'
}
        """.trimIndent()
        val code = build.canonicalization()
        buildProject(code)
//        val converted = (G2KtsConverter().apply { debug = true }.convert(code) as Node.Block).stmts
//        println("res:\n${converted.text()}")
//        val transformed = converted.transform()
//        println("transform:\n${transformed.text()}")
    }
}