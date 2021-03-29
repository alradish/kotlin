/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.codegen.AbstractBytecodeTextTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.DoTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestMetadata
import java.io.File
import java.lang.Exception
import java.util.regex.Pattern

class TestBytecodeTextTestCase : AbstractBytecodeTextTest() {
    private fun runTest(testDataFilePath: String) {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.JVM, testDataFilePath);
    }

    @TestMetadata("noBoxingUnboxingInAccessorsForDelegatedPropertyWithInlineClassDelegate.kt")
    fun testNoBoxingUnboxingInAccessorsForDelegatedPropertyWithInlineClassDelegate() {
        runTest("compiler/testData/codegen/bytecodeText/inlineClasses/noBoxingUnboxingInAccessorsForDelegatedPropertyWithInlineClassDelegate.kt")
    }
}