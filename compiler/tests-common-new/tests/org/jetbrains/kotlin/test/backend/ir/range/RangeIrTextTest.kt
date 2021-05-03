/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir.range

import org.jetbrains.kotlin.test.runners.ir.AbstractIrTextTest
import org.junit.jupiter.api.Test
import org.jetbrains.kotlin.test.TestMetadata


class RangeIrTextTest : AbstractIrTextTest() {
    @Test
    @TestMetadata("simpleRange.kt")
    fun testSimpleRange() {
        runTest("compiler/testData/ir/irText/range/simpleRange.kt")
    }

    @Test
    @TestMetadata("simpleArrayRange.kt")
    fun testSimpleArrayRange() {
        runTest("compiler/testData/ir/irText/range/simpleArrayRange.kt")
    }
}