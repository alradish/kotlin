/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import kastree.ast.Node
import org.jetbrains.kotlin.utils.addToStdlib.cast

fun GNode.toKotlin(): Node = when (this) {
    is GProject -> Node.Block(statements.map { it.toKotlin() as Node.Stmt })
    is GBlock -> Node.Block(statements.map { it.toKotlin() as Node.Stmt })
    is GStatement -> {
        val res = when (this) {
            is GStatement.GExpr -> expr.toKotlin()
            is GStatement.GDecl -> decl.toKotlin()
            else -> error("")
        }
        when (res) {
            is Node.Expr -> Node.Stmt.Expr(res)
            is Node.Decl -> Node.Stmt.Decl(res)
            else -> error("")
        }
    }
    is GArgumentsList -> TODO()
    is GIdentifier -> Node.Expr.Name(name)
    is GMethodCall -> {
        val expr: Node.Expr = when (obj) {
            null -> method.toKotlin().cast()
            else -> obj!!.toKotlin().cast<Node.Expr>() dot method.toKotlin().cast()
        }
        val lambda = when (this) {
            is GConfigurationBlock -> Node.Expr.Call.TrailLambda(
                emptyList(),
                null,
                configuration.toKotlin().cast()
            )
            else -> null
        }
        Node.Expr.Call(expr, emptyList(), arguments.args.map { it.toKotlin() as Node.ValueArg }, lambda)
    }
    is GClosure -> Node.Expr.Brace(emptyList(), statements.toKotlin().cast())
    is GTaskCreating -> {
        val lambda = Node.Expr.Call.TrailLambda(emptyList(), null, body.toKotlin().cast())
        property(
            vars = listOf(Node.Decl.Property.Var(name, null)),
            delegated = true,
            expr = name("tasks") dot call(name("creating"), lambda = lambda)
        )
    }
    is GConst -> Node.Expr.Const(text, Node.Expr.Const.Form.valueOf(type.toString()))
    is GString -> Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(str)), false)
    is GBinaryExpression -> Node.Expr.BinaryOp(left.toKotlin().cast(), operator.toKotlin().cast(), right.toKotlin().cast())
    is GSimplePropertyAccess ->
        if (obj != null) {
            obj.toKotlin().cast<Node.Expr>() dot property.toKotlin().cast()
        } else {
            property.toKotlin()
        }
    is GExtensionAccess -> Node.Expr.ArrayAccess(obj.toKotlin().cast(), listOf(property.toKotlin().cast()))
    is GOperator.Common -> Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.values().find { it.str == token.text } ?: error(""))
    is GOperator.Uncommon -> Node.Expr.BinaryOp.Oper.Infix(text)
    is GArgument -> Node.ValueArg(name, false, expr.toKotlin().cast())
    is GList -> Node.Expr.Call(
        Node.Expr.Name("listOf"),
        emptyList(),
        initializers.map { Node.ValueArg(null, false, it.toKotlin() as Node.Expr) },
        null
    )
    is GTaskAccess -> Node.Expr.BinaryOp(
        Node.Expr.Name("tasks"),
        Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.DOT),
        Node.Expr.Call(
            Node.Expr.Name("named"),
            listOf(Node.Type(emptyList(), Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(type.simpleName!!, emptyList()))))),
            listOf(Node.ValueArg(null, false, Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(task)), false))),
            if (this is GTaskConfigure) Node.Expr.Call.TrailLambda(emptyList(), null, configure.toKotlin().cast()) else null
        )
    )
}