/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import com.intellij.psi.PsiElement
import kastree.ast.Node
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import java.lang.Exception

data class FailCastException(val node: Node, val to: String) : Exception(
    "Can't cast '$node' to $to"
)

data class BadPsiElementException(val psi: PsiElement) : Exception(
    "Can't convert '${psi::class}' to Node"
)

data class MissingMethodCallName(val psi: GrMethodCall): Exception(
    "'$psi' have null name"
)