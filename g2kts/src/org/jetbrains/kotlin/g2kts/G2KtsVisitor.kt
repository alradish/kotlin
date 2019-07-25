/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import kastree.ast.Node
import org.gradle.tooling.model.GradleProject
import org.jetbrains.plugins.groovy.lang.groovydoc.psi.api.*
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.*
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrThrowsClause
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierList
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotation
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationArrayInitializer
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationNameValuePair
import org.jetbrains.plugins.groovy.lang.psi.api.statements.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrSpreadArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrOpenBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.clauses.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrRegex
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringInjection
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrPropertySelection
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAnnotationMethod
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrEnumConstant
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrEnumConstantList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.packaging.GrPackageDefinition
import org.jetbrains.plugins.groovy.lang.psi.api.types.*

class G2KtsVisitor : GroovyElementVisitor() {
    companion object {
        private val LOG = Logger.getInstance(this::class.java)
    }

    val simpleField: Int = 3
    val propertieWithGet: Int
        get() = 2

    var propertieWithSet: Int = 2
        set(a) {
            field = a - 2
        }
    var complexPropertie: Int = 1
        get() = field + 2
        set(a) {
            field = a - 3
        }

    init {
//        G2KtsVisitor::class.members.forEach {
//            println(
//                "${it.toString()}|name=${it.name}|parameters=${it.parameters}"
//            )
//        }
    }

    fun convert(element: GroovyPsiElement): Node {
        println("G2KtsVisitor.convert(element = [${element.text}])")
        accept(element)
        return pop()
    }

    fun accept(element: GroovyPsiElement) {
        println("G2KtsVisitor.accept(element = [${element.text}])")
        element.accept(this)
    }

    var result: Node? = null

    fun pop(): Node {
        println("G2KtsVisitor.pop()")
        val node = result!!
        result = null
        println("\treturn $node")
        return node
    }

    fun push(node: Node) {
        println("G2KtsVisitor.push(node = [${node}])")
        require(result == null)
        result = node
    }

    fun convertAll(elements: Iterable<PsiElement>): List<Node> {
        println("G2KtsVisitor.convertAll(elements = [${elements}])")
        return elements.map {
            when (it) {
                is GroovyPsiElement -> convert(it)
                else -> Node.Extra.BlankLines(it.text.count { c -> c == '\n' }) // GTODO БОЖЕ ЗА ЧТО
            }
        }
    }


    override fun visitElement(element: GroovyPsiElement) {
        println("G2KtsVisitor.visitElement(element = [${element.text}])")
    }

    override fun visitFile(file: GroovyFileBase) {
        println("G2KtsVisitor.visitFile(file = [${file.text}])")
        push(Node.Script(
            emptyList(),
            null,
            emptyList(),
            convertAll(file.children.asIterable()).map { it as Node.Expr }
        ))
    }

    override fun visitMethodCallExpression(methodCallExpression: GrMethodCallExpression) {
        println("G2KtsVisitor.visitMethodCallExpression(methodCallExpression = [${methodCallExpression.text}])")
        push(
            Node.Expr.Call(
                Node.Expr.Name(
                    (methodCallExpression.firstChild as? GrReferenceExpression)?.referenceName ?: error("Null method call name")
                ),
                emptyList(), // GTODO Здесь точно должно быть что-то типо Tasks or Copy
                convertAll(methodCallExpression.argumentList.allArguments.asIterable()).map { it as Node.ValueArg },
//            null
                Node.Expr.Call.TrailLambda(emptyList(), null, Node.Expr.Brace(emptyList(), null))
            )
        )

//        super.visitMethodCallExpression(methodCallExpression)
    }

    override fun visitApplicationStatement(applicationStatement: GrApplicationStatement) {
        println("G2KtsVisitor.visitApplicationStatement(applicationStatement = [${applicationStatement.text}])")
//        super.visitApplicationStatement(applicationStatement)
    }

    override fun visitThrowsClause(throwsClause: GrThrowsClause) {
        println("G2KtsVisitor.visitThrowsClause(throwsClause = [${throwsClause.text}])")
        super.visitThrowsClause(throwsClause)
    }

    override fun visitLiteralExpression(literal: GrLiteral) {
        println("G2KtsVisitor.visitLiteralExpression(literal = [${literal.text}])")
        super.visitLiteralExpression(literal)
    }

