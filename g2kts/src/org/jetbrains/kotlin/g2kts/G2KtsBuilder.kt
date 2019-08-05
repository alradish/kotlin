/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.g2kts.tree.*
import org.jetbrains.kotlin.g2kts.tree.impl.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.GrRangeExpression
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrSpreadArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrAssertStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrFlowInterruptingStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrReturnStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrThrowStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrPropertySelection
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.GrTopStatement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.packaging.GrPackageDefinition

//fun buildTree(psi: GroovyPsiElement): GradleElement {
//    when(psi) {
//
//    }
//}

fun GroovyFileBase.toGradleAst(): GradleProject {
    return GradleProjectImpl(topStatements.map { it.toGradleAst() })
}

fun GrTopStatement.toGradleAst(): GradleStatement = when (this) {
    is GrImportStatement -> TODO(this.text)
    is GrPackageDefinition -> TODO(this.text)
    is GrMethod -> TODO(this.text)
    is GrStatement -> toGradleAst().also { println("${this.text}=$it") }
    is GrTypeDefinition -> TODO(this.text)
    else -> error("Unreachable code")
}

fun GrStatement.toGradleAst(): GradleStatement = when (this) {
    is GrTryCatchStatement -> TODO(text)
    is GrExpression -> toGradleAst().cast()
    is GrApplicationStatement -> TODO(text)
    is GrFlowInterruptingStatement -> TODO(text)
    is GrWhileStatement -> TODO(text)
    is GrIfStatement -> TODO(text)
    is GrVariableDeclaration -> TODO(text)
    is GrLoopStatement -> TODO(text)
    is GrConstructorInvocation -> TODO(text)
    is GrAssertStatement -> TODO(text)
    is GrReturnStatement -> TODO(text)
    is GrLabeledStatement -> TODO(text)
    is GrSwitchStatement -> TODO(text)
    is GrThrowStatement -> TODO(text)
    is GrSynchronizedStatement -> TODO(text)
    is GrBlockStatement -> TODO(text)
    else -> error("Unreachable code")
}

fun GrExpression.toGradleAst(): GradleExpression = when (this) {
    is GrRangeExpression -> TODO(this::class.toString())
    is GrInstanceOfExpression -> TODO(this::class.toString())
    is GrUnaryExpression -> TODO(this::class.toString())
    is GrOperatorExpression -> toGradleAst()
    is GrLiteral -> TODO(this::class.toString())
    is GrSafeCastExpression -> TODO(this::class.toString())
    is GrReferenceExpression -> toGradleAst()
    is GrConditionalExpression -> TODO(this::class.toString())
    is GrTypeCastExpression -> TODO(this::class.toString())
    is GrBuiltinTypeClassExpression -> TODO(this::class.toString())
    is GrPropertySelection -> TODO(this::class.toString())
    is GrListOrMap -> TODO(this::class.toString())
    is GrSpreadArgument -> TODO(this::class.toString())
    is GrIndexProperty -> TODO(this::class.toString())
    is GrFunctionalExpression -> TODO(this::class.toString())
    is GrTupleAssignmentExpression -> TODO(this::class.toString())
    is GrParenthesizedExpression -> TODO(this::class.toString())
    is GrCallExpression -> toGradleAst()
    else -> error("Unreachable code")
}

fun GrReferenceExpression.toGradleAst(): GradleExpression {
    println(text)
    val method = resolve() as PsiMethod
    println(method.containingClass)
    TODO()
}

fun GrOperatorExpression.toGradleAst(): GradleExpression { // GTODO intreface Ope
    when (this) {
        is GrBinaryExpression -> TODO()
        is GrAssignmentExpression -> {
            return GradleAssignmentImpl(
                lValue.toGradleAst(),
                rValue?.toGradleAst() ?: error("Miss rvalue"),
                GradleOperatorImpl(operationToken.text)
            )
        }
        else -> error("Unreachable code")
    }
}

fun GrCallExpression.toGradleAst(): GradleExpression = when (this) {
    is GrMethodCall -> toGradleAst()
    is GrNewExpression -> TODO(this::class.toString())
    else -> error("Unreachable code")
}

fun GrMethodCall.toGradleAst(): GradleMethodCall = when (this) {
    is GrMethodCallExpression -> {
        if (hasClosureArguments()) {
            GradleBlockImpl(
                null /* GTODO */,
                GradleNameIdentifierImpl(invokedExpression.text ?: error("empty name")),
                argumentList.toGradleAst(),
                closureArguments.last().toGradleAst()
            )
        } else {
            TODO()
        }

    }
    is GrApplicationStatement -> {
        GradleMethodCallImpl(
            null,
            GradleNameIdentifierImpl(invokedExpression.text ?: error("empty name")), argumentList.toGradleAst()
        )
    }
    else -> error("Unreachable code")
}

fun GrArgumentList.toGradleAst(): GradleArgumentList {
    return GradleArgumentListImpl(allArguments.map { arg ->
        when (arg) {
            is GrLiteral -> GradleConstantImpl(arg.value)
            else -> TODO()
        }
    })
}

fun GrClosableBlock.toGradleAst(): GradleLambda {
    return GradleLambdaImpl(null, statements.map { it.toGradleAst() })
}

fun GrMethod.toGradleAst(): GradleMethodCall {
    TODO(this.toString())
}

fun buildTree(psi: PsiElement): GradleElement = when (psi) {
    is GroovyFileBase -> psi.toGradleAst()
    else -> TODO()
}