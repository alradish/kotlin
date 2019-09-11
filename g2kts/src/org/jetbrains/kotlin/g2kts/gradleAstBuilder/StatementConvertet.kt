/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.gradleAstBuilder

import org.jetbrains.kotlin.g2kts.GMethodCall
import org.jetbrains.kotlin.g2kts.GStatement
import org.jetbrains.kotlin.g2kts.unreachable
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

fun GrStatement.toGradleAst(): GStatement = when (this) {
    is GrTryCatchStatement -> TODO(text)
    is GrExpression -> toGradleAst().toStatement()
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
    else -> unreachable()
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