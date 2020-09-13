/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun GStatement.isApplyPlugin(): Boolean {
    val expr = this.safeAs<GStatement.GExpr>()?.expr ?: return false
    val call = expr as? GMethodCall ?: return false
    val method = expr.method as? GIdentifier ?: return false
    return method.name == "apply" && call.arguments.args.size == 1 && call.arguments.args.first().name == "plugin"
}

fun GNode.isConfigurationBlock(): Boolean {
    fun GExpression.isConfigurationBlock(): Boolean {
        return this is GMethodCall
                && obj == null
                && method is GIdentifier
                && arguments.args.isEmpty()
                && closure != null
    }
    return when (this) {
        is GStatement.GExpr -> expr.isConfigurationBlock()
        is GExpression -> isConfigurationBlock()
        else -> false
    }
}

fun GProject.firstIsBuildScriptBlock(): Boolean {
    return if (statements.first() is GStatement.GExpr) {
        val s = (statements.first() as GStatement.GExpr).expr
        s is GBuildScriptBlock || s.isBuildScriptBlock()
    } else {
        false
    }
}

fun GNode.isBuildScriptBlock(): Boolean {
    if (!(this is GMethodCall && obj == null)) return false
    val name = (method as? GIdentifier)?.name
    if (GBuildScriptBlock.BuildScriptBlockType.values().find { it.text == name } == null) return false
    if (arguments.args.isNotEmpty() || closure == null) return false
    return true
}