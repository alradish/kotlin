/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val propertyDelegationPhase = makeIrFilePhase(
    ::DelegationLowering,
    name = "PropertyDelegationLowering",
    description = "For property delegation lowering"
)

private data class KPropertyUsages(
    val get: KPropertyUsageInFun,
    val set: KPropertyUsageInFun
)

private data class KPropertyUsageInFun(
    val f: IrSimpleFunction?,
    val used: Boolean,
    val newF: IrSimpleFunction?
)

private class DelegationLowering(val context: JvmBackendContext) : IrElementVisitorVoid, FileLoweringPass {
    val analyzedDelegates: MutableMap<IrClass, KPropertyUsages> = mutableMapOf()

    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        if (!declaration.isDelegated) {
            super.visitProperty(declaration)
            return
        }

        val backingFieldExpression = declaration.backingField?.initializer?.expression ?: error("Backing field is null") // FIXME
        val delegateClass = when (backingFieldExpression) {
            is IrConstructorCall -> backingFieldExpression.annotationClass
            is IrCall -> backingFieldExpression.type.classOrNull?.owner ?: return
            else -> TODO("Backing field was initialize with ${backingFieldExpression::class.simpleName}")
        }

        val (get, set) = analyzedDelegates.getOrPut(delegateClass) {
            analyzeDelegateAndGenerate(delegateClass)
        }
        val parent = declaration.parent as IrClass

        if (!get.used && declaration.getter != null) {
            declaration.getter!!.replaceCallInReturn(get.newF!!)
        }
        if (!set.used && declaration.setter != null) {
            declaration.setter!!.replaceCallInReturn(set.newF!!)
        }
    }

    private fun IrType.allSuperTypes(): List<IrType> {
        return superTypes().flatMap { it.allSuperTypes().plus(it) }
    }

    private fun IrSimpleFunction.replaceCallInReturn(newCall: IrSimpleFunction) {
        val kProperty = context.irBuiltIns.kPropertyClass
        transform(object : IrElementTransformerVoidWithContext() {
            override fun visitReturn(expression: IrReturn): IrExpression {
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
                    irReturn(irCall(newCall as IrFunction).apply {
                        val call = expression.value as IrCall
                        dispatchReceiver = call.dispatchReceiver
                        var indexOfNewArg = 0
                        (0 until call.valueArgumentsCount).forEach { i ->
                            val valueArgument = call.getValueArgument(i)
                            if (kProperty !in valueArgument!!.type.allSuperTypes().map { it.classOrNull })
                                putValueArgument(indexOfNewArg++, valueArgument)
                        }
                    })
                }
            }
        }, null)
    }

    private fun analyzeDelegateAndGenerate(delegate: IrClass): KPropertyUsages {
        val getValue = delegate.symbol.getSimpleFunction("getValue")?.owner
        val setValue = delegate.symbol.getSimpleFunction("setValue")?.owner

        val usedInGet = getValue?.let { KPropertyUsageFuncAnalyzer(getValue).used } ?: false
        val usedInSet = setValue?.let { KPropertyUsageFuncAnalyzer(setValue).used } ?: false

        val newGet = if (!usedInGet) {
            copyFunWithoutProperty(delegate, getValue!!)
        } else null
        val newSet = if (!usedInSet) {
            copyFunWithoutProperty(delegate, setValue!!)
        } else null

        return KPropertyUsages(
            KPropertyUsageInFun(
                getValue,
                usedInGet,
                newGet
            ),
            KPropertyUsageInFun(
                setValue,
                usedInSet,
                newSet
            ),
        )
    }

    private fun copyFunWithoutProperty(delegate: IrClass, f: IrSimpleFunction): IrSimpleFunction {
        return delegate.addFunction(
            f.name.identifier,
            f.returnType,
        ).apply {
            f.valueParameters.forEach {
                if (it.index != 1) { // FIXME определять по типу
                    addValueParameter(it.name.identifier, it.type)
                }
            }
            body = f.body?.deepCopyWithSymbols(this)
        }
    }


}

private class KPropertyUsageFuncAnalyzer(val function: IrFunction) : IrElementVisitorVoid {
    var used = false
        private set

    private val propertySymbol = function.valueParameters[1].symbol // FIXME

    init {
        visitFunction(function)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitGetValue(expression: IrGetValue) {
        if (expression.symbol == propertySymbol)
            used = true
        else
            super.visitGetValue(expression)
    }
}