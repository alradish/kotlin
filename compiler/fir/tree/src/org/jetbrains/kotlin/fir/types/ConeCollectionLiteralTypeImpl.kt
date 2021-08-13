/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

class ConeCollectionLiteralTypeImpl(
    override val nullability: ConeNullability,
    override val possibleTypes: Set<ConeKotlinType>
) : ConeCollectionLiteralType() {

    override var argumentType: ConeKotlinType? = null

    override val attributes: ConeAttributes
        get() = ConeAttributes.Empty

}