    override fun visitNamedArgument(argument: GrNamedArgument) {
        println("G2KtsVisitor.visitNamedArgument(argument = [${argument.text}])")
        super.visitNamedArgument(argument)
    }

    override fun visitCallExpression(callExpression: GrCallExpression) {
        println("G2KtsVisitor.visitCallExpression(callExpression = [${callExpression.text}])")
        super.visitCallExpression(callExpression)
    }

    override fun visitReferenceExpression(referenceExpression: GrReferenceExpression) {
        println("G2KtsVisitor.visitReferenceExpression(referenceExpression = [${referenceExpression.text}])")
        super.visitReferenceExpression(referenceExpression)
    }

    override fun visitUnaryExpression(expression: GrUnaryExpression) {
        println("G2KtsVisitor.visitUnaryExpression(expression = [${expression.text}])")
        super.visitUnaryExpression(expression)
    }

    override fun visitDocMethodParameterList(params: GrDocMethodParams) {
        println("G2KtsVisitor.visitDocMethodParameterList(params = [${params.text}])")
        super.visitDocMethodParameterList(params)
    }

    override fun visitBlockStatement(blockStatement: GrBlockStatement) {
        println("G2KtsVisitor.visitBlockStatement(blockStatement = [${blockStatement.text}])")
        super.visitBlockStatement(blockStatement)
    }

    override fun visitField(field: GrField) {
        println("G2KtsVisitor.visitField(field = [${field.text}])")
        super.visitField(field)
    }

    override fun visitExpressionList(expressionList: GrExpressionList) {
        println("G2KtsVisitor.visitExpressionList(expressionList = [${expressionList.text}])")
        super.visitExpressionList(expressionList)
    }

    override fun visitParenthesizedExpression(expression: GrParenthesizedExpression) {
        println("G2KtsVisitor.visitParenthesizedExpression(expression = [${expression.text}])")
        super.visitParenthesizedExpression(expression)
    }

    override fun visitImplementsClause(implementsClause: GrImplementsClause) {
        println("G2KtsVisitor.visitImplementsClause(implementsClause = [${implementsClause.text}])")
        super.visitImplementsClause(implementsClause)
    }

    override fun visitMethodCall(call: GrMethodCall) {
        println("G2KtsVisitor.visitMethodCall(call = [${call.text}])")
        super.visitMethodCall(call)
    }

    override fun visitExpressionLambdaBody(body: GrExpressionLambdaBody) {
        println("G2KtsVisitor.visitExpressionLambdaBody(body = [${body.text}])")
        super.visitExpressionLambdaBody(body)
    }

    override fun visitTryStatement(tryCatchStatement: GrTryCatchStatement) {
        println("G2KtsVisitor.visitTryStatement(tryCatchStatement = [${tryCatchStatement.text}])")
        super.visitTryStatement(tryCatchStatement)
    }

    override fun visitDocFieldReference(reference: GrDocFieldReference) {
        println("G2KtsVisitor.visitDocFieldReference(reference = [${reference.text}])")
        super.visitDocFieldReference(reference)
    }

    override fun visitAssertStatement(assertStatement: GrAssertStatement) {
        println("G2KtsVisitor.visitAssertStatement(assertStatement = [${assertStatement.text}])")
        super.visitAssertStatement(assertStatement)
    }

    override fun visitFinallyClause(catchClause: GrFinallyClause) {
        println("G2KtsVisitor.visitFinallyClause(catchClause = [${catchClause.text}])")
        super.visitFinallyClause(catchClause)
    }

    override fun visitTypeArgumentList(typeArgumentList: GrTypeArgumentList) {
        println("G2KtsVisitor.visitTypeArgumentList(typeArgumentList = [${typeArgumentList.text}])")
        super.visitTypeArgumentList(typeArgumentList)
    }

    override fun visitTypeElement(typeElement: GrTypeElement) {
        println("G2KtsVisitor.visitTypeElement(typeElement = [${typeElement.text}])")
        super.visitTypeElement(typeElement)
    }

    override fun visitForStatement(forStatement: GrForStatement) {
        println("G2KtsVisitor.visitForStatement(forStatement = [${forStatement.text}])")
        super.visitForStatement(forStatement)
    }

