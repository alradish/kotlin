/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.gradleAstBuilder

import org.jetbrains.kotlin.g2kts.GBlock
import org.jetbrains.kotlin.g2kts.GBrace
import org.jetbrains.kotlin.g2kts.GStatement

fun GStatement.toGBrace(): GBrace {
    return if (this is GBlock) {
        GBrace(this)
    } else {
        GBrace(GBlock(listOf(this)))
    }
}