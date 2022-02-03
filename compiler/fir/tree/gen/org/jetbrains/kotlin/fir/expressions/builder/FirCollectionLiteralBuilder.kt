/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.CollectionLiteralKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteralEntry
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirCollectionLiteralImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirCollectionLiteralBuilder : FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var kind: CollectionLiteralKind
    var receiverExpression: FirExpression? = null
    var argumentType: FirTypeRef? = null
    var keyArgumentType: FirTypeRef? = null
    var valueArgumentType: FirTypeRef? = null
    val builders: MutableList<FirNamedFunctionSymbol> = mutableListOf()
    val expressions: MutableList<FirCollectionLiteralEntry> = mutableListOf()

    override fun build(): FirCollectionLiteral {
        return FirCollectionLiteralImpl(
            source,
            annotations,
            kind,
            receiverExpression,
            argumentType,
            keyArgumentType,
            valueArgumentType,
            builders,
            expressions,
        )
    }


    @Deprecated("Modification of 'typeRef' has no impact for FirCollectionLiteralBuilder", level = DeprecationLevel.HIDDEN)
    override var typeRef: FirTypeRef
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildCollectionLiteral(init: FirCollectionLiteralBuilder.() -> Unit): FirCollectionLiteral {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirCollectionLiteralBuilder().apply(init).build()
}
