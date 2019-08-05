/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.tree

interface GradleElement

interface GradleNameIdentifier : GradleElement {
    var name: String
}

interface GradleStatement : GradleElement

interface GradleExpression : GradleStatement

interface GradleProperty : GradleStatement // GTODO

interface GradleMethodCall : GradleExpression {
    var obj: GradleElement?
    var name: GradleNameIdentifier
    var arguments: GradleArgumentList
}

interface GradleBlock : GradleMethodCall {
    var configuration: GradleLambda
}

interface GradleArgumentList : GradleElement {
    var arguments: List<GradleElement>
}

interface GradleLambda : GradleElement {
    var delegate: GradleElement?
    var statements: List<GradleStatement>
}

interface GradleProject : GradleElement {
    var statements: List<GradleStatement>
}

interface GradleConstant : GradleExpression {
    var value: Any?
}


interface GradleAssignment : GradleExpression {
    var left: GradleExpression
    var right: GradleExpression
    var operator: GradleOperator
}

interface GradleOperator {
    val text: String
}

interface GradleReferenceExpression : GradleExpression {

}