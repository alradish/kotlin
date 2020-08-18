/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.junit.Test
import java.io.File

class TestTest : GradleImportingTestCase() {
    override fun testDataDirectory(): File {
        val baseDir = "g2kts/testData/${testDataDirName()}/"
        return File(baseDir, getTestName(true).substringBefore("_"))
    }

    @Test
    fun testConfigure10() {
//        PsiFileFactory.getInstance()
    }
}

//
//import junit.framework.TestCase
//import kastree.ast.Writer
//import kotlin.test.Test
//import kotlin.test.assertEquals
//
//class SimpleDebugTest {
////    @Test
////    fun extensionAccess() {
////        val build = "ext.test = 2"
////        val code = build.canonicalization()
////        assertEquals(
////            GProject(
////                listOf(
////                    GStatement.GExpr(GBinaryExpression(GExtensionAccess("test"), "=", GConst("2")))
////                )
////            ),
////            buildProject(code)
////        )
////    }
//
//    @Test
//    fun methodCall() {
//        val build = "println 'test'"
//        val code = build.canonicalization()
//
//        TestCase.assertEquals(
//            GProject(
//                listOf(
//                    GStatement.GExpr(
//                        GSimpleMethodCall(
//                            GIdentifier("this"), GIdentifier("println"), GArgumentsList(
//                                listOf(
//                                    GArgument(null, GString("test"))
//                                )
//                            )
//                        )
//
//                    )
//                )
//            ),
//            buildProject(code)
//        )
//    }
//
//    @Test
//    fun taskCreating() {
//        val build = """
//task simpleTask {
//    println 'test'
//}
//        """.trimIndent()
//        val code = build.canonicalization()
//        assertEquals(
//            GProject(
//                listOf(
//                    GStatement.GExpr(
//                        GTaskCreating(
//                            "simpleTask",
//                            "",
//                            GClosure(
//                                emptyList(),
//                                GBlock(
//                                    listOf(
//                                        GStatement.GExpr(
//                                            GSimpleMethodCall(
//                                                GIdentifier("this"), GIdentifier("println"), GArgumentsList(
//                                                    listOf(
//                                                        GArgument(null, GString("test"))
//                                                    )
//                                                )
//                                            )
//                                        )
//                                    )
//                                )
//                            )
//                        )
//                    )
//                )
//            ),
//            buildProject(code)
//        )
//    }
//
//    @Test
//    fun projectPropertySet() {
//        val build = """
//version '1.3'
//group 'test'
//status 3
//println '23'
//        """.trimIndent()
//        val code = build.canonicalization()
//        assertEquals(
//            GProject(
//                listOf(
//                    GStatement.GExpr(GBinaryExpression(GIdentifier("version"), GOperator.byValue("="), GString("1.3"))),
//                    GStatement.GExpr(GBinaryExpression(GIdentifier("group"), GOperator.byValue("="), GString("test"))),
//                    GStatement.GExpr(GBinaryExpression(GIdentifier("status"), GOperator.byValue("="), GConst("3", GConst.Type.INT))),
//                    GStatement.GExpr(
//                        GSimpleMethodCall(
//                            GIdentifier("this"),
//                            GIdentifier("println"),
//                            GArgumentsList(listOf(GArgument(null, GString("23"))))
//                        )
//                    )
//
//                )
//            ),
//            buildProject(code)
//        )
//
//    }
//
//    @Test
//    fun debug() {
//        val build = """
////plugins {
////    id 'java'
////}
////
////task someTask(dependsOn: test) {
////    doLast {
////        println 'someTask'
////    }
////}
////
////test.doLast {
////    println 'test'
////}
////
////group 'test'
//println sourceSets.jvm
////version '1.0-SNAPSHOT'
////
////sourceCompatibility = 1.8
////
////repositories {
////    mavenCentral()
////}
////
////dependencies {
////    testCompile group: 'junit', name: 'junit', version: '4.12'
////}
//        """.trimIndent()
//        val code = build.canonicalization()
//        val gProject = buildProject(code)
//        val kotlin = gProject.toKotlin()
//        println(Writer.write(kotlin))
////        val converted = (G2KtsConverter().apply { debug = true }.convert(code) as Node.Block).stmts
////        println("res:\n${converted.text()}")
////        val transformed = converted.transform()
////        println("transform:\n${transformed.text()}")
//    }
//}