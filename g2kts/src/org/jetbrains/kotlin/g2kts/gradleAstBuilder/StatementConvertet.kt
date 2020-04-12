/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.gradleAstBuilder

import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.plugins.groovy.lang.psi.api.GrDoWhileStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrAssertStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrFlowInterruptingStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrReturnStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrThrowStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.GrTopStatement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.packaging.GrPackageDefinition
import org.jetbrains.plugins.groovy.lang.psi.impl.GrClassReferenceType

fun GrStatement.toGradleAst(): GStatement = when (this) {
    is GrTryCatchStatement -> toGradleAst().toStatement()
    is GrExpression -> toGradleAst().toStatement()
    is GrBlockStatement -> block.toGradleAst()
    is GrWhileStatement -> toGradleAst().toStatement()
    is GrIfStatement -> toGradleAst().toStatement()
    is GrVariableDeclaration -> toGradleAst().toStatement()
    is GrLoopStatement -> toGradleAst()
    is GrConstructorInvocation -> TODO(text)
    is GrAssertStatement -> TODO(text)
    is GrReturnStatement -> TODO(text)
    is GrLabeledStatement -> TODO(text)
    is GrThrowStatement -> TODO(text)
    is GrSynchronizedStatement -> TODO(text)
    is GrSwitchStatement -> TODO(text)
    is GrApplicationStatement -> TODO(text)
    is GrFlowInterruptingStatement -> TODO(text)
    else -> unreachable()
}

fun GrLoopStatement.toGradleAst() = when(this) {
    is GrWhileStatement -> toGradleAst().toStatement()
    is GrForStatement -> TODO(text)
    is GrDoWhileStatement -> TODO(text)
    else -> unreachable()
}

fun GrTryCatchStatement.toGradleAst(): GTryCatch {
    val body = tryBlock?.toGradleAst() as GBlock
    val catches = catchClauses.map {
        val parameter = it.parameter ?: error("")
        GTryCatch.Catch(parameter.name, parameter.type.presentableText, it.body?.toGradleAst() as GBlock)
    }
    val finallyBody = finallyClause?.body?.toGradleAst()
    return GTryCatch(
        body,
        catches,
        finallyBody,
        this
    )
}

fun GrIfStatement.toGradleAst(): GIf {
    val condition = condition?.toGradleAst() ?: error("empty if condition")
    val body = thenBranch?.toGradleAst()?.toGBrace() ?: error("miss if then branch")
    val elseBody = elseBranch?.toGradleAst()?.toGBrace()
    return GIf(
        condition,
        body,
        elseBody,
        this
    )
}

fun GrWhileStatement.toGradleAst(): GWhile {
    val condition = condition?.toGradleAst() ?: error("miss while condition")
    val body: GExpression = body?.toGradleAst()?.toGBrace() ?: error("miss while body")
    return GWhile(
        condition,
        body,
        this
    )
}

fun GrVariableDeclaration.toGradleAst(): GVariableDeclaration {
    val type = (typeElementGroovy?.type as? GrClassReferenceType)?.className
    val variable = this.variables.first()
    val name = GIdentifier(variable.name)
    val expr = variable.initializerGroovy?.toGradleAst()
    return GVariableDeclaration(
        type,
        name,
        expr,
        this
    )
}

fun GrTopStatement.toGradleAst(): GStatement = when (this) {
    is GrImportStatement -> TODO(this.text)
    is GrPackageDefinition -> TODO(this.text)
    is GrMethod -> TODO(this.text)
//    is GrStatement -> toGradleAst()/*.also { println("${this.text}=$it") }*/
    is GrStatement -> toGradleAst()
    is GrTypeDefinition -> TODO(this.text)
    else -> unreachable()
}

fun GrMethod.toGradleAst(): GMethodCall {
    TODO(this.toString())
}