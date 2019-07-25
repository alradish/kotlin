/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import kastree.ast.Node
import kastree.ast.Writer
import org.gradle.api.Task
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.GrRangeExpression
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrSpreadArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrPropertySelection
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.GrExpressionImpl
import kotlin.reflect.KClass

@Suppress("MemberVisibilityCanBePrivate")
class G2KtsConverterWithoutVisitor {
    private fun cantConvert(to: KClass<out Any>, what: Any): Nothing =
        "Can't convert $what to ${to.qualifiedName}".let {
            LOG.error(it)
            error(it)
        }


    fun convertFile(file: GroovyFile) =
        Node.File(
            emptyList(),
            null, emptyList(),
            file.children.mapNotNull(::convertToDecl)
        )

    fun convertToDecl(psi: PsiElement): Node.Decl? = when (psi) {
        is GrMethodCall -> convertMethodCall(psi).let { it as? Node.Decl ?: throw FailCastException(it, "Node.Decl") }
        is GrCallExpression -> convertCallExpression(psi)
        is GrAssignmentExpression -> convertAssignmentExpression(psi)
        else -> when {
            psi.text == "\n" -> null
            else -> throw BadPsiElementException(psi)
        }
    }.also {
        println(
            "G2KtsConverterWithoutVisitor.convertToDecl([$psi|\n${psi.text}])\nres=\n${Writer.write(
                it ?: error("null")
            )}"
        )
    }

    private fun convertAssignmentExpression(psi: GrAssignmentExpression): Node.Decl {
        TODO()
    }

    private fun convertCallExpression(psi: GrCallExpression): Node.Decl {
        TODO()
    }

    private fun convertClosableBlock(psi: GrClosableBlock?): List<Node.Stmt> {
        return emptyList()
        return psi?.children?.map { convertToStatement(it) } ?: emptyList()
    }

    private fun convertToStatement(psi: PsiElement?): Node.Stmt {
        TODO("convert to statement")
    }

    fun convertMethodCall(psi: GrMethodCall): Node {
//        return when (val type = psi.invokedExpression.type?.canonicalText) {
//        Task::class.qualifiedName -> convertToTask(psi)

        return when (val name = psi.rawName) {
            "task" -> convertToTask(psi)
            else -> convertToFuncCall(psi)
        }
    }

    fun convertToFuncCall(psi: GrMethodCall): Node.Expr.Call {
        return Node.Expr.Call(
            Node.Expr.Name(psi.rawName ?: throw MissingMethodCallName(psi)),
            emptyList(),
            convertArgumentList(psi.argumentList),
            Node.Expr.Call.TrailLambda(
                emptyList(),
                null,
                Node.Expr.Brace(
                    emptyList(), // GTODO
                    Node.Block(
                        convertClosableBlock(psi.closureArguments.first())
                    )
                )
            )
        )
    }

    private fun convertArgumentList(argumentList: GrArgumentList): List<Node.ValueArg> {
        println("G2KtsConverterWithoutVisitor.convertArgumentList(argumentList = [${argumentList}])")
        return argumentList.allArguments.map { arg ->
            when (arg) {
                is GrNamedArgument -> Node.ValueArg(
                    arg.labelName,
                    false,
                    convertExpression(arg.expression ?: error(""))
                )

                else -> error("DA BLIN")
            }
        }

    }

    private fun convertExpression(expression: GrExpression): Node.Expr {
        return when (expression) {
            is GrListOrMap -> Node.Expr.Call(
                Node.Expr.Name(
                    if (expression.isMap) "mapOf" else "listOf"
                ),
                emptyList(),
                expression.initializers.map { Node.ValueArg(null, false, convertExpression(it)) },
                null
            )
            is GrReferenceExpression -> Node.Expr.Name(
                expression.text
            )
            is GrRangeExpression -> TODO()
            is GrInstanceOfExpression -> TODO()
            is GrOperatorExpression -> TODO()
            is GrUnaryExpression -> TODO()
            is GrLiteral -> TODO()
            is GrSafeCastExpression -> TODO()
            is GrConditionalExpression -> TODO()
            is GrTypeCastExpression -> TODO()
            is GrBuiltinTypeClassExpression -> TODO()
            is GrPropertySelection -> TODO()
            is GrIndexProperty -> TODO()
            is GrSpreadArgument -> TODO()
            is GrFunctionalExpression -> TODO()
            is GrTupleAssignmentExpression -> TODO()
            is GrParenthesizedExpression -> TODO()
            is GrCallExpression -> TODO()
            else -> error("")
        }
    }

    private fun convertToTask(psi: GrMethodCall): Node.Decl {
        val task = when (val it = psi.argumentList.allArguments.first()) {
            is GrMethodCall -> convertToFuncCall(it)
            else -> error("convert task")
        }

        return Node.Decl.Property(
            emptyList(), // GTODO
            true,
            emptyList(), // GTODO
            null,
            listOf(Node.Decl.Property.Var(task.name ?: error("null task name"), null)),
            emptyList(),
            true,
            Node.Expr.BinaryOp(
                TASKS,
                Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.DOT),
                Node.Expr.Call(
                    Node.Expr.Name("creating"),
                    emptyList(),
                    emptyList(),
                    task.lambda
                )
            ),
            null
        )
    }


    fun convert(psi: PsiElement): Node = when (psi) {
        is GroovyPsiElement -> when (psi) {
            is GroovyFile -> convertFile(psi)
            else -> cantConvert(Node::class, psi)
        }
        else -> cantConvert(Node::class, psi)
    }

    companion object {
        val LOG = Logger.getInstance(this::class.java)
        val TASKS = Node.Expr.Name("tasks")
    }
}