/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

class KtCollectionLiteralEntrySingle(node: ASTNode) : KtCollectionLiteralEntry(node) {
    val expression: KtExpression
        get() = PsiTreeUtil.findChildOfType(this, KtExpression::class.java)
            ?: throw NullPointerException("No expression was found for single collection literal entry: " + Arrays.toString(children))
}