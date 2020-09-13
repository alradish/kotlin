/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import kastree.ast.ExtrasMap
import kastree.ast.Node
import org.jetbrains.kotlin.utils.addToStdlib.cast

class GradleToKotlin {
    val extrasMap = ExtrasMapImpl()

    private fun GNode.toStatement(): GStatement? {
        return when (this) {
            is ConvertableToStatement -> this.toStatement()
            is GStatement -> this
            else -> null
        }
    }

    private fun List<GStatement>.toKotlin(): List<Node.Stmt> {
        val res = mutableListOf<Node.Stmt>()
        val extras: MutableList<Node.Extra> = mutableListOf()
        for (stmt in this) {
            when (stmt) {
                is GComment ->
                    extras.add(stmt.toKotlin() as Node.Extra.Comment)
                is GNewLine ->
                    extras.add(stmt.toKotlin() as Node.Extra.BlankLines)
                is GBlock -> {
                    val element = stmt.toKotlin() as Node.Stmt
                    res.add(element)
                    extras.forEach { extrasMap.addExtraBefore(element, it) }
                    extras.clear()
                }
                is GStatement.GExpr, is GStatement.GDecl -> {
                    var buf = when (stmt) {
                        is GStatement.GExpr -> stmt.expr.toKotlin()
                        is GStatement.GDecl -> stmt.decl.toKotlin()
                        else -> unreachable()
                    }
                    buf = when (buf) {
                        is Node.Expr -> Node.Stmt.Expr(buf)
                        is Node.Decl -> Node.Stmt.Decl(buf)
                        else -> buf
                    }

                    res.add(buf as Node.Stmt)
                    extras.forEach { extrasMap.addExtraBefore(buf, it) }
                    extras.clear()
                }
            }
        }
        if (extras.isNotEmpty() && res.isEmpty()) {
            extras.clear() // TODO не могу сохранить extras, т.к. нет доступа до Node
        }
        extras.forEach { extrasMap.addExtraAfter(res.last(), it) }
        return res
    }

    fun GNode.toKotlin(): Node = when (this) {
        is GComment -> Node.Extra.Comment(string, startsLine, ensLine)
        is GNewLine -> Node.Extra.BlankLines(n)
        is GProject -> Node.Block(statements.toKotlin())
        is GBlock -> Node.Block(statements.toKotlin())
        is GBrace -> Node.Expr.Brace(emptyList(), block?.toKotlin()?.cast())
        is GWhile -> Node.Expr.While(
            condition.toKotlin().cast(),
            body.toKotlin().cast(),
            false
        )
        is GStatement -> {
            val res = when (this) {
                is GStatement.GExpr -> expr.toKotlin()
                is GStatement.GDecl -> decl.toKotlin()
                is GBlock -> toKotlin()
                is GComment -> toKotlin()
                is GNewLine -> toKotlin()
            }
            when (res) {
                is Node.Expr -> Node.Stmt.Expr(res)
                is Node.Decl -> Node.Stmt.Decl(res)
                else -> res
            }
        }
        is GArgumentsList -> TODO()
        is GIdentifier -> Node.Expr.Name(name)
        is GMethodCall -> {
            val expr: Node.Expr = when (obj) {
                null -> method.toKotlin().cast()
                else -> obj!!.toKotlin().cast<Node.Expr>() dot method.toKotlin().cast()
            }
            val typeArgs =
                typeArguments.map { Node.Type(emptyList(), Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(it, emptyList())))) }

