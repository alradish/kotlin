/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPropertyGetter

val rangeMinusOne = makeIrFilePhase(
    ::RangeUntilLowering,
    name = "RangeUntilLowering",
    description = "For loops lowering"
)

class RangeUntilLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid()
    }

//    private val minusMatcher = createIrCallMatcher {
//
//    }

    private val intMinus = context.irBuiltIns.intClass.functions.single {
        it.owner.name.asString() == "minus" && it.owner.valueParameters.first().type == context.irBuiltIns.intType
    }
    private val arraySize = context.irBuiltIns.arrayClass.getPropertyGetter("size")

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression.origin != IrStatementOrigin.FOR_LOOP) {
            return super.visitBlock(expression)  // Not a for-loop block.
        }

        with(expression.statements) {
            assert(size == 2) { "Expected 2 statements in for-loop block, was:\n${expression.dump()}" }
            val iteratorVariable = get(0) as IrVariable
            assert(iteratorVariable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR) { "Expected FOR_LOOP_ITERATOR origin for iterator variable, was:\n${iteratorVariable.dump()}" }
//            val initializer = iteratorVariable.initializer ?: return super.visitBlock(expression)

            if (!iteratorVariable.type.isSubtypeOfClass(context.ir.symbols.iterator)) {
                return super.visitBlock(expression)
            }

            // Get the iterable expression, e.g., `someIterable` in the following loop variable declaration:
            //
            //   val it = someIterable.iterator()
            val iteratorCall = iteratorVariable.initializer as? IrCall
            val iterable = iteratorCall?.run {
                if (extensionReceiver != null) {
                    extensionReceiver
                } else {
                    dispatchReceiver
                }
            }

            val (from, to) = if (iterable?.type?.isSubtypeOfClass(context.ir.symbols.intRange) == true) {
                (iterable as IrCall).run {
                    if (extensionReceiver != null) {
                        extensionReceiver
                    } else {
                        dispatchReceiver
                    } to this.getValueArgument(0)
                }
            } else {
                return super.visitBlock(expression)
            }
            val (classifier, newFrom) = when (from) {
                is IrConst<*> -> when (from.kind) {
                    is IrConstKind.Int -> context.irBuiltIns.intClass to from.value as Int
                    else -> TODO()
                }
                else -> TODO()
            }
            println(newFrom)
            val newTo = when {
                to is IrConst<*> -> TODO()

                to is IrCall && to.symbol == intMinus -> {
                    val t = to.getValueArgument(0) as? IrConst<*> ?: return super.visitBlock(expression)

                    // Check if this is minus one pattern
                    val isMinusOne = t.kind == IrConstKind.Int && t.value as? Int == 1
                    println(isMinusOne)

                    // Check if this is `array.size` or acceptable constant(without overflow)
                    val dispatchReceiver = to.dispatchReceiver
                    when {
                        dispatchReceiver is IrConst<*> -> TODO("Check if this is `good` constant")
                        dispatchReceiver is IrCall && dispatchReceiver.symbol == arraySize -> {
                            dispatchReceiver
                        }
                        else -> return super.visitBlock(expression)
                    }
                }
                else -> return super.visitBlock(expression)
            }

//            val tmp = context.createJvmIrBuilder(iteratorCall.symbol).run {
            val tmp = JvmIrBuilder(context, iteratorCall.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET).run {
                val until = this@RangeUntilLowering.context.ir.symbols.untilByExtensionReceiver[classifier] ?: error("")
                val newRange = irCall(until).apply {
                    extensionReceiver = from
                    putValueArgument(0, newTo)
                }

                irCall(this@RangeUntilLowering.context.ir.symbols.iterable.functions.single { it.owner.name.asString() == "iterator" }).apply {
                    dispatchReceiver = newRange
                }
            }
            iteratorVariable.initializer = tmp
        }
        return super.visitBlock(expression)
    }
}