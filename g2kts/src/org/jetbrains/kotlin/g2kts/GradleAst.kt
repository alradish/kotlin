/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import com.intellij.psi.PsiElement
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

sealed class GNode(open val psi: PsiElement? = null) : Cloneable {
    var parent: GNode? = null

    private var childNum = 0
    var children: MutableList<Any?> = mutableListOf()

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

    protected fun <T : GNode, U : T> child(v: U): ReadWriteProperty<GNode, U> {
        children.add(childNum, v)
        v.attach(this)
        return GChild(childNum++)
    }

    protected fun <T : GNode, U : T> childNullable(v: U?): ReadWriteProperty<GNode, U?> {
        children.add(childNum, v)
        v?.attach(this)
        return GNullableChild(childNum++)
    }

    protected fun <T : GNode> children(v: List<T>): ReadWriteProperty<GNode, List<T>> {
        children.add(childNum, v)
        v.forEach { it.attach(this) }
        return GListChild(childNum++)
    }

    protected inline fun <reified T : GNode> children(): ReadWriteProperty<GNode, List<T>> {
        return children(emptyList())
    }

    @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
    fun copy(): GNode {
        val cloned = clone() as GNode
        val deepClonedChildren =
            cloned.children.map {
                when (it) {
                    is GNode -> it.copy()
                    is List<*> -> (it as List<GNode>).map { it.copy() }
                    null -> null
                    else -> error("Tree is corrupted")
                }
            }

        deepClonedChildren.forEach { child ->
            when (child) {
                is GNode -> {
                    child.detach(this)
                    child.attach(cloned)
                }
                is List<*> -> (child as List<GNode>).forEach {
                    it.detach(this)
                    it.attach(cloned)
                }
            }
        }
        cloned.children = deepClonedChildren.toMutableList()
        return cloned
    }
}

fun <T : GNode> GNode.topParent(type: KClass<T>): GNode? {
    if (parent == null) return null
    if (parent!!::class == type) return parent
    else return parent!!.topParent(type)
}
//
//private fun <T : GNode> KProperty0<Any>.detach(element: T) {
//    if (element.parent == null) return
//    val boundReceiver = (this as CallableReference).boundReceiver
//    require(boundReceiver != CallableReference.NO_RECEIVER)
//    require(boundReceiver is GNode)
//    element.detach(boundReceiver)
//}
//
//fun <T : GNode> KProperty0<T>.detached(): T =
//    get().also { detach(it) }
//
//fun <T : GNode> KProperty0<List<T>>.detached(): List<T> =
//    get().also { list -> list.forEach { detach(it) } }

fun <T : GNode> T.detached(): T =
    also { it.detach(it.parent) }

private class GChild<T : GNode>(val index: Int) : ReadWriteProperty<GNode, T> {
    override fun getValue(thisRef: GNode, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return thisRef.children[index] as T
    }

    override fun setValue(thisRef: GNode, property: KProperty<*>, value: T) {
        @Suppress("UNCHECKED_CAST")
        (thisRef.children[this.index] as T).detach(thisRef)
        thisRef.children[this.index] = value
        value.attach(thisRef)
    }
}

private class GNullableChild<T : GNode>(val index: Int) : ReadWriteProperty<GNode, T?> {
    override fun getValue(thisRef: GNode, property: KProperty<*>): T? {
        @Suppress("UNCHECKED_CAST")
        return thisRef.children[index] as T?
    }

    override fun setValue(thisRef: GNode, property: KProperty<*>, value: T?) {
        @Suppress("UNCHECKED_CAST")
        (thisRef.children[this.index] as T).detach(thisRef)
        thisRef.children[this.index] = value
        value?.attach(thisRef)
    }
}

private class GListChild<T : GNode>(val value: Int) : ReadWriteProperty<GNode, List<T>> {
    override operator fun getValue(thisRef: GNode, property: KProperty<*>): List<T> {
        @Suppress("UNCHECKED_CAST")
        return thisRef.children[value] as List<T>
    }

