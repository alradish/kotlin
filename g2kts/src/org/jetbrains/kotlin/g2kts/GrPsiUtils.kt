/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall

val GrMethodCall.rawName: String?
    get() = invokedExpression.text