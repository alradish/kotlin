/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

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
    override fun visitThrowsClause(throwsClause: GrThrowsClause) {
        super.visitThrowsClause(throwsClause)
    }

    override fun visitLiteralExpression(literal: GrLiteral) {
        super.visitLiteralExpression(literal)
    }

    override fun visitFile(file: GroovyFileBase) {
        super.visitFile(file)
    }

    override fun visitNamedArgument(argument: GrNamedArgument) {
        super.visitNamedArgument(argument)
    }

    override fun visitCallExpression(callExpression: GrCallExpression) {
        super.visitCallExpression(callExpression)
    }

    override fun visitReferenceExpression(referenceExpression: GrReferenceExpression) {
        super.visitReferenceExpression(referenceExpression)
    }

    override fun visitUnaryExpression(expression: GrUnaryExpression) {
        super.visitUnaryExpression(expression)
    }

    override fun visitDocMethodParameterList(params: GrDocMethodParams) {
        super.visitDocMethodParameterList(params)
    }

    override fun visitBlockStatement(blockStatement: GrBlockStatement) {
        super.visitBlockStatement(blockStatement)
    }

    override fun visitField(field: GrField) {
        super.visitField(field)
    }

    override fun visitExpressionList(expressionList: GrExpressionList) {
        super.visitExpressionList(expressionList)
    }

    override fun visitParenthesizedExpression(expression: GrParenthesizedExpression) {
        super.visitParenthesizedExpression(expression)
    }

    override fun visitImplementsClause(implementsClause: GrImplementsClause) {
        super.visitImplementsClause(implementsClause)
    }

    override fun visitMethodCall(call: GrMethodCall) {
        super.visitMethodCall(call)
    }

    override fun visitExpressionLambdaBody(body: GrExpressionLambdaBody) {
        super.visitExpressionLambdaBody(body)
    }

    override fun visitTryStatement(tryCatchStatement: GrTryCatchStatement) {
        super.visitTryStatement(tryCatchStatement)
    }

    override fun visitDocFieldReference(reference: GrDocFieldReference) {
        super.visitDocFieldReference(reference)
    }

    override fun visitAssertStatement(assertStatement: GrAssertStatement) {
        super.visitAssertStatement(assertStatement)
    }

    override fun visitFinallyClause(catchClause: GrFinallyClause) {
        super.visitFinallyClause(catchClause)
    }

    override fun visitTypeArgumentList(typeArgumentList: GrTypeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList)
    }

    override fun visitTypeElement(typeElement: GrTypeElement) {
        super.visitTypeElement(typeElement)
    }

    override fun visitForStatement(forStatement: GrForStatement) {
        super.visitForStatement(forStatement)
    }

    override fun visitOpenBlock(block: GrOpenBlock) {
        super.visitOpenBlock(block)
    }

    override fun visitModifierList(modifierList: GrModifierList) {
        super.visitModifierList(modifierList)
    }

    override fun visitTypeParameterList(list: GrTypeParameterList) {
        super.visitTypeParameterList(list)
    }

    override fun visitElvisExpression(expression: GrElvisExpression) {
        super.visitElvisExpression(expression)
    }

    override fun visitConstructorInvocation(invocation: GrConstructorInvocation) {
        super.visitConstructorInvocation(invocation)
    }

    override fun visitDisjunctionTypeElement(disjunctionTypeElement: GrDisjunctionTypeElement) {
        super.visitDisjunctionTypeElement(disjunctionTypeElement)
    }

    override fun visitAnnotationArgumentList(annotationArgumentList: GrAnnotationArgumentList) {
        super.visitAnnotationArgumentList(annotationArgumentList)
    }

    override fun visitSpreadArgument(spreadArgument: GrSpreadArgument) {
        super.visitSpreadArgument(spreadArgument)
    }

    override fun visitAnnotationTypeDefinition(annotationTypeDefinition: GrAnnotationTypeDefinition) {
        super.visitAnnotationTypeDefinition(annotationTypeDefinition)
    }

    override fun visitEnumDefinition(enumDefinition: GrEnumTypeDefinition) {
        super.visitEnumDefinition(enumDefinition)
    }

    override fun visitSwitchStatement(switchStatement: GrSwitchStatement) {
        super.visitSwitchStatement(switchStatement)
    }

    override fun visitPropertySelection(expression: GrPropertySelection) {
        super.visitPropertySelection(expression)
    }

    override fun visitRangeExpression(range: GrRangeExpression) {
        super.visitRangeExpression(range)
    }

    override fun visitClassInitializer(initializer: GrClassInitializer) {
        super.visitClassInitializer(initializer)
    }

    override fun visitCaseLabel(caseLabel: GrCaseLabel) {
        super.visitCaseLabel(caseLabel)
    }

    override fun visitApplicationStatement(applicationStatement: GrApplicationStatement) {
        super.visitApplicationStatement(applicationStatement)
    }

    override fun visitClassDefinition(classDefinition: GrClassDefinition) {
        super.visitClassDefinition(classDefinition)
    }

    override fun visitAnnotation(annotation: GrAnnotation) {
        super.visitAnnotation(annotation)
    }

    override fun visitInstanceofExpression(expression: GrInstanceOfExpression) {
        super.visitInstanceofExpression(expression)
    }

    override fun visitMethodCallExpression(methodCallExpression: GrMethodCallExpression) {
        super.visitMethodCallExpression(methodCallExpression)
    }

    override fun visitDocMethodParameter(parameter: GrDocMethodParameter) {
        super.visitDocMethodParameter(parameter)
    }

    override fun visitAnnotationNameValuePair(nameValuePair: GrAnnotationNameValuePair) {
        super.visitAnnotationNameValuePair(nameValuePair)
    }

    override fun visitVariableDeclaration(variableDeclaration: GrVariableDeclaration) {
        super.visitVariableDeclaration(variableDeclaration)
    }

    override fun visitAnnotationMethod(annotationMethod: GrAnnotationMethod) {
        super.visitAnnotationMethod(annotationMethod)
    }

    override fun visitPackageDefinition(packageDefinition: GrPackageDefinition) {
        super.visitPackageDefinition(packageDefinition)
    }

    override fun visitBlockLambdaBody(body: GrBlockLambdaBody) {
        super.visitBlockLambdaBody(body)
    }

    override fun visitLambdaExpression(expression: GrLambdaExpression) {
        super.visitLambdaExpression(expression)
    }

    override fun visitFunctionalExpression(expression: GrFunctionalExpression) {
        super.visitFunctionalExpression(expression)
    }

    override fun visitStatement(statement: GrStatement) {
        super.visitStatement(statement)
    }

    override fun visitReturnStatement(returnStatement: GrReturnStatement) {
        super.visitReturnStatement(returnStatement)
    }

    override fun visitConditionalExpression(expression: GrConditionalExpression) {
        super.visitConditionalExpression(expression)
    }

    override fun visitGStringExpression(gstring: GrString) {
        super.visitGStringExpression(gstring)
    }

    override fun visitDoWhileStatement(statement: GrDoWhileStatement) {
        super.visitDoWhileStatement(statement)
    }

    override fun visitCaseSection(caseSection: GrCaseSection) {
        super.visitCaseSection(caseSection)
    }

    override fun visitTraditionalForClause(forClause: GrTraditionalForClause) {
        super.visitTraditionalForClause(forClause)
    }

    override fun visitNewExpression(newExpression: GrNewExpression) {
        super.visitNewExpression(newExpression)
    }

    override fun visitForInClause(forInClause: GrForInClause) {
        super.visitForInClause(forInClause)
    }

    override fun visitSynchronizedStatement(synchronizedStatement: GrSynchronizedStatement) {
        super.visitSynchronizedStatement(synchronizedStatement)
    }

    override fun visitArrayTypeElement(typeElement: GrArrayTypeElement) {
        super.visitArrayTypeElement(typeElement)
    }

    override fun visitMethod(method: GrMethod) {
        super.visitMethod(method)
    }

    override fun visitContinueStatement(continueStatement: GrContinueStatement) {
        super.visitContinueStatement(continueStatement)
    }

    override fun visitForClause(forClause: GrForClause) {
        super.visitForClause(forClause)
    }

    override fun visitArrayInitializer(arrayInitializer: GrArrayInitializer) {
        super.visitArrayInitializer(arrayInitializer)
    }

    override fun visitTuple(tuple: GrTuple) {
        super.visitTuple(tuple)
    }

    override fun visitTraitDefinition(traitTypeDefinition: GrTraitTypeDefinition) {
        super.visitTraitDefinition(traitTypeDefinition)
    }

    override fun visitElement(element: GroovyPsiElement) {
        super.visitElement(element)
    }

    override fun visitParameterList(parameterList: GrParameterList) {
        super.visitParameterList(parameterList)
    }

    override fun visitListOrMap(listOrMap: GrListOrMap) {
        super.visitListOrMap(listOrMap)
    }

    override fun visitIfStatement(ifStatement: GrIfStatement) {
        super.visitIfStatement(ifStatement)
    }

    override fun visitDocComment(comment: GrDocComment) {
        super.visitDocComment(comment)
    }

    override fun visitRegexExpression(expression: GrRegex) {
        super.visitRegexExpression(expression)
    }

    override fun visitVariable(variable: GrVariable) {
        super.visitVariable(variable)
    }

    override fun visitAnonymousClassDefinition(anonymousClassDefinition: GrAnonymousClassDefinition) {
        super.visitAnonymousClassDefinition(anonymousClassDefinition)
    }

    override fun visitFlowInterruptStatement(statement: GrFlowInterruptingStatement) {
        super.visitFlowInterruptStatement(statement)
    }

    override fun visitIndexProperty(expression: GrIndexProperty) {
        super.visitIndexProperty(expression)
    }

    override fun visitCatchClause(catchClause: GrCatchClause) {
        super.visitCatchClause(catchClause)
    }

    override fun visitThrowStatement(throwStatement: GrThrowStatement) {
        super.visitThrowStatement(throwStatement)
    }

    override fun visitTypeDefinition(typeDefinition: GrTypeDefinition) {
        super.visitTypeDefinition(typeDefinition)
    }

    override fun visitExtendsClause(extendsClause: GrExtendsClause) {
        super.visitExtendsClause(extendsClause)
    }

    override fun visitCastExpression(typeCastExpression: GrTypeCastExpression) {
        super.visitCastExpression(typeCastExpression)
    }

    override fun visitLabeledStatement(labeledStatement: GrLabeledStatement) {
        super.visitLabeledStatement(labeledStatement)
    }

    override fun visitBinaryExpression(expression: GrBinaryExpression) {
        super.visitBinaryExpression(expression)
    }

    override fun visitCommandArguments(argumentList: GrCommandArgumentList) {
        super.visitCommandArguments(argumentList)
    }

    override fun visitDocMethodReference(reference: GrDocMethodReference) {
        super.visitDocMethodReference(reference)
    }

    override fun visitBreakStatement(breakStatement: GrBreakStatement) {
        super.visitBreakStatement(breakStatement)
    }

    override fun visitImportStatement(importStatement: GrImportStatement) {
        super.visitImportStatement(importStatement)
    }

    override fun visitClosure(closure: GrClosableBlock) {
        super.visitClosure(closure)
    }

    override fun visitSafeCastExpression(typeCastExpression: GrSafeCastExpression) {
        super.visitSafeCastExpression(typeCastExpression)
    }

    override fun visitTryResourceList(resourceList: GrTryResourceList) {
        super.visitTryResourceList(resourceList)
    }

    override fun visitEnumConstants(enumConstantsSection: GrEnumConstantList) {
        super.visitEnumConstants(enumConstantsSection)
    }

    override fun visitParameter(parameter: GrParameter) {
        super.visitParameter(parameter)
    }

    override fun visitLambdaBody(body: GrLambdaBody) {
        super.visitLambdaBody(body)
    }

    override fun visitCodeReferenceElement(refElement: GrCodeReferenceElement) {
        super.visitCodeReferenceElement(refElement)
    }

    override fun visitDocTag(docTag: GrDocTag) {
        super.visitDocTag(docTag)
    }

    override fun visitArrayDeclaration(arrayDeclaration: GrArrayDeclaration) {
        super.visitArrayDeclaration(arrayDeclaration)
    }

    override fun visitTypeParameter(typeParameter: GrTypeParameter) {
        super.visitTypeParameter(typeParameter)
    }

    override fun visitBuiltinTypeElement(typeElement: GrBuiltInTypeElement) {
        super.visitBuiltinTypeElement(typeElement)
    }

    override fun visitArgumentList(list: GrArgumentList) {
        super.visitArgumentList(list)
    }

    override fun visitWildcardTypeArgument(wildcardTypeArgument: GrWildcardTypeArgument) {
        super.visitWildcardTypeArgument(wildcardTypeArgument)
    }

    override fun visitTypeDefinitionBody(typeDefinitionBody: GrTypeDefinitionBody) {
        super.visitTypeDefinitionBody(typeDefinitionBody)
    }

    override fun visitAnnotationArrayInitializer(arrayInitializer: GrAnnotationArrayInitializer) {
        super.visitAnnotationArrayInitializer(arrayInitializer)
    }

    override fun visitGStringInjection(injection: GrStringInjection) {
        super.visitGStringInjection(injection)
    }

    override fun visitTupleAssignmentExpression(expression: GrTupleAssignmentExpression) {
        super.visitTupleAssignmentExpression(expression)
    }

    override fun visitExpression(expression: GrExpression) {
        super.visitExpression(expression)
    }

    override fun visitClassTypeElement(typeElement: GrClassTypeElement) {
        super.visitClassTypeElement(typeElement)
    }

    override fun visitWhileStatement(whileStatement: GrWhileStatement) {
        super.visitWhileStatement(whileStatement)
    }

    override fun visitBuiltinTypeClassExpression(expression: GrBuiltinTypeClassExpression) {
        super.visitBuiltinTypeClassExpression(expression)
    }

    override fun visitArgumentLabel(argumentLabel: GrArgumentLabel) {
        super.visitArgumentLabel(argumentLabel)
    }

    override fun visitEnumConstant(enumConstant: GrEnumConstant) {
        super.visitEnumConstant(enumConstant)
    }

    override fun visitAssignmentExpression(expression: GrAssignmentExpression) {
        super.visitAssignmentExpression(expression)
    }

    override fun visitEnumDefinitionBody(enumDefinitionBody: GrEnumDefinitionBody) {
        super.visitEnumDefinitionBody(enumDefinitionBody)
    }

    override fun visitInExpression(expression: GrInExpression) {
        super.visitInExpression(expression)
    }

    override fun visitInterfaceDefinition(interfaceDefinition: GrInterfaceDefinition) {
        super.visitInterfaceDefinition(interfaceDefinition)
    }
}