            val lambda = closure?.let { lambda(it.toKotlin().cast()) }
            Node.Expr.Call(expr, typeArgs, arguments.args.map { it.toKotlin() as Node.ValueArg }, lambda)
        }
        is GClosure -> Node.Expr.Brace(emptyList(), statements.toKotlin().cast())
        is GTaskCreating -> {
            val typeArgs = if (type.isEmpty()) emptyList() else listOf(
                Node.Type(
                    emptyList(),
                    Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(type, emptyList())))
                )
            )
            val lambda = body?.let { lambda(it.toKotlin().cast()) }
            name("tasks") dot call(
                expr = name("register"),
                typeArgs = typeArgs,
                args = listOf(Node.ValueArg(null, false, simpleString(name))),
                lambda
            )
        }
        is GConst -> Node.Expr.Const(text, Node.Expr.Const.Form.valueOf(type.toString()))
        is GString -> Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(str)), false)
        is GBinaryExpression -> Node.Expr.BinaryOp(left.toKotlin().cast(), operator.toKotlin().cast(), right.toKotlin().cast())
        is GSimplePropertyAccess ->
            obj?.toKotlin()?.cast<Node.Expr>()?.dot(property.toKotlin().cast()) ?: property.toKotlin()
        is GExtensionAccess -> Node.Expr.ArrayAccess(obj!!.toKotlin().cast(), listOf(property.toKotlin().cast()))
        is GBinaryOperator.Common -> Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.values().find { it.str == token.text } ?: error(
            ""
        ))
        is GBinaryOperator.Uncommon -> Node.Expr.BinaryOp.Oper.Infix(text)
        is GUnaryExpression -> Node.Expr.UnaryOp(expr.toKotlin().cast(), operator.toKotlin().cast(), prefix)
        is GUnaryOperator -> Node.Expr.UnaryOp.Oper(Node.Expr.UnaryOp.Token.values().find { it.str == token.text } ?: error(""))
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
                listOf(Node.Type(emptyList(), Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(type ?: "Task", emptyList()))))),
                listOf(Node.ValueArg(null, false, Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(task)), false))),
                if (this is GTaskConfigure) lambda(configure.toKotlin().cast()) else null
            )
        )
        is GBuildScriptBlock -> call(
            expr = name(type.text),
            lambda = lambda(block.toKotlin().cast())
        )
        is GVariableDeclaration -> {
            property(
                vars = listOf(
                    Node.Decl.Property.Var(
                        name.name,
                        type?.let {
                            Node.Type(
                                emptyList(),
                                Node.TypeRef.Nullable(Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(it, emptyList()))))
                            )
                        })
                ),
                expr = expr?.toKotlin()?.cast(),
                readOnly = false
            )
        }
        is GIf -> Node.Expr.If(condition.toKotlin().cast(), body.toKotlin().cast(), elseBody?.toKotlin()?.cast())
        is GTryCatch -> Node.Expr.Try(
            body.toKotlin().cast(),
            catches.map {
                Node.Expr.Try.Catch(
                    emptyList(),
                    it.name,
                    Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(it.type, emptyList()))),
                    it.block.toKotlin().cast()
                )
            },
            finallyBody?.toKotlin()?.cast()
        )
        is GSwitchCase -> Node.Expr.When.Entry(
            listOf(Node.Expr.When.Cond.Expr(expr.toKotlin().cast())),
            body.toKotlin().cast()
        )
        is GSwitch -> {
            val entries = cases.map { it.toKotlin() as Node.Expr.When.Entry }
            Node.Expr.When(
                expr.toKotlin().cast(),
                if (default == null) entries else entries.plus(default!!.toKotlin() as Node.Expr.When.Entry)
            )
        }
        is Catch -> TODO()
    }


}

class ExtrasMapImpl : ExtrasMap {
    private val extras: MutableList<Node.Extra> = mutableListOf()

    private val extrasAfter: MutableMap<Node, MutableList<Node.Extra>> = mutableMapOf()
    private val extrasBefore: MutableMap<Node, MutableList<Node.Extra>> = mutableMapOf()
    private val extrasWithin: MutableMap<Node, MutableList<Node.Extra>> = mutableMapOf()

    override fun extrasAfter(v: Node): List<Node.Extra> {
        return extrasAfter[v]?.toList() ?: return emptyList()
    }

    override fun extrasBefore(v: Node): List<Node.Extra> {
        return extrasBefore[v]?.toList() ?: return emptyList()
    }

    override fun extrasWithin(v: Node): List<Node.Extra> {
        return extrasWithin[v]?.toList() ?: return emptyList()
    }

    fun addExtraAfter(v: Node, extra: Node.Extra) {
        val list = extrasAfter[v]
        if (list == null) {
            extrasAfter[v] = mutableListOf(extra)
        } else {
            list.add(extra)
        }
//        extrasAfter.getOrDefault(v, mutableListOf()).add(extra)
    }

    fun addExtraBefore(v: Node, extra: Node.Extra) {
        val list = extrasBefore[v]
        if (list == null) {
            extrasBefore[v] = mutableListOf(extra)
        } else {
            list.add(extra)
        }
//        extrasBefore.getOrDefault(v, mutableListOf()).add(extra)
    }

    fun addExtraWithin(v: Node, extra: Node.Extra) {
        val list = extrasWithin[v]
        if (list == null) {
            extrasWithin[v] = mutableListOf(extra)
        } else {
            list.add(extra)
        }
//        extrasWithin.getOrDefault(v, mutableListOf()).add(extra)
    }
}

