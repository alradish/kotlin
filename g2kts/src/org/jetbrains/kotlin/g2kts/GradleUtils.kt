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