    override operator fun setValue(thisRef: GNode, property: KProperty<*>, value: List<T>) {
        @Suppress("UNCHECKED_CAST")
        (thisRef.children[this.value] as List<T>).forEach { it.detach(thisRef) }
        thisRef.children[this.value] = value
        value.forEach { it.attach(thisRef) }
    }
}

sealed class GStatement(psi: PsiElement? = null) : GNode(psi) {
    class GExpr(expr: GExpression, psi: PsiElement? = null) : GStatement(psi) {
        var expr: GExpression by child(expr)
    }

    class GDecl(decl: GDeclaration, psi: PsiElement? = null) : GStatement(psi) {
        var decl: GDeclaration by child(decl)
    }
}

class GTryCatch(
    var body: GBlock,
    var catches: List<Catch>,
    var finallyBody: GBlock?,
    psi: PsiElement? = null
) : GExpression(psi) {
    // FIXME make catch : GNode
    data class Catch(
//        var anns: List<Node.Modifier.AnnotationSet>,
        var name: String,
        var type: String,
        var block: GBlock
    )
}

class GSwitch(
    var expr: GExpression,
    var cases: List<GSwitchCase>,
    var default: GSwitchCase?,
    psi: PsiElement? = null
): GExpression(psi)

class GSwitchCase(
    var expr: GExpression,
    var body: GBrace,
    psi: PsiElement? = null
) : GNode(psi)

class GWhile(
    var condition: GExpression,
    var body: GExpression,
    psi: PsiElement?
) : GExpression(psi)

/*
data class For(
            override val anns: List<Modifier.AnnotationSet>,
            // More than one means destructure, null means underscore
            val vars: List<Decl.Property.Var?>,
            val inExpr: Expr,
            val body: Expr
        ) : Expr(), WithAnnotations
 */


class GIf(
    var condition: GExpression,
    var body: GExpression,
    var elseBody: GExpression?,
    psi: PsiElement? = null
) : GExpression(psi)

class GBlock(
    statements: List<GStatement>, psi: PsiElement? = null
) : GStatement(psi) {
    var statements: List<GStatement> by children(statements)
}

class GArgument(
    var name: String?,
    expr: GExpression,
    psi: PsiElement? = null
) : GNode(psi) {
    var expr: GExpression by child(expr)
}

class GArgumentsList(
    args: List<GArgument>,
    psi: PsiElement? = null
) : GNode(psi) {
    var args: List<GArgument> by children(args)
}


// ********** EXPRESSION **********
sealed class GExpression(psi: PsiElement? = null) : GNode(psi) {
    fun toStatement(): GStatement = GStatement.GExpr(this)
}

class GBrace(
    // var param: GParameter,
    var block: GBlock?,
    psi: PsiElement? = null
) : GExpression(psi)

class GIdentifier(
    var name: String,
    psi: PsiElement? = null
) : GExpression(psi)

class GList(
    initializers: List<GExpression>,
    psi: PsiElement? = null
) : GExpression(psi) {
    var initializers: List<GExpression> by children(initializers)
}

sealed class GMethodCall(
    obj: GExpression?,
    method: GExpression,
    arguments: GArgumentsList,
    closure: GClosure?,
    psi: PsiElement? = null
) : GExpression(psi) {
    var obj: GExpression? by childNullable(obj)
    var method: GExpression by child(method)
    var arguments: GArgumentsList by child(arguments)
    var closure: GClosure? by childNullable(closure)
}

class GSimpleMethodCall(
    obj: GExpression?,
    method: GExpression,
    arguments: GArgumentsList,
    closure: GClosure?,
    psi: PsiElement? = null
) : GMethodCall(obj, method, arguments, closure, psi)

class GConfigurationBlock(
    obj: GExpression?,
    method: GExpression,
    arguments: GArgumentsList,
    closure: GClosure,
    psi: PsiElement? = null
) : GMethodCall(obj, method, arguments, closure, psi) {}

