/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

sealed class GNode

sealed class GStatement : GNode() {
    data class GExpr(var expr: GExpression) : GStatement()
    data class GDecl(var decl: GDeclaration) : GStatement()
}

data class GBlock(
    var statements: List<GStatement>
) : GStatement()

data class GArgument(
    var name: String?,
    var expr: GExpression
) : GNode()

data class GArgumentsList(
    var args: List<GArgument>
) : GNode()

sealed class GOperator : GNode() {
    data class Common(val token: Token) : GOperator()
    data class Uncommon(val text: String) : GOperator()
    enum class Token(val text: String) {
        MUL("*"), DIV("/"), MOD("%"), ADD("+"), SUB("-"),
        IN("in"), NOT_IN("!in"),
        GT(">"), GTE(">="), LT("<"), LTE("<="),
        EQ("=="), NEQ("!="),
        ASSN("="), MUL_ASSN("*="), DIV_ASSN("/="), MOD_ASSN("%="), ADD_ASSN("+="), SUB_ASSN("-="),
        OR("||"), AND("&&"), ELVIS("?:"), RANGE(".."),
        DOT("."), DOT_SAFE("?."), SAFE("?")
    }

    companion object {
        fun isCommon(text: String): Boolean =
            text in Token.values().map(Token::text)

        fun byValue(text: String): GOperator {
            return Token.values().find { it.text == text }?.let {
                GOperator.Common(it)
            } ?: GOperator.Uncommon(text)
        }
    }
}


// ********** EXPRESSION **********
sealed class GExpression : GNode() {
    fun toStatement(): GStatement = GStatement.GExpr(this)
}

data class GName(
    var name: String
) : GExpression()

sealed class GMethodCall : GExpression() {
    abstract val obj: GExpression
    abstract val method: GExpression
    abstract val arguments: GArgumentsList
}

data class GSimpleMethodCall(
    override val obj: GExpression,
    override val method: GExpression,
    override val arguments: GArgumentsList
) : GMethodCall()

data class GConfigurationBlock(
    override val obj: GExpression,
    override val method: GExpression,
    override val arguments: GArgumentsList,
    val configuration: GClosure
) : GMethodCall()

data class GClosure( // GTODO arguments
    val parameters: List<GExpression>, // GTODO make GParameter
    val statements: GBlock
) : GExpression()

data class GTaskCreating(
    val name: String,
    val type: String, // GTODO make something like GType
    val body: GClosure
) : GExpression()

data class GConst(
    val text: String,
    val type: Type
) : GExpression() {
    enum class Type { BOOLEAN, CHAR, INT, FLOAT, NULL }
}

data class GString(
    val str: String // GTODO template
) : GExpression()

data class GBinaryExpression(
    val left: GExpression,
    val operator: GOperator,
    val right: GExpression
) : GExpression()


sealed class GPropertyAccess : GExpression() {
    abstract val obj: GExpression
    abstract val property: GExpression
}

data class GSimplePropertyAccess(
    override val obj: GExpression,
    override val property: GExpression
) : GPropertyAccess()

data class GExtensionAccess(
    override val obj: GExpression,
    override val property: GExpression
) : GPropertyAccess() {
    companion object {
        val EXT: String = "ext"
    }
}

data class GTaskAccess(
    override val obj: GExpression,
    override val property: GExpression
) : GPropertyAccess() {
    companion object {
        val TASKS: String = "tasks"
    }
}

// ********** EXPRESSION END **********


sealed class GDeclaration : GNode() {
    fun toStatement(): GStatement = GStatement.GDecl(this)
}


data class GProject(
    val statements: List<GStatement>
) : GNode()