/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.codegen.ir.AbstractIrBytecodeTextTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestMetadata

class TestIrTextTestCase : AbstractIrBytecodeTextTest() {
    private fun runTest(testDataFilePath: String) {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.JVM_IR, testDataFilePath);
    }

    @TestMetadata("notUseProperty.kt")
    fun testNotUseProperty() {
        runTest("compiler/testData/ir/irText/delegatedOperators/notUseProperty.kt")
    }

    @TestMetadata("usePropertyName.kt")
    fun testUsePropertyName() {
        runTest("compiler/testData/ir/irText/delegatedOperators/usePropertyName.kt")
    }

    @TestMetadata("useProperty.kt")
    fun testUseProperty() {
        runTest("compiler/testData/ir/irText/delegatedOperators/useProperty.kt")
    }

    @TestMetadata("usePropertyTwice.kt")
    fun testUsePropertyTwice() {
        runTest("compiler/testData/ir/irText/delegatedOperators/usePropertyTwice.kt")
    }

    @TestMetadata("delegatedInDerived.kt")
    fun testDelegatedInDerived() {
        runTest("compiler/testData/ir/irText/delegatedOperators/delegatedInDerived.kt")
    }

    @TestMetadata("delegateToAnother.kt")
    fun testDelegateToAnother() {
        runTest("compiler/testData/ir/irText/delegatedOperators/delegateToAnother.kt")
    }

    @TestMetadata("simpleDelegateFun.kt")
    fun testSimpleDelegateFun() {
        runTest("compiler/testData/ir/irText/delegatedOperators/simpleDelegateFun.kt")
    }

    @TestMetadata("delegateFun.kt")
    fun testDelegateFun() {
        runTest("compiler/testData/ir/irText/delegatedOperators/delegateFun.kt")
    }

    @TestMetadata("delegateTopLevelVarToInlineClass.kt")
    fun testDelegateTopLevelToInlineClass() {
        runTest("compiler/testData/ir/irText/delegatedOperators/delegateTopLevelVarToInlineClass.kt")
    }

    @TestMetadata("extensionDelegated.kt")
    fun testExtensionDelegated(){
        runTest("compiler/testData/ir/irText/delegatedOperators/extensionDelegated.kt")
    }

    // FIXME
    @TestMetadata("genericSetValueViaSyntheticAccessor.kt")
    fun testGenericSetValueViaSyntheticAccessor() {
        runTest("compiler/testData/ir/irText/delegatedOperators/genericSetValueViaSyntheticAccessor.kt")
    }

    @TestMetadata("annotations.kt")
    fun testAnnotations() {
        runTest("compiler/testData/ir/irText/delegatedOperators/annotations.kt")
    }

    // FIXME DELETE THIS
    fun testSpread() {
        runTest("compiler/testData/ir/irOptimization/spreadVararg.kt")
    }

}