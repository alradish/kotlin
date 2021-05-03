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
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
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

    private val intMinus = context.irBuiltIns.intClass.functions.single {
        it.owner.name.asString() == "minus" && it.owner.valueParameters.first().type == context.irBuiltIns.intType
    }
    private val arraySize = context.irBuiltIns.arrayClass.getPropertyGetter("size")
    private val collectionSize = context.irBuiltIns.collectionClass.getPropertyGetter("size")
    private val iterator = context.ir.symbols.iterable.functions.single { it.owner.name.asString() == "iterator" }
    private val intRangeTo = context.irBuiltIns.intClass.functions.single {
        it.owner.name.asString() == "rangeTo" && it.owner.valueParameters[0].type == context.irBuiltIns.intType
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression.origin != IrStatementOrigin.FOR_LOOP) {
            return super.visitBlock(expression)  // Not a for-loop block.
        }
        val old = expression.dumpKotlinLike()
        with(expression.statements) {
            assert(size == 2) { "Expected 2 statements in for-loop block, was:\n${expression.dump()}" }
            val iteratorVariable = get(0) as IrVariable
            assert(iteratorVariable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR) { "Expected FOR_LOOP_ITERATOR origin for iterator variable, was:\n${iteratorVariable.dump()}" }

            if (!iteratorVariable.type.isSubtypeOfClass(context.ir.symbols.iterator)) {
                return super.visitBlock(expression)
            }


            val iteratorCall = iteratorVariable.initializer as? IrCall
            val iterable = iteratorCall?.run {
                if (extensionReceiver != null) {
                    extensionReceiver
                } else {
                    dispatchReceiver
                }
            }

            val (from, to) = if ((iterable as? IrCall)?.symbol == intRangeTo) {
                iterable.run {
                    if (extensionReceiver != null) {
                        extensionReceiver
                    } else {
                        dispatchReceiver
                    } to this.getValueArgument(0)
                }
            } else {
                return super.visitBlock(expression)
            }
            val classifier = when (from) {
                is IrConst<*> -> when (from.kind) {
                    is IrConstKind.Int -> context.irBuiltIns.intClass
                    else -> return super.visitBlock(expression)
                }
                else -> return super.visitBlock(expression)
            }
            val newTo = when {
                to is IrCall && to.symbol == intMinus -> {
                    val t = to.getValueArgument(0) as? IrConst<*> ?: return super.visitBlock(expression)

                    // Check if this is minus one pattern
                    val isMinusOne = t.kind == IrConstKind.Int && t.value as? Int == 1
                    if (!isMinusOne)
                        return super.visitBlock(expression)

                    // Check if this is `array.size` or acceptable constant(without overflow)
                    val dispatchReceiver = to.dispatchReceiver
                    when {
                        dispatchReceiver is IrConst<*> -> TODO("Check if this is `good` constant")
                        dispatchReceiver is IrCall &&
                                (dispatchReceiver.symbol == arraySize || collectionSize in dispatchReceiver.symbol.owner.overriddenSymbols) -> {
                            dispatchReceiver
                        }
                        else -> return super.visitBlock(expression)
                    }
                }
                else -> return super.visitBlock(expression)
            }

            val tmp = JvmIrBuilder(context, iteratorCall.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET).run {
                val until = this@RangeUntilLowering.context.ir.symbols.untilByExtensionReceiver[classifier] ?: error("")
                val newRange = irCall(until).apply {
                    extensionReceiver = from
                    putValueArgument(0, newTo)
                }

                irCall(iterator).apply {
                    dispatchReceiver = newRange
                }
            }
            iteratorVariable.initializer = tmp
        }
        val new = expression.dumpKotlinLike()
        println("$old <> $new")
        return super.visitBlock(expression)
    }
}