//fun GNode.toKotlin(): Node = when (this) {
//    is GComment -> TODO("add comment to struct")
//    is GProject -> Node.Block(statements.map { it.toKotlin() as Node.Stmt })
//    is GBlock -> Node.Block(statements.map { it.toKotlin() as Node.Stmt })
//    is GBrace -> Node.Expr.Brace(emptyList(), block?.toKotlin()?.cast())
//    is GWhile -> Node.Expr.While(
//        condition.toKotlin().cast(),
//        body.toKotlin().cast(),
//        false
//    )
//    is GStatement -> {
//        val res = when (this) {
//            is GStatement.GExpr -> expr.toKotlin()
//            is GStatement.GDecl -> decl.toKotlin()
//            is GBlock -> toKotlin()
//            is GComment -> toKotlin()
//        }
//        when (res) {
//            is Node.Expr -> Node.Stmt.Expr(res)
//            is Node.Decl -> Node.Stmt.Decl(res)
//            else -> res
//        }
//    }
//    is GArgumentsList -> TODO()
//    is GIdentifier -> Node.Expr.Name(name)
//    is GMethodCall -> {
//        val expr: Node.Expr = when (obj) {
//            null -> method.toKotlin().cast()
//            else -> obj!!.toKotlin().cast<Node.Expr>() dot method.toKotlin().cast()
//        }
////        val lambda = when (this) {
////            is GConfigurationBlock -> lambda(closure.toKotlin().cast())
////            else -> null
////        }
//        val lambda = closure?.let { lambda(it.toKotlin().cast()) }
//        Node.Expr.Call(expr, emptyList(), arguments.args.map { it.toKotlin() as Node.ValueArg }, lambda)
//    }
//    is GClosure -> Node.Expr.Brace(emptyList(), statements.toKotlin().cast())
//    is GTaskCreating -> {
//        val lambda = body?.let { lambda(it.toKotlin().cast()) }
//        property(
//            vars = listOf(Node.Decl.Property.Var(name, null)),
//            delegated = true,
//            expr = name("tasks") dot call(name("creating"), lambda = lambda)
//        )
//    }
//    is GConst -> Node.Expr.Const(text, Node.Expr.Const.Form.valueOf(type.toString()))
//    is GString -> Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(str)), false)
//    is GBinaryExpression -> Node.Expr.BinaryOp(left.toKotlin().cast(), operator.toKotlin().cast(), right.toKotlin().cast())
//    is GSimplePropertyAccess ->
//        obj?.toKotlin()?.cast<Node.Expr>()?.dot(property.toKotlin().cast()) ?: property.toKotlin()
//    is GExtensionAccess -> Node.Expr.ArrayAccess(obj!!.toKotlin().cast(), listOf(property.toKotlin().cast()))
//    is GBinaryOperator.Common -> Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.values().find { it.str == token.text } ?: error(""))
//    is GBinaryOperator.Uncommon -> Node.Expr.BinaryOp.Oper.Infix(text)
//    is GUnaryExpression -> Node.Expr.UnaryOp(expr.toKotlin().cast(), operator.toKotlin().cast(), prefix)
//    is GUnaryOperator -> Node.Expr.UnaryOp.Oper(Node.Expr.UnaryOp.Token.values().find { it.str == token.text } ?: error(""))
//    is GArgument -> Node.ValueArg(name, false, expr.toKotlin().cast())
//    is GList -> Node.Expr.Call(
//        Node.Expr.Name("listOf"),
//        emptyList(),
//        initializers.map { Node.ValueArg(null, false, it.toKotlin() as Node.Expr) },
//        null
//    )
//    is GTaskAccess -> Node.Expr.BinaryOp(
//        Node.Expr.Name("tasks"),
//        Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.DOT),
//        Node.Expr.Call(
//            Node.Expr.Name("named"),
//            listOf(Node.Type(emptyList(), Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(type ?: "Task", emptyList()))))),
//            listOf(Node.ValueArg(null, false, Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(task)), false))),
//            if (this is GTaskConfigure) lambda(configure.toKotlin().cast()) else null
//        )
//    )
//    is GBuildScriptBlock -> call(
//        expr = name(type.text),
//        lambda = lambda(block.toKotlin().cast())
//    )
//    is GVariableDeclaration -> {
//        property(
//            vars = listOf(
//                Node.Decl.Property.Var(
//                    name.name,
//                    type?.let {
//                        Node.Type(
//                            emptyList(),
//                            Node.TypeRef.Nullable(Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(it, emptyList()))))
//                        )
//                    })
//            ),
//            expr = expr?.toKotlin()?.cast(),
//            readOnly = false
//        )
//    }
//    is GIf -> Node.Expr.If(condition.toKotlin().cast(), body.toKotlin().cast(), elseBody?.toKotlin()?.cast())
//    is GTryCatch -> Node.Expr.Try(
//        body.toKotlin().cast(),
//        catches.map {
//            Node.Expr.Try.Catch(
//                emptyList(),
//                it.name,
//                Node.TypeRef.Simple(listOf(Node.TypeRef.Simple.Piece(it.type, emptyList()))),
//                it.block.toKotlin().cast()
//            )
//        },
//        finallyBody?.toKotlin()?.cast()
//    )
//    is GSwitchCase -> Node.Expr.When.Entry(
//        listOf(Node.Expr.When.Cond.Expr(expr.toKotlin().cast())),
//        body.toKotlin().cast()
//    )
//    is GSwitch -> {
//        val entries = cases.map { it.toKotlin() as Node.Expr.When.Entry }
//        Node.Expr.When(
//            expr.toKotlin().cast(),
//            if (default == null) entries else entries.plus(default!!.toKotlin() as Node.Expr.When.Entry)
//        )
//    }
//}