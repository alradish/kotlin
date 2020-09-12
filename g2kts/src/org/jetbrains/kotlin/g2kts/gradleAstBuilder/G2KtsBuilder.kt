/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.gradleAstBuilder

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWhiteSpace
import org.gradle.api.NamedDomainObjectCollection
import org.jetbrains.kotlin.g2kts.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.GrDoWhileStatement
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.GrLambdaExpression
import org.jetbrains.plugins.groovy.lang.psi.api.GrRangeExpression
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrSpreadArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrPropertySelection
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.GrTopStatement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.packaging.GrPackageDefinition
import org.jetbrains.plugins.groovy.lang.psi.impl.GrClassReferenceType
import java.math.BigDecimal

class G2KtsBuilder(val gradleContext: GradleBuildContext) {
    private val expressionConverter = ExpressionConverter()
    private val statementConverter = StatementConverter()

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
            is GrNamedArgument -> GArgument(labelName, with(expressionConverter) { expression!!.toGradleAst() }, this)
            is GrStatement -> with(statementConverter) { toGradleAst() }
            is GrExpression -> with(expressionConverter) { toGradleAst() }
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
                is GrLiteral -> GArgument(null, with(expressionConverter) { arg.toGradleAst() }, arg)
//            is GrNamedArgument -> GArgument(arg.labelName, arg.expression!!.toGradleAst())
                is GrNamedArgument -> arg.toGradleAst().cast()
                is GrExpression -> GArgument(null, with(expressionConverter) { arg.toGradleAst() }, arg)
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

    private inner class ExpressionConverter {

        fun GrExpression.toGradleAst(): GExpression = when (this) {
            is GrUnaryExpression -> toGradleAst()
            is GrOperatorExpression -> toGradleAst()
            is GrLiteral -> toGradleAst()
            is GrReferenceExpression -> toGradleAst()
            is GrListOrMap -> toGradleAst()
            is GrFunctionalExpression -> toGradleAst()
            is GrCallExpression -> toGradleAst()
//            is GrRangeExpression -> TODO(this::class.toString())
//            is GrInstanceOfExpression -> TODO(this::class.toString())
//            is GrSafeCastExpression -> TODO(this::class.toString())
//            is GrConditionalExpression -> TODO(this::class.toString())
//            is GrTypeCastExpression -> TODO(this::class.toString())
//            is GrBuiltinTypeClassExpression -> TODO(this::class.toString())
//            is GrPropertySelection -> TODO(this::class.toString())
//            is GrSpreadArgument -> TODO(this::class.toString())
//            is GrIndexProperty -> TODO(this::class.toString())
//            is GrTupleAssignmentExpression -> TODO(this::class.toString())
//            is GrParenthesizedExpression -> TODO(this::class.toString())
//            else -> unreachable()
            else -> unknownPsiElement(this)
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
            // TODO check for typeArgs
            return GSimpleMethodCall(
                obj?.toGradleAst(),
                method!!.toGradleAst() as GIdentifier,
                emptyList(),
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
    }

    private inner class StatementConverter {

        fun GrStatement.toGradleAst(): GStatement = when (this) {
            is GrTryCatchStatement -> toGradleAst().toStatement()
            is GrExpression -> with(expressionConverter) { toGradleAst().toStatement() }
            is GrBlockStatement -> block.toGradleAst()
            is GrWhileStatement -> toGradleAst().toStatement()
            is GrIfStatement -> toGradleAst().toStatement()
            is GrVariableDeclaration -> toGradleAst().toStatement()
            is GrLoopStatement -> toGradleAst()
//    is GrConstructorInvocation -> TODO(text)
//    is GrAssertStatement -> TODO(text)
//    is GrReturnStatement -> TODO(text)
//    is GrLabeledStatement -> TODO(text)
//    is GrThrowStatement -> TODO(text)
//    is GrSynchronizedStatement -> TODO(text)
//    is GrSwitchStatement -> TODO(text)
//    is GrApplicationStatement -> TODO(text)
//    is GrFlowInterruptingStatement -> TODO(text)
            else -> unknownPsiElement(this)
        }

        fun GrLoopStatement.toGradleAst() = when (this) {
            is GrWhileStatement -> toGradleAst().toStatement()
            is GrForStatement -> TODO(text)
            is GrDoWhileStatement -> TODO(text)
            else -> unreachable()
        }

        fun GrTryCatchStatement.toGradleAst(): GTryCatch {
            val body = tryBlock?.toGradleAst() as GBlock
            val catches = catchClauses.map {
                val parameter = it.parameter ?: error("")
                Catch(parameter.name, parameter.type.presentableText, it.body?.toGradleAst() as GBlock)
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
            val condition = with(expressionConverter) { condition?.toGradleAst() } ?: error("empty if condition")
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
            val condition = with(expressionConverter) { condition?.toGradleAst() } ?: error("miss while condition")
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
            val expr = with(expressionConverter) { variable.initializerGroovy?.toGradleAst() }
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
    }
}


