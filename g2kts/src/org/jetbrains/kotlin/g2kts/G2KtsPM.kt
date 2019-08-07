/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.transform.sc.transformers.CompareIdentityExpression
import org.codehaus.groovy.transform.sc.transformers.CompareToNullExpression
import org.jetbrains.kotlin.utils.addToStdlib.cast

fun buildProject(statement: Statement): GProject {
    if (statement !is BlockStatement) TODO()
    return GProject(statement.statements.map { it.toGNode() })
}

fun Statement.toGNode(): GStatement = when (this) {
    is ExpressionStatement -> expression.toGNode().toStatement()
    is BlockStatement -> GBlock(statements.map { it.toGNode() })
    else -> TODO(this::class.toString())
}

fun Expression.toGNode(): GExpression = when (this) {
    is MethodCallExpression -> toGNode()
    is ConstantExpression -> toGNode()
    is VariableExpression -> toGNode()
    is ClosureExpression -> GClosure(emptyList(), code.toGNode().cast())
    is BinaryExpression -> toGNode()
    is PropertyExpression -> toGNode()
    else -> TODO(this::class.toString())
}

fun PropertyExpression.toGNode(): GExpression {
    val obj = objectExpression.toGNode()
    val property = property.toGNode()
    return if (obj is GName && obj.name == "this" && property is GName && property.name in extensions) {
        GExtensionAccess(obj, property)
    } else {
        GSimplePropertyAccess(obj, property)
    }
}

fun NamedArgumentListExpression.toGNode(): GArgumentsList =
    GArgumentsList(mapEntryExpressions.map { GArgument(it.keyExpression.text, it.valueExpression.toGNode()) })

fun BinaryExpression.toGNode(): GExpression = when (this) {
    is CompareToNullExpression -> TODO()
    is CompareIdentityExpression -> TODO()
    is DeclarationExpression -> TODO()
    else -> GBinaryExpression(
        leftExpression.toGNode(),
        GOperator.byValue(operation.text),
        rightExpression.toGNode()
    )
}

fun VariableExpression.toGNode(): GExpression = when (this) {
    VariableExpression.THIS_EXPRESSION -> GName("this")
    VariableExpression.SUPER_EXPRESSION -> GName("super")
    else -> GName(this.text) // GTODO create GPropertyAccess or something
}

fun ConstantExpression.toGNode(): GExpression {
    return when (val value = value) {
        is String -> GString(value)
        //BOOLEAN, CHAR, INT, FLOAT, NULL

        is Char -> TODO()
        else -> GConst(
            value.toString(),
            when (value) {
                is Boolean -> GConst.Type.BOOLEAN
                is Char -> GConst.Type.CHAR
                is Int -> GConst.Type.INT
                is Float -> GConst.Type.FLOAT
                else -> GConst.Type.NULL
            }
        )
    }
}

fun TupleExpression.toGArgumentList(): GArgumentsList = GArgumentsList(
    expressions.flatMap { expr ->
        when (expr) {
            is MapExpression -> expr.mapEntryExpressions.map {
                GArgument(it.keyExpression.text, it.valueExpression.toGNode())
            }
            else -> listOf(GArgument(null, expr.toGNode()))
        }
    }
)

fun createTask(task: GConfigurationBlock): GTaskCreating {
    return GTaskCreating(
        (task.method as GName).name,
        "",
        task.configuration
    )
}

fun MethodCallExpression.toGNode(): GExpression {
    val obj = objectExpression.toGNode()
    val m = when (method) {
        is ConstantExpression -> GName((method as ConstantExpression).text)
        else -> method.toGNode()
    }
    val args: GArgumentsList = when (val a = arguments) {
        is TupleExpression -> a.toGArgumentList()
        else -> error("cant parse arguments")
    }
    return when {
        m is GName && m.name in vars && args.args.size == 1 -> GBinaryExpression(
            m,
            GOperator.Common(GOperator.Token.ASSN),
            args.args.first().expr
        )
        m is GName && m.name == "task" && args.args.size == 1 && args.args.first().expr is GConfigurationBlock ->
            createTask(args.args.first().expr.cast())
        args.args.lastOrNull()?.expr is GClosure -> GConfigurationBlock(
            obj,
            m,
            GArgumentsList(args.args.slice(0 until (args.args.size - 1))),
            args.args.last().expr.cast()
        )
        else -> GSimpleMethodCall(obj, m, args)
    }
}