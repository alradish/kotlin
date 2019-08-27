/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.gradleAstBuilder

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.gradle.api.NamedDomainObjectCollection
import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.GrRangeExpression
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrSpreadArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrPropertySelection
import java.math.BigDecimal

fun GrExpression.toGradleAst(): GExpression = when (this) {
    is GrRangeExpression -> TODO(this::class.toString())
    is GrInstanceOfExpression -> TODO(this::class.toString())
    is GrUnaryExpression -> TODO(this::class.toString())
    is GrOperatorExpression -> toGradleAst()
    is GrLiteral -> toGradleAst()
    is GrSafeCastExpression -> TODO(this::class.toString())
    is GrReferenceExpression -> toGradleAst()
    is GrConditionalExpression -> TODO(this::class.toString())
    is GrTypeCastExpression -> TODO(this::class.toString())
    is GrBuiltinTypeClassExpression -> TODO(this::class.toString())
    is GrPropertySelection -> TODO(this::class.toString())
    is GrListOrMap -> toGradleAst()
    is GrSpreadArgument -> TODO(this::class.toString())
    is GrIndexProperty -> TODO(this::class.toString())
    is GrFunctionalExpression -> TODO(this::class.toString())
    is GrTupleAssignmentExpression -> TODO(this::class.toString())
    is GrParenthesizedExpression -> TODO(this::class.toString())
    is GrCallExpression -> toGradleAst()
    else -> error("Unreachable code")
}

fun GrListOrMap.toGradleAst(): GExpression {
    return if (isMap) {
        TODO()
    } else {
        GList(initializers.map { it.toGradleAst() })
    }
}

fun GrLiteral.toGradleAst(): GExpression {
    val value = value
    return when {
        this is GrString -> GString(toSimpleString())
        value is String -> GString(value.cast())
        value is Int || (value is BigDecimal && value.scale() == 0) -> GConst(
            value.toString(),
            GConst.Type.INT
        )
        value is Float || (value is BigDecimal && value.scale() != 0) -> GConst(
            value.toString(),
            GConst.Type.FLOAT
        )
        value is Char -> GConst(value.toString(), GConst.Type.CHAR)
        value is Boolean -> GConst(value.toString(), GConst.Type.BOOLEAN)
        value == null -> GConst("null", GConst.Type.NULL)
//        else -> GConst(value.toString(), GConst.Type.NULL)
        else -> TODO(value::class.toString())
    }
}

fun GrReferenceExpression.toGradleAst(): GExpression {
    val q = qualifierExpression
    return if (q != null) {
        val qualifier = q.toGradleAst()
        if (qualifier is GIdentifier && qualifier.name in extensions) {
            GExtensionAccess(
                qualifier,
                GString(referenceName!!)
            )
        } else {
            q.type?.let {
                val qName = NamedDomainObjectCollection::class.qualifiedName ?: return@let
                val type = PsiType.getTypeByName(qName, q.project, q.resolveScope)
                if (it.equals(type) || it.superTypes.contains(type)) {
                    println("***************************${q.text}")
                }
            }
            GSimplePropertyAccess(q.toGradleAst(), referenceNameElement!!.toGradleAst())
        }
    } else {
        if (referenceName in tasks.keys) {
            GSimpleTaskAccess(referenceName!!, tasks.getValue(referenceName!!))
        } else {
            GIdentifier(referenceName!!)
        }
    }
}


fun GrOperatorExpression.toGradleAst(): GBinaryExpression {
    return when (this) {
        is GrBinaryExpression -> GBinaryExpression(
            leftOperand.toGradleAst(),
            GOperator.byValue(operationToken.text),
            rightOperand!!.toGradleAst()
        )
        is GrAssignmentExpression -> GBinaryExpression(
            lValue.toGradleAst(),
            GOperator.byValue("="),
            rValue!!.toGradleAst()
        )
        else -> error("Unreachable code")
    }
}

fun GrCallExpression.toGradleAst(): GExpression = when (this) {
    is GrMethodCall -> toGradleAst()
    is GrNewExpression -> TODO(this::class.toString())
    else -> error("Unreachable code")
}

fun GrMethodCall.toGradleAst(): GExpression {
    val (obj, method) = parseInvokedExpression()
    var gobj = obj?.toGradleAst()
    val gmethod = method!!.toGradleAst()

    return when {
        gmethod.name == "task" -> toTaskCreate()
        resolveMethod()?.containingClass?.qualifiedName == "org.gradle.api.Project" && invokedExpression.text in vars -> {
            val args = argumentList.toGradleAst()
            GBinaryExpression(
                GSimplePropertyAccess(gobj, gmethod),
                GOperator.byValue("="),
                args.args.first().expr
            )
        }
        gobj == null && gmethod.name in tasks.keys && hasClosureArguments() && argumentList.isEmpty -> {
            GTaskConfigure(
                gmethod.name,
                tasks.getValue(gmethod.name),
                closureArguments.last().toGradleAst()
            )
        }
        else -> when (this) {
            is GrMethodCallExpression -> {
                if (hasClosureArguments()) {
                    GConfigurationBlock(
                        gobj,
                        gmethod,
                        argumentList.toGradleAst(),
                        closureArguments.last().toGradleAst()
                    )
                } else
                    GSimpleMethodCall(gobj, gmethod, argumentList.toGradleAst())
            }
            is GrApplicationStatement -> GSimpleMethodCall(gobj, gmethod, argumentList.toGradleAst())
            else -> error("Unreachable code")
        }
    }
}

fun GrMethodCall.toTaskCreate(): GTaskCreating {
    val task = argumentList.allArguments.first().cast<GrMethodCall>()
    val (_, name) = task.parseInvokedExpression()
//    val map = task.argumentList.allArguments.map {
//        it.toGradleAst()
//    }
    return GTaskCreating(
        name?.toGradleAst().cast<GIdentifier>().name,
        "",
        task.closureArguments.first().toGradleAst()
    )
}

fun GrMethodCall.parseInvokedExpression(): Pair<GrExpression?, PsiElement?> {
    val referenceExpression = invokedExpression as GrReferenceExpression
    val obj = referenceExpression.qualifierExpression
    val method = referenceExpression.referenceNameElement
    return obj to method
}

fun GrMethodCall.toSimpleMethodCall(): GSimpleMethodCall {
    val (obj, method) = parseInvokedExpression()
    return GSimpleMethodCall(obj?.toGradleAst(), method!!.toGradleAst(), argumentList.toGradleAst())
}

fun GrClosableBlock.toGradleAst(): GClosure {
    return GClosure(
        parameters.map { it.toGradleAst() },
        GBlock(statements.map { it.toGradleAst() })
    )
}