/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.CollectionLiteralKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteralEntry
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirCollectionLiteralImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override val kind: CollectionLiteralKind,
    override var receiverExpression: FirExpression?,
    override var argumentType: FirTypeRef?,
    override var keyArgumentType: FirTypeRef?,
    override var valueArgumentType: FirTypeRef?,
    override val expressions: MutableList<FirCollectionLiteralEntry>,
) : FirCollectionLiteral() {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        typeRef.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        receiverExpression?.accept(visitor, data)
        argumentType?.accept(visitor, data)
        keyArgumentType?.accept(visitor, data)
        valueArgumentType?.accept(visitor, data)
        expressions.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirCollectionLiteralImpl {
        typeRef = typeRef.transform(transformer, data)
        transformAnnotations(transformer, data)
        receiverExpression = receiverExpression?.transform(transformer, data)
        argumentType = argumentType?.transform(transformer, data)
        keyArgumentType = keyArgumentType?.transform(transformer, data)
        valueArgumentType = valueArgumentType?.transform(transformer, data)
        transformExpressions(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirCollectionLiteralImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformExpressions(transformer: FirTransformer<D>, data: D): FirCollectionLiteralImpl {
        expressions.transformInplace(transformer, data)
        return this
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceReceiverExpression(newReceiverExpression: FirExpression?) {
        receiverExpression = newReceiverExpression
    }

    override fun replaceArgumentType(newArgumentType: FirTypeRef?) {
        argumentType = newArgumentType
    }

    override fun replaceKeyArgumentType(newKeyArgumentType: FirTypeRef?) {
        keyArgumentType = newKeyArgumentType
    }

    override fun replaceValueArgumentType(newValueArgumentType: FirTypeRef?) {
        valueArgumentType = newValueArgumentType
    }
}
