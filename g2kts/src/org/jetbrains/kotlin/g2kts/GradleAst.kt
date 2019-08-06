/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

sealed class GNode

sealed class GStatement : GNode() {
    data class GExpr(var expr: GExpression) : GStatement()
    data class GDecl(var decl: GDeclaration) : GStatement()
}

data class GBlock(
    var statements: List<GStatement>
) : GStatement()

data class GArgumentsList(
    var args: List<GArgument>
) : GNode() {
    data class GArgument(
        var name: String?,
        var expr: GExpression
    )

    constructor(vararg args: Pair<String?, GExpression>) : this(args.map { GArgument(it.first, it.second) })
}

// ********** EXPRESSION **********
sealed class GExpression : GNode() {
    fun toStatement(): GStatement = GStatement.GExpr(this)
}

data class GName(
    var name: String
) : GExpression()

sealed class GMethodCall : GExpression() {
    abstract var obj: GExpression?
    abstract var method: GExpression
    abstract var arguments: GArgumentsList
}

data class GSimpleMethodCall(
    override var obj: GExpression?,
    override var method: GExpression,
    override var arguments: GArgumentsList
) : GMethodCall()

data class GConfigurationBlock(
    override var obj: GExpression?,
    override var method: GExpression,
    override var arguments: GArgumentsList,
    var configuration: GClosure
) : GMethodCall()

data class GClosure( // GTODO arguments
    var parameters: List<GExpression>, // GTODO make GParameter
    var statements: GBlock
) : GExpression()

data class GTaskCreating(
    var buf: String
) : GExpression()

data class GConst(
    var text: String
) : GExpression()

data class GString(
    var str: String // GTODO template
) : GExpression()

data class GBinaryExpression(
    var left: GExpression,
    var operator: String,
    var right: GExpression
) : GExpression()

data class GExtensionAccess(
    var ext: String
) : GExpression() {
    companion object {
        val EXT: String = "ext"
    }
}

data class GTaskAccess(
    var task: String
) : GExpression() {
    companion object {
        val TASKS: String = "tasks"
    }
}

// ********** EXPRESSION END **********


sealed class GDeclaration : GNode() {
    fun toStatement(): GStatement = GStatement.GDecl(this)
}


data class GProject(
    var statements: List<GStatement>
) : GNode()