/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import kastree.ast.Node
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


inline fun <reified T : Node> List<T>.transform(): List<T> = map { transform(it) as T }
inline fun <reified T : Node> List<T?>.transformOrNull(): List<T?> = map { transformOrNull(it) as? T }

fun transformOrNull(node: Node?): Node? = node?.let(::transform)

fun taskArgsToCall(args: List<Node.ValueArg>): List<Node.Expr.Call> {
    val taskArgs = args.map { it.expr as Node.Expr.Call }
    val maps = taskArgs.find { it.name == "mapOf" }
    return maps?.let {
        it.args.map { arg ->
            val e = arg.expr as Node.Expr.BinaryOp
            call(
                name((e.lhs as Node.Expr.StringTmpl).text()),
                args = listOf(
                    Node.ValueArg(
                        null,
                        false,
                        (e.rhs as Node.Expr.Name)
                    )
                )
            )
        }
    } ?: emptyList()
}

fun taskNamedArgsToCall(args: List<Node.ValueArg>): List<Node.Expr.Call> {
    return args.mapNotNull { arg ->
        call(
            name(arg.name ?: return@mapNotNull null),
            args = listOf(Node.ValueArg(null, false, transform(arg.expr).cast()))
        )
    }
}

fun transformToTask(node: Node.Expr.Call): Node.Decl.Property {
    val task = (node.args.first().expr as Node.Expr.Call) // GTODO может быть и строка
    val taskName = task.name ?: error("null task name")

    val lambda: Node.Expr.Call.TrailLambda? = if (task.args.isEmpty()) {
        transformOrNull(task.lambda).safeAs()
    } else {
        val inv = taskArgsToCall(task.args).map { Node.Stmt.Expr(it) }.transform()
        val anns = task.lambda?.anns?.transform() ?: emptyList()
        val label = task.lambda?.label
        val params = task.lambda?.func?.params?.transform() ?: emptyList()
        val stmts = inv + (task.lambda?.func?.block?.stmts?.transform() ?: emptyList())
        Node.Expr.Call.TrailLambda(
            anns, label,
            Node.Expr.Brace(
                params,
                Node.Block(stmts)
            )
        )
    }
    val expr = name("tasks") dot call(name("creating"), lambda = lambda)
    return property(listOf(Node.Decl.Property.Var(taskName, null)), delegated = true, expr = expr)
}

fun transformSpecialToken(name: Node.Expr.Name, args: List<Node.ValueArg>, types: List<MemberType>): Node.Expr {
    return when {
        MemberType.VAR in types && args.size == 1 -> name assn transform(args.first().expr).cast()
        MemberType.TASK in types && args.isEmpty() -> Node.Expr.ArrayAccess(
            name("tasks"),
            listOf(
                Node.Expr.StringTmpl(
                    listOf(
                        Node.Expr.StringTmpl.Elem.Regular(name.name)
                    ), false
                )
            )
        )
        else -> TODO()
    }
}

