/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.gradleAstBuilder

import com.intellij.psi.PsiType
import org.gradle.api.NamedDomainObjectCollection
import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.GrLambdaExpression
import org.jetbrains.plugins.groovy.lang.psi.api.GrRangeExpression
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrSpreadArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrPropertySelection
import java.math.BigDecimal

fun GrExpression.toGradleAst(): GExpression = when (this) {
    is GrUnaryExpression -> toGradleAst()
    is GrOperatorExpression -> toGradleAst()
    is GrLiteral -> toGradleAst()
    is GrReferenceExpression -> toGradleAst()
    is GrListOrMap -> toGradleAst()
    is GrFunctionalExpression -> toGradleAst()
    is GrCallExpression -> toGradleAst()
    is GrRangeExpression -> TODO(this::class.toString())
    is GrInstanceOfExpression -> TODO(this::class.toString())
    is GrSafeCastExpression -> TODO(this::class.toString())
    is GrConditionalExpression -> TODO(this::class.toString())
    is GrTypeCastExpression -> TODO(this::class.toString())
    is GrBuiltinTypeClassExpression -> TODO(this::class.toString())
    is GrPropertySelection -> TODO(this::class.toString())
    is GrSpreadArgument -> TODO(this::class.toString())
    is GrIndexProperty -> TODO(this::class.toString())
    is GrTupleAssignmentExpression -> TODO(this::class.toString())
    is GrParenthesizedExpression -> TODO(this::class.toString())
    else -> unreachable()
}


fun GrFunctionalExpression.toGradleAst(): GExpression {
    return when (this) {
        is GrLambdaExpression -> TODO(this::class.toString())
        is GrClosableBlock -> toGradleAst()
        else -> unreachable()
    }
}

fun GrListOrMap.toGradleAst(): GExpression {
    return if (isMap) {
        TODO()
    } else {
        GList(initializers.map { it.toGradleAst() }, this)
    }
}

fun GrLiteral.toGradleAst(): GExpression {
    val value = value
    return when {
        this is GrString -> GString(toSimpleString(), this)
        value is String -> GString(value.cast(), this)
        value is Int || (value is BigDecimal && value.scale() == 0) -> GConst(
            value.toString(),
            GConst.Type.INT,
            this
        )
        value is Float || (value is BigDecimal && value.scale() != 0) -> GConst(
            value.toString(),
            GConst.Type.FLOAT,
            this
        )
        value is Char -> GConst(value.toString(), GConst.Type.CHAR, this)
        value is Boolean -> GConst(value.toString(), GConst.Type.BOOLEAN, this)
        value == null -> GConst("null", GConst.Type.NULL, this)
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
                GString(referenceName!!),
                this
            )
        } else {
            q.type?.let {
                val qName = NamedDomainObjectCollection::class.qualifiedName ?: return@let
                val type = PsiType.getTypeByName(qName, q.project, q.resolveScope)
                // TODO Кажется здесь я хотел проверить на неймдомейнобжект коллекцию
            }
            GSimplePropertyAccess(q.toGradleAst(), referenceNameElement!!.toGradleAst() as GIdentifier, this)
        }
    } else {
        GIdentifier(referenceName!!, this)
//        if (referenceName in tasks.keys) {
//            GSimpleTaskAccess(referenceName!!, tasks.getValue(referenceName!!), this)
//        } else {
//            GIdentifier(referenceName!!, this)
//        }
    }
}


fun GrUnaryExpression.toGradleAst(): GExpression {
    return GUnaryExpression(
        operand!!.toGradleAst(),
        GUnaryOperator.byValue(operationToken.text) ?: error("bad token for unary operation"),
        !isPostfix,
        this
    )
}

fun GrOperatorExpression.toGradleAst(): GBinaryExpression {
    return when (this) {
        is GrBinaryExpression -> GBinaryExpression(
            leftOperand.toGradleAst(),
            GBinaryOperator.byValue(operationToken.text),
            rightOperand!!.toGradleAst(),
            this
        )
        is GrAssignmentExpression -> GBinaryExpression(
            lValue.toGradleAst(),
            GBinaryOperator.byValue(operationToken.text),
            rValue!!.toGradleAst(),
            this
        )
        else -> unreachable()
    }
}

fun GrCallExpression.toGradleAst(): GExpression = when (this) {
    is GrMethodCall -> toGradleAst().cast()
    is GrNewExpression -> TODO(this::class.toString())
    else -> unreachable()
}

fun GrMethodCall.toGradleAst(): GExpression {
    return toSimpleMethodCall()
}

fun GrMethodCall.toSimpleMethodCall(): GSimpleMethodCall {
    val referenceExpression = invokedExpression as GrReferenceExpression
    val obj = referenceExpression.qualifierExpression
    val method = referenceExpression.referenceNameElement
    return GSimpleMethodCall(
        obj?.toGradleAst(),
        method!!.toGradleAst() as GIdentifier,
        argumentList.toGradleAst(),
        closureArguments.firstOrNull()?.toGradleAst(),
        this
    )
}

fun GrClosableBlock.toGradleAst(): GClosure {
    return GClosure(
        parameters.map { it.toGradleAst() },
        (this as GrCodeBlock).toGradleAst(),
        this
    )
}