    override fun visitOpenBlock(block: GrOpenBlock) {
        println("G2KtsVisitor.visitOpenBlock(block = [${block.text}])")
        super.visitOpenBlock(block)
    }

    override fun visitModifierList(modifierList: GrModifierList) {
        println("G2KtsVisitor.visitModifierList(modifierList = [${modifierList.text}])")
        super.visitModifierList(modifierList)
    }

    override fun visitTypeParameterList(list: GrTypeParameterList) {
        println("G2KtsVisitor.visitTypeParameterList(list = [${list.text}])")
        super.visitTypeParameterList(list)
    }

    override fun visitElvisExpression(expression: GrElvisExpression) {
        println("G2KtsVisitor.visitElvisExpression(expression = [${expression.text}])")
        super.visitElvisExpression(expression)
    }

    override fun visitConstructorInvocation(invocation: GrConstructorInvocation) {
        println("G2KtsVisitor.visitConstructorInvocation(invocation = [${invocation.text}])")
        super.visitConstructorInvocation(invocation)
    }

    override fun visitDisjunctionTypeElement(disjunctionTypeElement: GrDisjunctionTypeElement) {
        println("G2KtsVisitor.visitDisjunctionTypeElement(disjunctionTypeElement = [${disjunctionTypeElement.text}])")
        super.visitDisjunctionTypeElement(disjunctionTypeElement)
    }

    override fun visitAnnotationArgumentList(annotationArgumentList: GrAnnotationArgumentList) {
        println("G2KtsVisitor.visitAnnotationArgumentList(annotationArgumentList = [${annotationArgumentList.text}])")
        super.visitAnnotationArgumentList(annotationArgumentList)
    }

    override fun visitSpreadArgument(spreadArgument: GrSpreadArgument) {
        println("G2KtsVisitor.visitSpreadArgument(spreadArgument = [${spreadArgument.text}])")
        super.visitSpreadArgument(spreadArgument)
    }

    override fun visitAnnotationTypeDefinition(annotationTypeDefinition: GrAnnotationTypeDefinition) {
        println("G2KtsVisitor.visitAnnotationTypeDefinition(annotationTypeDefinition = [${annotationTypeDefinition.text}])")
        super.visitAnnotationTypeDefinition(annotationTypeDefinition)
    }

    override fun visitEnumDefinition(enumDefinition: GrEnumTypeDefinition) {
        println("G2KtsVisitor.visitEnumDefinition(enumDefinition = [${enumDefinition.text}])")
        super.visitEnumDefinition(enumDefinition)
    }

    override fun visitSwitchStatement(switchStatement: GrSwitchStatement) {
        println("G2KtsVisitor.visitSwitchStatement(switchStatement = [${switchStatement.text}])")
        super.visitSwitchStatement(switchStatement)
    }

    override fun visitPropertySelection(expression: GrPropertySelection) {
        println("G2KtsVisitor.visitPropertySelection(expression = [${expression.text}])")
        super.visitPropertySelection(expression)
    }

    override fun visitRangeExpression(range: GrRangeExpression) {
        println("G2KtsVisitor.visitRangeExpression(range = [${range.text}])")
        super.visitRangeExpression(range)
    }

    override fun visitClassInitializer(initializer: GrClassInitializer) {
        println("G2KtsVisitor.visitClassInitializer(initializer = [${initializer.text}])")
        super.visitClassInitializer(initializer)
    }

    override fun visitCaseLabel(caseLabel: GrCaseLabel) {
        println("G2KtsVisitor.visitCaseLabel(caseLabel = [${caseLabel.text}])")
        super.visitCaseLabel(caseLabel)
    }

    override fun visitClassDefinition(classDefinition: GrClassDefinition) {
        println("G2KtsVisitor.visitClassDefinition(classDefinition = [${classDefinition.text}])")
        super.visitClassDefinition(classDefinition)
    }

    override fun visitAnnotation(annotation: GrAnnotation) {
        println("G2KtsVisitor.visitAnnotation(annotation = [${annotation.text}])")
        super.visitAnnotation(annotation)
    }

