/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.tree.impl

import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.g2kts.tree.*
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement

data class GradleProjectImpl(override var statements: List<GradleStatement>) : GradleProject

data class GradleNameIdentifierImpl(override var name: String) : GradleNameIdentifier

data class GradleMethodCallImpl(
    override var obj: GradleElement?,
    override var name: GradleNameIdentifier,
    override var arguments: GradleArgumentList
) : GradleMethodCall

data class GradleBlockImpl(
    override var obj: GradleElement?,
    override var name: GradleNameIdentifier,
    override var arguments: GradleArgumentList,
    override var configuration: GradleLambda
) : GradleBlock

data class GradleLambdaImpl(
    override var delegate: GradleElement?,
    override var statements: List<GradleStatement>

) : GradleLambda

data class GradleArgumentListImpl(
    override var arguments: List<GradleElement>
) : GradleArgumentList

data class GradleConstantImpl(
    override var value: Any?
) : GradleConstant

data class GradleAssignmentImpl(
    override var left: GradleExpression,
    override var right: GradleExpression,
    override var operator: GradleOperator
) : GradleAssignment

data class GradleOperatorImpl(
    override val text: String
) : GradleOperator {
    /*override val text: String
        get() = when(token) {
            JavaTokenType.EQ -> "="
            JavaTokenType.EQEQ -> "=="
            JavaTokenType.NE -> "!="
            JavaTokenType.ANDAND -> "&&"
            JavaTokenType.OROR -> "||"
            JavaTokenType.GT -> ">"
            JavaTokenType.LT -> "<"
            JavaTokenType.GE -> ">="
            JavaTokenType.LE -> "<="
            JavaTokenType.EXCL -> "!"
            JavaTokenType.PLUS -> "+"
            JavaTokenType.MINUS -> "-"
            JavaTokenType.ASTERISK -> "*"
            JavaTokenType.DIV -> "/"
            JavaTokenType.PERC -> "%"
            JavaTokenType.PLUSEQ -> "+="
            JavaTokenType.MINUSEQ -> "-="
            JavaTokenType.ASTERISKEQ -> "*="
            JavaTokenType.DIVEQ -> "/="
            JavaTokenType.PERCEQ -> "%="
            JavaTokenType.GTGT -> "shr"
            JavaTokenType.LTLT -> "shl"
            JavaTokenType.XOR -> "xor"
            JavaTokenType.AND -> "and"
            JavaTokenType.OR -> "or"
            JavaTokenType.GTGTGT -> "ushr"
            JavaTokenType.GTGTEQ -> "shr"
            JavaTokenType.LTLTEQ -> "shl"
            JavaTokenType.XOREQ -> "xor"
            JavaTokenType.ANDEQ -> "and"
            JavaTokenType.OREQ -> "or"
            JavaTokenType.GTGTGTEQ -> "ushr"
            JavaTokenType.PLUSPLUS -> "++"
            JavaTokenType.MINUSMINUS -> "--"
            JavaTokenType.TILDE -> "~"
            else -> TODO(token.toString())
        }*/
}
