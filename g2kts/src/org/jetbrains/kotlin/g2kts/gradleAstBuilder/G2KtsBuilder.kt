/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.gradleAstBuilder

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList

fun buildTree(psi: PsiElement): GNode {
    return when (psi) {
        is GroovyFileBase -> psi.toGradleAst()
        else -> error("Need groovy file $psi")
    }
}

fun GroovyFileBase.toGradleAst(): GProject {
    return GProject(this.children.mapNotNull { psiElement ->
        psiElement.toGradleAst().let {
            when (it) {
                is GStatement -> it
                is ConvertableToStatement -> it.toStatement()
                null, is GIdentifier -> null
                else -> unreachable()
            }
        }
    }, this)
}

fun GroovyPsiElement.toGradleAst(): GNode? {
    return when (this) {
        is GrCodeBlock -> toGradleAst()
        is GrNamedArgument -> GArgument(labelName, expression!!.toGradleAst(), this)
        is GrStatement -> toGradleAst()
        is GrExpression -> toGradleAst()
        // TODO По какой-то причине иногда в скрипте в рандомных местах появляется пустой параметр лист
        is GrParameterList -> if (this.isEmpty) {
            return null
        } else {
            unknownPsiElement(this)
        }
//    is GrTypeParameterList -> TODO(this::class.toString())
//    is GrTryCatchStatement -> TODO(this::class.toString())
//    is GrModifierList -> TODO(this::class.toString())
//    is GrAnnotationArgumentList -> TODO(this::class.toString())
//    is GrReferenceList -> TODO(this::class.toString())
//    is GrMembersDeclaration -> TODO(this::class.toString())
//    is GrEnumConstantList -> TODO(this::class.toString())
//    is GrParametersOwner -> TODO(this::class.toString())
//    is GrCaseLabel -> TODO(this::class.toString())
//    is GrCommandArgumentList -> TODO(this::class.toString())
//    is GrExpressionList -> TODO(this::class.toString())
//    is GrForClause -> TODO(this::class.toString())
////    is GrReferenceElement -> TODO(this::class.toString())
//    is GrStatementOwner -> TODO(this::class.toString())
//    is GrArgumentList -> TODO(this::class.toString())
//    is GrArrayDeclaration -> TODO(this::class.toString())
////    is GrStubElementBase -> TODO(this::class.toString())
//    is GrVariableDeclarationOwner -> TODO(this::class.toString())
//    is GrTopStatement -> TODO(this::class.toString())
//    is GrMember -> TODO(this::class.toString())
//    is GrArgumentLabel -> TODO(this::class.toString())
//    is GrTuple -> TODO(this::class.toString())
//    is GrDeclarationHolder -> TODO(this::class.toString())
//    is GrSwitchStatement -> TODO(this::class.toString())
//    is GrNamedElement -> TODO(this::class.toString())
//    is GrSynchronizedStatement -> TODO(this::class.toString())
//    is GrLiteralContainer -> TODO(this::class.toString())
//    is GroovyDocPsiElement -> TODO(this::class.toString())
//    is GrDocCommentOwner -> TODO(this::class.toString())
//    is GrAnnotationNameValuePair -> TODO(this::class.toString())
//    is GrParameterList -> TODO(this::class.toString())
//    is GrAnnotationMemberValue -> TODO(this::class.toString())
//    is GrTypeElement -> TODO(this::class.toString())
//    is GrTypeArgumentList -> TODO(this::class.toString())
//    is GrCaseSection -> TODO(this::class.toString())
//    is GrImportAlias -> TODO(this::class.toString())
//    is GrCatchClause -> TODO(this::class.toString())
//    is GrFinallyClause -> TODO(this::class.toString())
//    is GrStringInjection -> TODO(this::class.toString())
//    is GrSpreadArgument -> TODO(this::class.toString())
//    is GrControlStatement -> TODO(this::class.toString())
//    is GrControlFlowOwner -> TODO(this::class.toString())
//    is GrCondition -> TODO(this::class.toString())
//    is GrArrayInitializer -> TODO(this::class.toString())
//    is GrCall -> TODO(this::class.toString())
//    is GrTryResourceList -> TODO(this::class.toString())
//    else -> GIdentifier(text, this)
        else -> unknownPsiElement(this)
    }
}

fun GrCodeBlock.toGradleAst(): GBlock {
    val statements = children.mapNotNull {
        if (it.text == "{" || it.text == "}") return@mapNotNull null
        when (val t = it.toGradleAst()) {
            is ConvertableToStatement -> t.toStatement()
            is GStatement -> t
            else -> null
        }
    }
    return GBlock(statements, this)
}


fun GrString.toSimpleString(): String = allContentParts.joinToString(separator = "") { it.text }


fun GrArgumentList.toGradleAst(): GArgumentsList {
    return GArgumentsList(allArguments.map { arg ->
        when (arg) {
            is GrLiteral -> GArgument(null, arg.toGradleAst(), arg)
//            is GrNamedArgument -> GArgument(arg.labelName, arg.expression!!.toGradleAst())
            is GrNamedArgument -> arg.toGradleAst().cast()
            is GrExpression -> GArgument(null, arg.toGradleAst(), arg)
            else -> TODO(arg::class.toString())
        }
    }, this)
}


fun GrParameter.toGradleAst(): GExpression {
    TODO()
}

fun PsiElement.toGradleAst(): GNode? {
    return when {
        this is GroovyPsiElement -> this.toGradleAst()
        this is PsiWhiteSpace || toString() == "PsiElement(new line)" -> {
            val n = text.count { it == '\n' } - 1
            if (n > 0) GNewLine(n, this)
            else null
        }
        this is PsiComment -> GComment(this.text, startsLine = true, ensLine = true)
        toString() == "PsiElement(identifier)" -> GIdentifier(text, this)
        else -> unknownPsiElement(this)
    }
}