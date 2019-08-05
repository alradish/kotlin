/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import junit.framework.TestCase
import kastree.ast.Node
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings

class SimpleDebugTest : TestCase("debug") {


    fun test() {
        val build = """
//plugins {
//    id 'java'
//}
//
//task someTask(dependsOn: test) {
//    doLast {
//        println 'someTask'
//    }
//}

test.doLast {
    println 'test'
}

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
        val converted = (G2KtsConverter().apply { debug = true }.convert(code) as Node.Block).stmts
        println("res:\n${converted.text()}")
        val transformed = converted.transform()
        println("transform:\n${transformed.text()}")
    }
}