    override fun visitInstanceofExpression(expression: GrInstanceOfExpression) {
        println("G2KtsVisitor.visitInstanceofExpression(expression = [${expression.text}])")
        super.visitInstanceofExpression(expression)
    }

    override fun visitDocMethodParameter(parameter: GrDocMethodParameter) {
        println("G2KtsVisitor.visitDocMethodParameter(parameter = [${parameter.text}])")
        super.visitDocMethodParameter(parameter)
    }

    override fun visitAnnotationNameValuePair(nameValuePair: GrAnnotationNameValuePair) {
        println("G2KtsVisitor.visitAnnotationNameValuePair(nameValuePair = [${nameValuePair.text}])")
        super.visitAnnotationNameValuePair(nameValuePair)
    }

    override fun visitVariableDeclaration(variableDeclaration: GrVariableDeclaration) {
        println("G2KtsVisitor.visitVariableDeclaration(variableDeclaration = [${variableDeclaration.text}])")
        super.visitVariableDeclaration(variableDeclaration)
    }

    override fun visitAnnotationMethod(annotationMethod: GrAnnotationMethod) {
        println("G2KtsVisitor.visitAnnotationMethod(annotationMethod = [${annotationMethod.text}])")
        super.visitAnnotationMethod(annotationMethod)
    }

    override fun visitPackageDefinition(packageDefinition: GrPackageDefinition) {
        println("G2KtsVisitor.visitPackageDefinition(packageDefinition = [${packageDefinition.text}])")
        super.visitPackageDefinition(packageDefinition)
    }

    override fun visitBlockLambdaBody(body: GrBlockLambdaBody) {
        println("G2KtsVisitor.visitBlockLambdaBody(body = [${body.text}])")
        super.visitBlockLambdaBody(body)
    }

    override fun visitLambdaExpression(expression: GrLambdaExpression) {
        println("G2KtsVisitor.visitLambdaExpression(expression = [${expression.text}])")
        super.visitLambdaExpression(expression)
    }

    override fun visitFunctionalExpression(expression: GrFunctionalExpression) {
        println("G2KtsVisitor.visitFunctionalExpression(expression = [${expression.text}])")
        super.visitFunctionalExpression(expression)
    }

    override fun visitStatement(statement: GrStatement) {
        println("G2KtsVisitor.visitStatement(statement = [${statement.text}])")
        super.visitStatement(statement)
    }

    override fun visitReturnStatement(returnStatement: GrReturnStatement) {
        println("G2KtsVisitor.visitReturnStatement(returnStatement = [${returnStatement.text}])")
        super.visitReturnStatement(returnStatement)
    }

    override fun visitConditionalExpression(expression: GrConditionalExpression) {
        println("G2KtsVisitor.visitConditionalExpression(expression = [${expression.text}])")
        super.visitConditionalExpression(expression)
    }

    override fun visitGStringExpression(gstring: GrString) {
        println("G2KtsVisitor.visitGStringExpression(gstring = [${gstring.text}])")
        super.visitGStringExpression(gstring)
    }

    override fun visitDoWhileStatement(statement: GrDoWhileStatement) {
        println("G2KtsVisitor.visitDoWhileStatement(statement = [${statement.text}])")
        super.visitDoWhileStatement(statement)
    }

    override fun visitCaseSection(caseSection: GrCaseSection) {
        println("G2KtsVisitor.visitCaseSection(caseSection = [${caseSection.text}])")
        super.visitCaseSection(caseSection)
    }

    override fun visitTraditionalForClause(forClause: GrTraditionalForClause) {
        println("G2KtsVisitor.visitTraditionalForClause(forClause = [${forClause.text}])")
        super.visitTraditionalForClause(forClause)
    }

    override fun visitNewExpression(newExpression: GrNewExpression) {
        println("G2KtsVisitor.visitNewExpression(newExpression = [${newExpression.text}])")
        super.visitNewExpression(newExpression)
    }

    override fun visitForInClause(forInClause: GrForInClause) {
        println("G2KtsVisitor.visitForInClause(forInClause = [${forInClause.text}])")
        super.visitForInClause(forInClause)
    }

    override fun visitSynchronizedStatement(synchronizedStatement: GrSynchronizedStatement) {
        println("G2KtsVisitor.visitSynchronizedStatement(synchronizedStatement = [${synchronizedStatement.text}])")
        super.visitSynchronizedStatement(synchronizedStatement)
    }