fun transform(node: Node): Node = node.run {
    when (this) {
        is Node.File -> Node.File(anns.transform(), transformOrNull(pkg).safeAs(), imports.transform(), decls.transform())
        is Node.Script -> TODO()
        is Node.Package -> TODO()
        is Node.Import -> TODO()
        is Node.Decl.Structured -> TODO()
        is Node.Decl.Init -> TODO()
        is Node.Decl.Func -> TODO()
        is Node.Decl.Property -> TODO()
        is Node.Decl.TypeAlias -> TODO()
        is Node.Decl.Constructor -> TODO()
        is Node.Decl.EnumEntry -> TODO()
        is Node.Decl.Structured.Parent.CallConstructor -> TODO()
        is Node.Decl.Structured.Parent.Type -> TODO()
        is Node.Decl.Structured.PrimaryConstructor -> TODO()
        is Node.Decl.Func.Param -> TODO()
        is Node.Decl.Func.Body.Block -> TODO()
        is Node.Decl.Func.Body.Expr -> TODO()
        is Node.Decl.Property.Var -> TODO()
        is Node.Decl.Property.Accessors -> TODO()
        is Node.Decl.Property.Accessor.Get -> TODO()
        is Node.Decl.Property.Accessor.Set -> TODO()
        is Node.Decl.Constructor.DelegationCall -> TODO()
        is Node.TypeParam -> TODO()
        is Node.TypeConstraint -> TODO()
        is Node.TypeRef.Paren -> TODO()
        is Node.TypeRef.Func -> TODO()
        is Node.TypeRef.Simple -> TODO()
        is Node.TypeRef.Nullable -> TODO()
        is Node.TypeRef.Dynamic -> TODO()
        is Node.TypeRef.Func.Param -> TODO()
        is Node.TypeRef.Simple.Piece -> TODO()
        is Node.Type -> TODO()
        is Node.ValueArg -> Node.ValueArg(name, asterisk, transform(expr).cast())
        is Node.Expr.If -> TODO()
        is Node.Expr.Try -> TODO()
        is Node.Expr.For -> TODO()
        is Node.Expr.While -> TODO()
        is Node.Expr.BinaryOp -> Node.Expr.BinaryOp(transform(lhs).cast(), transform(oper).cast(), transform(rhs).cast())
        is Node.Expr.UnaryOp -> TODO()
        is Node.Expr.TypeOp -> TODO()
        is Node.Expr.DoubleColonRef.Callable -> TODO()
        is Node.Expr.DoubleColonRef.Class -> TODO()
        is Node.Expr.Paren -> TODO()
        is Node.Expr.StringTmpl -> Node.Expr.StringTmpl(elems.transform(), raw)
        is Node.Expr.Const -> this
        is Node.Expr.Brace -> Node.Expr.Brace(params.transform(), transformOrNull(block).safeAs())
        is Node.Expr.Brace.Param -> TODO()
        is Node.Expr.This -> TODO()
        is Node.Expr.Super -> TODO()
        is Node.Expr.When -> TODO()
        is Node.Expr.Object -> TODO()
        is Node.Expr.Throw -> TODO()
        is Node.Expr.Return -> TODO()
        is Node.Expr.Continue -> TODO()
        is Node.Expr.Break -> TODO()
        is Node.Expr.CollLit -> TODO()
        is Node.Expr.Name -> when (name) {
            in tasks -> Node.Expr.ArrayAccess(
                name("tasks"),
                listOf(Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(name)), false))
            )
            in extensions -> TODO()
            else -> this
        }
        is Node.Expr.Labeled -> TODO()
        is Node.Expr.Annotated -> TODO()
        is Node.Expr.Call -> {
            this.convertMapToNamed().run {
                val callExpr = expr
                when {
                    callExpr is Node.Expr.Name && callExpr.name == "task" ->
                        transformToTask(this)
                    callExpr is Node.Expr.Name && callExpr.name in vars && args.size == 1 ->
                        callExpr assn transform(args.first().expr).cast()
                    else ->
                        call(transform(expr).cast(), typeArgs.transformOrNull(), args.transform(), transformOrNull(lambda).safeAs())
                }
            }
        }
        is Node.Expr.ArrayAccess -> TODO()
        is Node.Expr.AnonFunc -> TODO()
        is Node.Expr.Property -> TODO()
        is Node.Expr.Try.Catch -> TODO()
        is Node.Expr.BinaryOp.Oper.Infix -> this
        is Node.Expr.BinaryOp.Oper.Token -> this
        is Node.Expr.UnaryOp.Oper -> TODO()
        is Node.Expr.TypeOp.Oper -> TODO()
        is Node.Expr.DoubleColonRef.Recv.Expr -> TODO()
        is Node.Expr.DoubleColonRef.Recv.Type -> TODO()
        is Node.Expr.StringTmpl.Elem.Regular -> this
        is Node.Expr.StringTmpl.Elem.ShortTmpl -> this
        is Node.Expr.StringTmpl.Elem.UnicodeEsc -> this
        is Node.Expr.StringTmpl.Elem.RegularEsc -> this
        is Node.Expr.StringTmpl.Elem.LongTmpl -> Node.Expr.StringTmpl.Elem.LongTmpl(transform(expr).cast())
        is Node.Expr.When.Entry -> TODO()
        is Node.Expr.When.Cond.Expr -> TODO()
        is Node.Expr.When.Cond.In -> TODO()
        is Node.Expr.When.Cond.Is -> TODO()
        is Node.Expr.Call.TrailLambda -> Node.Expr.Call.TrailLambda(anns.transform(), label, transform(func).cast())
        is Node.Block -> Node.Block(stmts.transform())
        is Node.Stmt.Decl -> TODO()
        is Node.Stmt.Expr -> {
            when (val t = transform(expr)) {
                is Node.Expr -> Node.Stmt.Expr(t)
                is Node.Decl -> Node.Stmt.Decl(t)
                else -> error("bad expr transform")
            }
        }
        is Node.Modifier.AnnotationSet -> TODO()
        is Node.Modifier.Lit -> TODO()
        is Node.Modifier.AnnotationSet.Annotation -> TODO()
        is Node.Extra.BlankLines -> this
        is Node.Extra.Comment -> this
    }
}