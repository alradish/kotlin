/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import kastree.ast.Node
import kastree.ast.Writer

val mapOf = Node.Expr.Name("mapOf")
val listOf = Node.Expr.Name("listOf")
val to = Node.Expr.BinaryOp.Oper.Infix("to")

val Node.Expr.Call.name: String?
    get() = (expr as? Node.Expr.Name)?.name

fun <T : Node> List<T>.text() = joinToString(separator = "\n") { Writer.write(it) }


fun Node.Expr.StringTmpl.text(): String {
    return elems.joinToString() {
        when (it) {
            is Node.Expr.StringTmpl.Elem.Regular -> it.str
            is Node.Expr.StringTmpl.Elem.ShortTmpl -> it.str
            is Node.Expr.StringTmpl.Elem.UnicodeEsc -> it.digits
            is Node.Expr.StringTmpl.Elem.RegularEsc -> it.char.toString()
            is Node.Expr.StringTmpl.Elem.LongTmpl -> TODO()
        }
    }
}

fun Node.Expr.Call.convertMapToNamed(): Node.Expr.Call {
    if (!(args.size == 1 && args.first().expr is Node.Expr.Call && (args.first().expr as Node.Expr.Call).name == "mapOf")) return this
    val map = args.first().expr as Node.Expr.Call
    val newArgs = map.args.map { arg ->
        val (l, _, r) = (arg.expr as Node.Expr.BinaryOp)
        val name = if (l is Node.Expr.StringTmpl) {
            l.text()
        } else return this
        Node.ValueArg(name, false, r)
    }
    return call(expr, typeArgs, newArgs, lambda)
}

val Node.Expr.isPair: Boolean
    get() = this is Node.Expr.BinaryOp && this.oper == to

infix fun Node.Expr.dot(m: Node.Expr) = Node.Expr.BinaryOp(
    this,
    Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.DOT),
    m
)

infix fun Node.Expr.Name.assn(expr: Node.Expr) = Node.Expr.BinaryOp(
    this,
    Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.ASSN),
    expr
)

infix fun Node.Expr.to(expr: Node.Expr) = Node.Expr.BinaryOp(
    this,
    to,
    expr
)

fun name(name: String = "") = Node.Expr.Name(name)

fun call(
    expr: Node.Expr,
    typeArgs: List<Node.Type?> = emptyList(),
    args: List<Node.ValueArg> = emptyList(),
    lambda: Node.Expr.Call.TrailLambda? = null
) = Node.Expr.Call(expr, typeArgs, args, lambda)

fun property(
    vars: List<Node.Decl.Property.Var?>,
    mods: List<Node.Modifier> = emptyList(),
    readOnly: Boolean = true,
    typeParams: List<Node.TypeParam> = emptyList(),
    receiverType: Node.Type? = null,
    typeConstraints: List<Node.TypeConstraint> = emptyList(),
    delegated: Boolean = false,
    expr: Node.Expr? = null,
    accessors: Node.Decl.Property.Accessors? = null
) = Node.Decl.Property(mods, readOnly, typeParams, receiverType, vars, typeConstraints, delegated, expr, accessors)