    override fun visitArrayTypeElement(typeElement: GrArrayTypeElement) {
        println("G2KtsVisitor.visitArrayTypeElement(typeElement = [${typeElement.text}])")
        super.visitArrayTypeElement(typeElement)
    }

    override fun visitMethod(method: GrMethod) {
        println("G2KtsVisitor.visitMethod(method = [${method.text}])")
        super.visitMethod(method)
    }

    override fun visitContinueStatement(continueStatement: GrContinueStatement) {
        println("G2KtsVisitor.visitContinueStatement(continueStatement = [${continueStatement.text}])")
        super.visitContinueStatement(continueStatement)
    }

    override fun visitForClause(forClause: GrForClause) {
        println("G2KtsVisitor.visitForClause(forClause = [${forClause.text}])")
        super.visitForClause(forClause)
    }

    override fun visitArrayInitializer(arrayInitializer: GrArrayInitializer) {
        println("G2KtsVisitor.visitArrayInitializer(arrayInitializer = [${arrayInitializer.text}])")
        super.visitArrayInitializer(arrayInitializer)
    }

    override fun visitTuple(tuple: GrTuple) {
        println("G2KtsVisitor.visitTuple(tuple = [${tuple.text}])")
        super.visitTuple(tuple)
    }

    override fun visitTraitDefinition(traitTypeDefinition: GrTraitTypeDefinition) {
        println("G2KtsVisitor.visitTraitDefinition(traitTypeDefinition = [${traitTypeDefinition.text}])")
        super.visitTraitDefinition(traitTypeDefinition)
    }

    override fun visitParameterList(parameterList: GrParameterList) {
        println("G2KtsVisitor.visitParameterList(parameterList = [${parameterList.text}])")
        super.visitParameterList(parameterList)
    }

    override fun visitListOrMap(listOrMap: GrListOrMap) {
        println("G2KtsVisitor.visitListOrMap(listOrMap = [${listOrMap.text}])")
        super.visitListOrMap(listOrMap)
    }

    override fun visitIfStatement(ifStatement: GrIfStatement) {
        println("G2KtsVisitor.visitIfStatement(ifStatement = [${ifStatement.text}])")
        super.visitIfStatement(ifStatement)
    }

    override fun visitDocComment(comment: GrDocComment) {
        println("G2KtsVisitor.visitDocComment(comment = [${comment.text}])")
        super.visitDocComment(comment)
    }

    override fun visitRegexExpression(expression: GrRegex) {
        println("G2KtsVisitor.visitRegexExpression(expression = [${expression.text}])")
        super.visitRegexExpression(expression)
    }

    override fun visitVariable(variable: GrVariable) {
        println("G2KtsVisitor.visitVariable(variable = [${variable.text}])")
        super.visitVariable(variable)
    }

    override fun visitAnonymousClassDefinition(anonymousClassDefinition: GrAnonymousClassDefinition) {
        println("G2KtsVisitor.visitAnonymousClassDefinition(anonymousClassDefinition = [${anonymousClassDefinition.text}])")
        super.visitAnonymousClassDefinition(anonymousClassDefinition)
    }

    override fun visitFlowInterruptStatement(statement: GrFlowInterruptingStatement) {
        println("G2KtsVisitor.visitFlowInterruptStatement(statement = [${statement.text}])")
        super.visitFlowInterruptStatement(statement)
    }

    override fun visitIndexProperty(expression: GrIndexProperty) {
        println("G2KtsVisitor.visitIndexProperty(expression = [${expression.text}])")
        super.visitIndexProperty(expression)
    }

    override fun visitCatchClause(catchClause: GrCatchClause) {
        println("G2KtsVisitor.visitCatchClause(catchClause = [${catchClause.text}])")
        super.visitCatchClause(catchClause)
    }

    override fun visitThrowStatement(throwStatement: GrThrowStatement) {
        println("G2KtsVisitor.visitThrowStatement(throwStatement = [${throwStatement.text}])")
        super.visitThrowStatement(throwStatement)
    }

    override fun visitTypeDefinition(typeDefinition: GrTypeDefinition) {
        println("G2KtsVisitor.visitTypeDefinition(typeDefinition = [${typeDefinition.text}])")
        super.visitTypeDefinition(typeDefinition)
    }

