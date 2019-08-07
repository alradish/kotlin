/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import kastree.ast.Node
import org.jetbrains.kotlin.utils.addToStdlib.cast

fun GNode.toKotlin(): Node = when (this) {
    is GProject -> Node.Block(statements.map { it.toKotlin() as Node.Stmt })
    is GBlock -> {
        Node.Block(statements.map { it.toKotlin() as Node.Stmt })
    }
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
    is GName -> Node.Expr.Name(name)
    is GSimpleMethodCall -> {
        val expr: Node.Expr = if (obj is GName && obj.name == "this")
            method.toKotlin().cast()
        else Node.Expr.BinaryOp(
            obj.toKotlin().cast(),
            Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.DOT),
            method.toKotlin().cast()
        )
        Node.Expr.Call(expr, emptyList(), arguments.args.map { it.toKotlin() as Node.ValueArg }, null)
    }
    is GConfigurationBlock -> {
        val expr: Node.Expr = if (obj is GName && obj.name == "this")
            method.toKotlin().cast()
        else Node.Expr.BinaryOp(
            obj.toKotlin().cast(),
            Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.DOT),
            method.toKotlin().cast()
        )
        Node.Expr.Call(
            expr,
            emptyList(),
            arguments.args.map { it.toKotlin() as Node.ValueArg },
            Node.Expr.Call.TrailLambda(
                emptyList(),
                null,
                configuration.toKotlin().cast()
            )
        )

    }
    is GClosure -> {
        Node.Expr.Brace(emptyList(), statements.toKotlin().cast())
    }
    is GTaskCreating -> {
        val lambda = Node.Expr.Call.TrailLambda(emptyList(), null, body.toKotlin().cast())
        Node.Decl.Property(
            emptyList(),
            true,
            emptyList(),
            null,
            listOf(Node.Decl.Property.Var(name, null)),
            emptyList(),
            true,
            name("tasks") dot call(name("creating"), lambda = lambda),
            null
        )
    }
    is GConst -> Node.Expr.Const(text, Node.Expr.Const.Form.valueOf(type.toString()))
    is GString -> Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(str)), false)
    is GBinaryExpression -> Node.Expr.BinaryOp(left.toKotlin().cast(), operator.toKotlin().cast(), right.toKotlin().cast())
    is GSimplePropertyAccess -> TODO()
    is GExtensionAccess -> TODO()
    is GTaskAccess -> TODO()
    is GOperator.Common -> Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.values().find { it.str == token.text } ?: error(""))
    is GOperator.Uncommon -> Node.Expr.BinaryOp.Oper.Infix(text)
    is GArgument -> Node.ValueArg(name, false, expr.toKotlin().cast())
}