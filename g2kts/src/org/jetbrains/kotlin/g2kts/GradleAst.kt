/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass

sealed class GNode {
    var parent: GNode? = null

    fun detach(from: GNode?) {
        from ?: return
        val prevParent = parent
        require(from == prevParent)
        parent = null
    }

    fun attach(to: GNode) {
        check(parent == null)
        parent = to
    }

    protected fun <T : GNode?, U : T> child(v: U): ReadWriteProperty<GNode?, U> {
        v?.detach(v.parent)
        v?.attach(this)
        return Delegates.observable(v) { _, old, new ->
            old?.detach(this)
            new?.attach(this)
        }
    }

    protected fun <T : GNode> children(v: List<T>): ReadWriteProperty<GNode, List<T>> {
        v.forEach { it.detach(it.parent) }
        v.forEach { it.attach(this) }
        return Delegates.observable(v) { _, old, new ->
            old.forEach { it.detach(this) }
            new.forEach { it.attach(this) }
        }
    }
}


sealed class GStatement : GNode() {
    class GExpr(expr: GExpression) : GStatement() {
        var expr: GExpression by child(expr)
    }
    class GDecl(decl: GDeclaration) : GStatement() {
        var decl: GDeclaration by child(decl)
    }
}

class GBlock(
    statements: List<GStatement>
) : GStatement() {
    var statements: List<GStatement> by children(statements)
}

class GArgument(
    var name: String?,
    expr: GExpression
) : GNode() {
    var expr: GExpression by child(expr)
}

class GArgumentsList(
    args: List<GArgument>
) : GNode() {
    var args: List<GArgument> by children(args)
}

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

data class GIdentifier(
    var name: String
) : GExpression()

class GList(
    initializers: List<GExpression>
) : GExpression() {
    var initializers: List<GExpression> by children(initializers)
}

sealed class GMethodCall(
    obj: GExpression?,
    method: GExpression,
    arguments: GArgumentsList
) : GExpression() {
    val obj: GExpression? by child(obj)
    val method: GExpression by child(method)
    val arguments: GArgumentsList by child(arguments)
}

class GSimpleMethodCall(
    obj: GExpression?,
    method: GExpression,
    arguments: GArgumentsList
) : GMethodCall(obj, method, arguments)

class GConfigurationBlock(
    obj: GExpression?,
    method: GExpression,
    arguments: GArgumentsList,
    configuration: GClosure
) : GMethodCall(obj, method, arguments) {
    val configuration: GClosure by child(configuration)
}

class GClosure( // GTODO arguments
    parameters: List<GExpression>,
    statements: GBlock
) : GExpression() {
    val parameters: List<GExpression> by children(parameters) // GTODO make GParameter
    val statements: GBlock by child(statements)

}

class GTaskCreating(
    val name: String,
    val type: String, // GTODO make something like GType
    body: GClosure
) : GExpression() {
    val body: GClosure by child(body)

}

data class GConst(
    val text: String,
    val type: Type
) : GExpression() {
    enum class Type { BOOLEAN, CHAR, INT, FLOAT, NULL }
}

data class GString(
    val str: String // GTODO template
) : GExpression()

class GBinaryExpression(
    left: GExpression,
    operator: GOperator,
    right: GExpression
) : GExpression() {
    val left: GExpression by child(left)
    val operator: GOperator by child(operator)
    val right: GExpression by child(right)
}


sealed class GPropertyAccess(
    obj: GExpression?,
    property: GExpression
) : GExpression() {
    val obj: GExpression? by child(obj)
    val property: GExpression by child(property)
}

class GSimplePropertyAccess(
    obj: GExpression?,
    property: GExpression
) : GPropertyAccess(obj, property)

class GExtensionAccess(
    obj: GExpression,
    property: GExpression
) : GPropertyAccess(obj, property) {
    companion object {
        val EXT: String = "ext"
    }
}

sealed class GTaskAccess : GExpression() {
    abstract val task: String
    abstract val type: KClass<*>
}

data class GSimpleTaskAccess(
    override val task: String,
    override val type: KClass<*>
) : GTaskAccess()

class GTaskConfigure(
    override val task: String,
    override val type: KClass<*>,
    configure: GClosure
) : GTaskAccess() {
    val configure: GClosure by child(configure)
}

//data class GMemberAccess(
//    val obj: GExpression,
//    val member: GExpression
//)

// ********** EXPRESSION END **********


sealed class GDeclaration : GNode() {
    fun toStatement(): GStatement = GStatement.GDecl(this)
}


class GProject(
    statements: List<GStatement>
) : GNode() {
    val statements: List<GStatement> by children(statements)
}

class GBuildScriptBlock(
    val type: BuildScriptBlockType,
    block: GClosure
) : GExpression() {
    val block: GClosure by child(block)

    enum class BuildScriptBlockType(val text: String) {
        ALL_PROJECTS("allprojects"),
        ARTIFACTS("artifacts"),
        BUILD_SCRIPT("buildscript"),
        CONFIGURATIONS("configurations"),
        DEPENDENCIES("dependencies"),
        REPOSITORIES("repositories"),
        SOURCE_SETS("sourceSets"),
        SUBPROJECTS("subprojects"),
        PUBLISHING("publishing");

        companion object {
            fun byName(name: String) = values().find { it.text == name }
        }
    }
}