    override fun visitExtendsClause(extendsClause: GrExtendsClause) {
        println("G2KtsVisitor.visitExtendsClause(extendsClause = [${extendsClause.text}])")
        super.visitExtendsClause(extendsClause)
    }

    override fun visitCastExpression(typeCastExpression: GrTypeCastExpression) {
        println("G2KtsVisitor.visitCastExpression(typeCastExpression = [${typeCastExpression.text}])")
        super.visitCastExpression(typeCastExpression)
    }

    override fun visitLabeledStatement(labeledStatement: GrLabeledStatement) {
        println("G2KtsVisitor.visitLabeledStatement(labeledStatement = [${labeledStatement.text}])")
        super.visitLabeledStatement(labeledStatement)
    }

    override fun visitBinaryExpression(expression: GrBinaryExpression) {
        println("G2KtsVisitor.visitBinaryExpression(expression = [${expression.text}])")
        super.visitBinaryExpression(expression)
    }

    override fun visitCommandArguments(argumentList: GrCommandArgumentList) {
        println("G2KtsVisitor.visitCommandArguments(argumentList = [${argumentList.text}])")
        super.visitCommandArguments(argumentList)
    }

    override fun visitDocMethodReference(reference: GrDocMethodReference) {
        println("G2KtsVisitor.visitDocMethodReference(reference = [${reference.text}])")
        super.visitDocMethodReference(reference)
    }

    override fun visitBreakStatement(breakStatement: GrBreakStatement) {
        println("G2KtsVisitor.visitBreakStatement(breakStatement = [${breakStatement.text}])")
        super.visitBreakStatement(breakStatement)
    }

    override fun visitImportStatement(importStatement: GrImportStatement) {
        println("G2KtsVisitor.visitImportStatement(importStatement = [${importStatement.text}])")
        super.visitImportStatement(importStatement)
    }

    override fun visitClosure(closure: GrClosableBlock) {
        println("G2KtsVisitor.visitClosure(closure = [${closure.text}])")
        super.visitClosure(closure)
    }

    override fun visitSafeCastExpression(typeCastExpression: GrSafeCastExpression) {
        println("G2KtsVisitor.visitSafeCastExpression(typeCastExpression = [${typeCastExpression.text}])")
        super.visitSafeCastExpression(typeCastExpression)
    }

    override fun visitTryResourceList(resourceList: GrTryResourceList) {
        println("G2KtsVisitor.visitTryResourceList(resourceList = [${resourceList.text}])")
        super.visitTryResourceList(resourceList)
    }

    override fun visitEnumConstants(enumConstantsSection: GrEnumConstantList) {
        println("G2KtsVisitor.visitEnumConstants(enumConstantsSection = [${enumConstantsSection.text}])")
        super.visitEnumConstants(enumConstantsSection)
    }

    override fun visitParameter(parameter: GrParameter) {
        println("G2KtsVisitor.visitParameter(parameter = [${parameter.text}])")
        super.visitParameter(parameter)
    }

    override fun visitLambdaBody(body: GrLambdaBody) {
        println("G2KtsVisitor.visitLambdaBody(body = [${body.text}])")
        super.visitLambdaBody(body)
    }

    override fun visitCodeReferenceElement(refElement: GrCodeReferenceElement) {
        println("G2KtsVisitor.visitCodeReferenceElement(refElement = [${refElement.text}])")
        super.visitCodeReferenceElement(refElement)
    }

    override fun visitDocTag(docTag: GrDocTag) {
        println("G2KtsVisitor.visitDocTag(docTag = [${docTag.text}])")
        super.visitDocTag(docTag)
    }

    override fun visitArrayDeclaration(arrayDeclaration: GrArrayDeclaration) {
        println("G2KtsVisitor.visitArrayDeclaration(arrayDeclaration = [${arrayDeclaration.text}])")
        super.visitArrayDeclaration(arrayDeclaration)
    }

    override fun visitTypeParameter(typeParameter: GrTypeParameter) {
        println("G2KtsVisitor.visitTypeParameter(typeParameter = [${typeParameter.text}])")
        super.visitTypeParameter(typeParameter)
    }

