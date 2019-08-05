/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.tree

interface GradleSymbol {
    val target: Any
    val declaredIn: GradleSymbol
    val fqName: String
}

interface GradleNamedSymbol : GradleSymbol {
    val name: String
}