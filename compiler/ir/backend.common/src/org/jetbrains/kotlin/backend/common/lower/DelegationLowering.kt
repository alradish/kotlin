/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val propertyDelegationPhase = makeIrFilePhase(
    ::DelegationLowering,
    name = "PropertyDelegationLowering",
    description = "For property delegation lowering"
)

class DelegationLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = PropertyDelegationTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

private class PropertyDelegationTransformer(val context: CommonBackendContext) : IrElementTransformerVoid() {
//    override fun <T> visitConst(expression: IrConst<T>): IrExpression {
//        return IrConstImpl<String>(expression.startOffset, expression.endOffset, expression.type, IrConstKind.String, "privet")
//    }
}