    override fun visitBuiltinTypeElement(typeElement: GrBuiltInTypeElement) {
        println("G2KtsVisitor.visitBuiltinTypeElement(typeElement = [${typeElement.text}])")
        super.visitBuiltinTypeElement(typeElement)
    }

    override fun visitArgumentList(list: GrArgumentList) {
        println("G2KtsVisitor.visitArgumentList(list = [${list.text}])")
        super.visitArgumentList(list)
    }

    override fun visitWildcardTypeArgument(wildcardTypeArgument: GrWildcardTypeArgument) {
        println("G2KtsVisitor.visitWildcardTypeArgument(wildcardTypeArgument = [${wildcardTypeArgument.text}])")
        super.visitWildcardTypeArgument(wildcardTypeArgument)
    }

    override fun visitTypeDefinitionBody(typeDefinitionBody: GrTypeDefinitionBody) {
        println("G2KtsVisitor.visitTypeDefinitionBody(typeDefinitionBody = [${typeDefinitionBody.text}])")
        super.visitTypeDefinitionBody(typeDefinitionBody)
    }

    override fun visitAnnotationArrayInitializer(arrayInitializer: GrAnnotationArrayInitializer) {
        println("G2KtsVisitor.visitAnnotationArrayInitializer(arrayInitializer = [${arrayInitializer.text}])")
        super.visitAnnotationArrayInitializer(arrayInitializer)
    }

    override fun visitGStringInjection(injection: GrStringInjection) {
        println("G2KtsVisitor.visitGStringInjection(injection = [${injection.text}])")
        super.visitGStringInjection(injection)
    }

    override fun visitTupleAssignmentExpression(expression: GrTupleAssignmentExpression) {
        println("G2KtsVisitor.visitTupleAssignmentExpression(expression = [${expression.text}])")
        super.visitTupleAssignmentExpression(expression)
    }

    override fun visitExpression(expression: GrExpression) {
        println("G2KtsVisitor.visitExpression(expression = [${expression.text}])")
        super.visitExpression(expression)
    }

    override fun visitClassTypeElement(typeElement: GrClassTypeElement) {
        println("G2KtsVisitor.visitClassTypeElement(typeElement = [${typeElement.text}])")
        super.visitClassTypeElement(typeElement)
    }

    override fun visitWhileStatement(whileStatement: GrWhileStatement) {
        println("G2KtsVisitor.visitWhileStatement(whileStatement = [${whileStatement.text}])")
        super.visitWhileStatement(whileStatement)
    }

    override fun visitBuiltinTypeClassExpression(expression: GrBuiltinTypeClassExpression) {
        println("G2KtsVisitor.visitBuiltinTypeClassExpression(expression = [${expression.text}])")
        super.visitBuiltinTypeClassExpression(expression)
    }

    override fun visitArgumentLabel(argumentLabel: GrArgumentLabel) {
        println("G2KtsVisitor.visitArgumentLabel(argumentLabel = [${argumentLabel.text}])")
        super.visitArgumentLabel(argumentLabel)
    }

    override fun visitEnumConstant(enumConstant: GrEnumConstant) {
        println("G2KtsVisitor.visitEnumConstant(enumConstant = [${enumConstant.text}])")
        super.visitEnumConstant(enumConstant)
    }

    override fun visitAssignmentExpression(expression: GrAssignmentExpression) {
        println("G2KtsVisitor.visitAssignmentExpression(expression = [${expression.text}])")
        super.visitAssignmentExpression(expression)
    }

    override fun visitEnumDefinitionBody(enumDefinitionBody: GrEnumDefinitionBody) {
        println("G2KtsVisitor.visitEnumDefinitionBody(enumDefinitionBody = [${enumDefinitionBody.text}])")
        super.visitEnumDefinitionBody(enumDefinitionBody)
    }

    override fun visitInExpression(expression: GrInExpression) {
        println("G2KtsVisitor.visitInExpression(expression = [${expression.text}])")
        super.visitInExpression(expression)
    }

    override fun visitInterfaceDefinition(interfaceDefinition: GrInterfaceDefinition) {
        println("G2KtsVisitor.visitInterfaceDefinition(interfaceDefinition = [${interfaceDefinition.text}])")
        super.visitInterfaceDefinition(interfaceDefinition)
    }
}