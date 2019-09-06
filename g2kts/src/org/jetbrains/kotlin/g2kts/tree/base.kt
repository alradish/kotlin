/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.tree

import com.intellij.psi.PsiElement

interface GradleElement {
    val parrent: GradleElement?

    fun attach(to: GradleElement)

    fun detach(from: GradleElement)
}

interface GradleTreeElement : GradleElement

interface PsiOwner {
    var psi: PsiElement?
}

interface GradleStatement : GradleTreeElement

interface GradleExpression : GradleTreeElement

interface GradleExpressionStatement : GradleStatement {
    var expression: GradleExpression
}

interface GradleOperator {
    val token: GradleOperatorToken
}

interface GradleOperatorToken {
    val text: String
}

interface GradleArgument : GradleTreeElement {
    var expression: GradleExpression
}

interface GradleNamedArgument: GradleArgument {
    val name: String
}

interface GradleArgumentList: GradleTreeElement {
    var arguments: List<GradleArgument>
}

interface GradleFile : GradleTreeElement {
    var statements: List<GradleStatement>
}

interface GradleBlock : GradleStatement {
    var statements: List<GradleStatement>
}

interface GradleExpressionList: GradleExpression {
    val expressions: List<GradleExpression>
}

interface GradleLiteralExpression: GradleExpression {
    val literal: String
    val type: LiteralType

    enum class LiteralType {
        STRING, CHAR, BOOLEAN, NULL, INT, LONG, FLOAT, DOUBLE
    }
}

interface GradleStringLiteralExpression: GradleLiteralExpression {
    var text: String
    // GTODO add template
}

interface GradleLambdaExpression : GradleExpression {
//    var parameters: List<GradleL>
//    val returnType: GradleTypeElement
    var statement: GradleStatement
//    val functionalType: GradleTypeElement
}

interface GradleMethodReferenceExpression: GradleExpression, PsiOwner {
    val qualifier: GradleExpression
    val identifier: String // GTODO bad
}

interface GradleMethodCallExpression: GradleExpression {
    val identifier: GradleMethodReferenceExpression
    val arguments: GradleArgumentList?
    val closure: GradleLambdaExpression?
}

interface GradleConfigurationExpression : GradleExpression, GradleMethodCallExpression {
    override val arguments: GradleArgumentList?
        get() = null
    override val closure: GradleLambdaExpression
}


