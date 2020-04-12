/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.gradleAstBuilder

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.plugins.groovy.lang.groovydoc.psi.api.GrDocCommentOwner
import org.jetbrains.plugins.groovy.lang.groovydoc.psi.api.GroovyDocPsiElement
import org.jetbrains.plugins.groovy.lang.psi.GrControlFlowOwner
import org.jetbrains.plugins.groovy.lang.psi.GrNamedElement
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.GrArrayInitializer
import org.jetbrains.plugins.groovy.lang.psi.api.GrExpressionList
import org.jetbrains.plugins.groovy.lang.psi.api.GrImportAlias
import org.jetbrains.plugins.groovy.lang.psi.api.GrTryResourceList
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrCondition
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierList
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationMemberValue
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationNameValuePair
import org.jetbrains.plugins.groovy.lang.psi.api.formatter.GrControlStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrSpreadArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.clauses.GrCaseLabel
import org.jetbrains.plugins.groovy.lang.psi.api.statements.clauses.GrCaseSection
import org.jetbrains.plugins.groovy.lang.psi.api.statements.clauses.GrForClause
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteralContainer
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringInjection
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrReferenceList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrEnumConstantList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMember
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMembersDeclaration
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.GrTopStatement
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeElement
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeParameterList
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrDeclarationHolder
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrVariableDeclarationOwner

fun GroovyFileBase.toGradleAst(): GProject {

    return GProject(topStatements.map { it.toGradleAst() }, this)
}

fun GroovyPsiElement.toGradleAst(): GNode = when (this) {
    is GrCodeBlock -> toGradleAst()
    is GrTypeParameterList -> TODO(this::class.toString())
    is GrTryCatchStatement -> TODO(this::class.toString())
    is GrModifierList -> TODO(this::class.toString())
    is GrAnnotationArgumentList -> TODO(this::class.toString())
    is GrReferenceList -> TODO(this::class.toString())
    is GrMembersDeclaration -> TODO(this::class.toString())
    is GrEnumConstantList -> TODO(this::class.toString())
    is GrParametersOwner -> TODO(this::class.toString())
    is GrCaseLabel -> TODO(this::class.toString())
    is GrCommandArgumentList -> TODO(this::class.toString())
    is GrExpressionList -> TODO(this::class.toString())
    is GrForClause -> TODO(this::class.toString())
//    is GrReferenceElement -> TODO(this::class.toString())
    is GrStatementOwner -> TODO(this::class.toString())
    is GrArgumentList -> TODO(this::class.toString())
    is GrArrayDeclaration -> TODO(this::class.toString())
//    is GrStubElementBase -> TODO(this::class.toString())
    is GrVariableDeclarationOwner -> TODO(this::class.toString())
    is GrTopStatement -> TODO(this::class.toString())
    is GrMember -> TODO(this::class.toString())
    is GrArgumentLabel -> TODO(this::class.toString())
    is GrTuple -> TODO(this::class.toString())
    is GrDeclarationHolder -> TODO(this::class.toString())
    is GrSwitchStatement -> TODO(this::class.toString())
    is GrNamedElement -> TODO(this::class.toString())
    is GrSynchronizedStatement -> TODO(this::class.toString())
    is GrLiteralContainer -> TODO(this::class.toString())
    is GroovyDocPsiElement -> TODO(this::class.toString())
    is GrDocCommentOwner -> TODO(this::class.toString())
    is GrAnnotationNameValuePair -> TODO(this::class.toString())
    is GrParameterList -> TODO(this::class.toString())
    is GrAnnotationMemberValue -> TODO(this::class.toString())
    is GrTypeElement -> TODO(this::class.toString())
    is GrTypeArgumentList -> TODO(this::class.toString())
    is GrCaseSection -> TODO(this::class.toString())
    is GrImportAlias -> TODO(this::class.toString())
    is GrCatchClause -> TODO(this::class.toString())
    is GrNamedArgument -> GArgument(labelName, expression!!.toGradleAst(),this)
    is GrFinallyClause -> TODO(this::class.toString())
    is GrStringInjection -> TODO(this::class.toString())
    is GrSpreadArgument -> TODO(this::class.toString())
    is GrControlStatement -> TODO(this::class.toString())
    is GrControlFlowOwner -> TODO(this::class.toString())
    is GrCondition -> TODO(this::class.toString())
    is GrArrayInitializer -> TODO(this::class.toString())
    is GrCall -> TODO(this::class.toString())
    is GrTryResourceList -> TODO(this::class.toString())
    else -> GIdentifier(text, this)
}

fun GrCodeBlock.toGradleAst(): GBlock {
    val statements = children.mapNotNull {
        if (it.text == "{" || it.text == "}") return@mapNotNull null
        val t = it.toGradleAst()
        when (t) {
            is GExpression -> t.toStatement()
            is GDeclaration -> t.toStatement()
            is GStatement -> t
            else -> null
        }
    }
    return GBlock(
        statements,
        this
    )
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


fun buildTree(psi: PsiElement): GNode {
    val project = when (psi) {
        is GroovyFileBase -> psi.toGradleAst()
        else -> TODO()
    }
    return convertApplyToPluginsBlock(project)
}

fun convertApplyToPluginsBlock(project: GProject): GProject {
    return project
//    val pluginsId = mutableSetOf<String>()
//    val other = mutableListOf<GStatement>()
//    for (statement in project.statements) {
//        if (statement.isApplyPlugin()) {
//            val call = statement.cast<GStatement.GExpr>().expr.cast<GMethodCall>()
//            val expr = call.arguments.args.first().expr.cast<GString>()
//            pluginsId.add(expr.str)
//        } else {
//            other.add(statement.copy() as GStatement)
//        }
//    }
//
//    val newStatements = if (pluginsId.isNotEmpty()) {
//        val first = other.firstOrNull().safeAs<GStatement.GExpr>()?.expr
//        val plugins = mutableListOf<GStatement>()
//        if (first is GConfigurationBlock && first.method == GIdentifier("plugins")) {
//            val withoutId = first.configuration.statements.statements.mapNotNull {
//                if (it.safeAs<GStatement.GExpr>()?.expr?.safeAs<GSimpleMethodCall>()?.method?.safeAs<GIdentifier>()?.name == "id") {
//                    pluginsId.add(it.cast<GStatement.GExpr>().expr.cast<GSimpleMethodCall>().arguments.args.first().expr.cast<GString>().str)
//                    null
//                } else {
//                    it.copy() as GStatement
//                }
//            }
//            plugins.addAll(withoutId + pluginsId.map {
//                val arg = GArgument(null, GString(it))
//                GSimpleMethodCall(
//                    null,
//                    GIdentifier("id"),
//                    GArgumentsList(
//                        listOf(
//                            arg
//                        )
//                    )
//                ).toStatement()
//            })
//            other.removeAt(0)
//        }
//        other.apply {
//            val configuration = GClosure(
//                emptyList(),
//                GBlock(plugins)
//            )
//            add(
//                0,
//                GConfigurationBlock(
//                    null,
//                    GIdentifier("plugins"),
//                    GArgumentsList(emptyList()),
//                    configuration
//                ).toStatement()
//            )
//        }
//    } else
//        other
//
//    return GProject(newStatements)
}

fun PsiElement.toGradleAst(): GNode? {
    return when  {
        this is GrStatement -> toGradleAst()
        this is GrExpression -> toGradleAst()
        this is PsiWhiteSpace -> null // TODO обработать пробелы и новые строки
        toString() == "PsiElement(new line)" -> null // TODO
        else -> GIdentifier(text, this)
    }
}