class GClosure( // GTODO arguments
    parameters: List<GExpression>,
    statements: GBlock,
    psi: PsiElement? = null
) : GExpression(psi) {
    var parameters: List<GExpression> by children(parameters) // GTODO make GParameter
    var statements: GBlock by child(statements)

}

class GTaskCreating(
    var name: String,
    var type: String, // GTODO make something like GType
    body: GClosure?,
    psi: PsiElement? = null
) : GExpression(psi) {
    var body: GClosure? by childNullable(body)

}

class GConst(
    var text: String,
    var type: Type,
    psi: PsiElement? = null
) : GExpression(psi) {
    enum class Type { BOOLEAN, CHAR, INT, FLOAT, NULL }
}

class GString(
    var str: String, // GTODO template
    psi: PsiElement? = null
) : GExpression(psi)

class GUnaryExpression(
    var expr: GExpression,
    val operator: GUnaryOperator,
    var prefix: Boolean,
    psi: PsiElement? = null
) : GExpression(psi)

data class GUnaryOperator(var token: Token) : GNode() {
    enum class Token(val text: String) {
        NEG("-"), POS("+"), INC("++"), DEC("--"), NOT("!"), NULL_DEREF("!!")
    }

    companion object {
        fun byValue(text: String): GUnaryOperator? {
            return GUnaryOperator.Token.values().find { it.text == text }?.let { GUnaryOperator(it) }
        }
    }
}


class GBinaryExpression(
    left: GExpression,
    operator: GBinaryOperator,
    right: GExpression,
    psi: PsiElement? = null
) : GExpression(psi) {
    var left: GExpression by child(left)
    var operator: GBinaryOperator by child(operator)
    var right: GExpression by child(right)
}

sealed class GBinaryOperator : GNode() {
    data class Common(val token: Token) : GBinaryOperator()
    data class Uncommon(val text: String) : GBinaryOperator()
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

        fun byValue(text: String): GBinaryOperator {
            return Token.values().find { it.text == text }?.let {
                Common(it)
            } ?: Uncommon(text)
        }
    }
}

sealed class GPropertyAccess(
    obj: GExpression?,
    property: GExpression,
    psi: PsiElement? = null
) : GExpression(psi) {
    var obj: GExpression? by childNullable(obj)
    var property: GExpression by child(property)
}

class GSimplePropertyAccess(
    obj: GExpression?,
    property: GExpression,
    psi: PsiElement? = null
) : GPropertyAccess(obj, property, psi)

class GExtensionAccess(
    obj: GExpression,
    property: GExpression,
    psi: PsiElement? = null
) : GPropertyAccess(obj, property, psi) {
    companion object {
        val EXT: String = "ext"
    }
}

sealed class GTaskAccess(psi: PsiElement? = null) : GExpression(psi) {
    abstract var task: String
    abstract var type: String?
}

class GSimpleTaskAccess(
    override var task: String,
    override var type: String?,
    psi: PsiElement? = null
) : GTaskAccess(psi)

class GTaskConfigure(
    override var task: String,
    override var type: String?,
    configure: GClosure,
    psi: PsiElement? = null
) : GTaskAccess(psi) {
    var configure: GClosure by child(configure)
}

//data class GMemberAccess(
//    var obj: GExpression,
//    var member: GExpression
//)

// ********** EXPRESSION END **********


sealed class GDeclaration(psi: PsiElement? = null) : GNode(psi) {
    fun toStatement(): GStatement = GStatement.GDecl(this)
}

class GVariableDeclaration(
    var type: String?,
    var name: GIdentifier,
    var expr: GExpression?,
    psi: PsiElement? = null
) : GDeclaration(psi)

class GProject(
    statements: List<GStatement>,
    psi: PsiElement? = null
) : GNode(psi) {
    var statements: List<GStatement> by children(statements)
}

class GBuildScriptBlock(
    var type: BuildScriptBlockType,
    block: GClosure,
    psi: PsiElement? = null
) : GExpression(psi) {
    var block: GClosure by child(block)

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