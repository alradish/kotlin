/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.backend.jvm.codegen.representativeUpperBound
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.same

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
    val function: IrSimpleFunction?,
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
        if (!declaration.isDelegated || declaration.isFakeOverride) {
            return
        }

        val backingFieldExpression = declaration
            .backingField
            ?.initializer
            ?.expression ?: return

        val delegateClass = when (backingFieldExpression) {
            is IrConstructorCall -> backingFieldExpression.annotationClass
            is IrGetObjectValue -> backingFieldExpression.symbol.owner
            is IrCall -> backingFieldExpression.getDelegatedClassFromCall() ?: return
            is IrBlock -> backingFieldExpression.type.takeIf { it is IrClassSymbol }?.classOrNull?.owner ?: return
            is IrConst<*> -> return
            is IrPropertyReference -> TODO()
            else -> TODO("Backing field was initialize with ${backingFieldExpression::class.simpleName}\n${backingFieldExpression.dumpKotlinLike()}")
        }

        if (delegateClass.isInterface)
            return

        analyzeAndReplaceCalls(declaration, delegateClass)
    }

    // TODO Убрать аннотацию когда реализую хотя бы один из случаев
    @Suppress("UNREACHABLE_CODE")
    private fun IrCall.getDelegatedClassFromCall(): IrClass? {
        val function = symbol.owner
        val expressions: List<IrExpression> = when {
            function.isOperator && function.name.identifier == "provideDelegate" -> TODO("Обработать случай с provideDelegate")
            else -> TODO("Обработать случай с делегированием через функцию")
        }
        return if (expressions.isNotEmpty() && expressions.same { (it.type as? IrConstructorCall)?.type }) {
            (expressions.first() as IrConstructorCall).annotationClass
        } else {
            null
        }
    }

    private fun analyzeAndReplaceCalls(delegatedProperty: IrProperty, delegate: IrClass) {
        val (get, set) = analyzedDelegates.getOrPut(delegate) {
            analyzeDelegateAndGenerate(delegate)
        }

        if (!get.used && delegatedProperty.getter != null && get.newF != null) {
            delegatedProperty.getter!!.replaceCallInReturn(get.newF)
        }
        if (!set.used && delegatedProperty.setter != null && set.newF != null) {
            delegatedProperty.setter!!.replaceCallInReturn(set.newF)
        }
    }

    private fun IrSimpleFunction.replaceCallInReturn(newCall: IrSimpleFunction) {
        transform(object : IrElementTransformerVoidWithContext() {
            override fun visitReturn(expression: IrReturn): IrExpression {
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
                    irReturn(irCall(newCall as IrFunction).apply {
                        val call = expression.value as IrCall
                        dispatchReceiver = call.dispatchReceiver
                        var indexOfNewArg = 0
                        (0 until call.valueArgumentsCount).forEach { i ->
                            val valueArgument = call.getValueArgument(i)
                            if (i != 1)
                                putValueArgument(indexOfNewArg++, valueArgument)
                        }
                    })
                }
            }
        }, null)
    }

    private fun analyzeDelegateAndGenerate(delegate: IrClass): KPropertyUsages {
        // FIXME Нет доступа к операторам объявленым внешне(через экстеншн)
        val getValue = delegate.symbol.getSimpleFunction("getValue")?.owner
        val setValue = delegate.symbol.getSimpleFunction("setValue")?.owner

        // FIXME Тело может быть пустым. Нужно как-то обработать это
        val usedInGet = getValue?.let { analyzeOperatorForKPropertyUsage(it) } ?: false
        val usedInSet = setValue?.let { analyzeOperatorForKPropertyUsage(it) } ?: false

        val newGet = if (!usedInGet && getValue != null) {
            copyFunWithoutProperty(delegate, getValue)
        } else null
        val newSet = if (!usedInSet && setValue != null) {
            copyFunWithoutProperty(delegate, setValue)
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

    private fun copyFunWithoutProperty(delegate: IrClass, function: IrSimpleFunction): IrSimpleFunction {
        return delegate.addFunction {
            this.startOffset = function.startOffset
            this.endOffset = function.endOffset
            this.name = Name.identifier(function.name.identifier)
            this.returnType = function.returnType
            this.modality = function.modality
            this.visibility = function.visibility
            this.isSuspend = function.isSuspend
            this.isFakeOverride = function.isFakeOverride
            this.origin = function.origin
            this.isOperator = false
        }.apply {
            function.typeParameters.forEach {
                addTypeParameter(it.name.identifier, it.representativeUpperBound, it.variance)
            }

            dispatchReceiverParameter = function.dispatchReceiverParameter?.copyTo(this)

            function.valueParameters.forEach { valueArgument ->
                if (valueArgument.index != 1 && valueArgument.index != -1) { // FIXME определять по типу
                    addValueParameter(valueArgument.name.identifier, valueArgument.type)
                }
            }


            val oldParameters = function.valueParameters + function.dispatchReceiverParameter!!
            val newParameters = valueParameters.toMutableList<IrValueParameter?>().apply {
                add(1, null)
                add(dispatchReceiverParameter)
            }

            val parameterMapping = oldParameters.zip(newParameters).toMap()

            val parameterTransformer = object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrGetValue {
                    return parameterMapping[expression.symbol.owner]?.let {
                        expression.run { IrGetValueImpl(startOffset, endOffset, type, it.symbol, origin) }
                    } ?: expression
                }
            }

            body = function.body?.deepCopyWithSymbols(this)?.also {
                it.transform(parameterTransformer, null)
            }

            overriddenSymbols = function.overriddenSymbols.toList()
        }
    }

    private fun analyzeOperatorForKPropertyUsage(function: IrFunction): Boolean {
        val kPropertySymbol = function.valueParameters[1].symbol
        val analyzer = object : IrElementVisitorVoid {
            var used = false
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitGetValue(expression: IrGetValue) {
                if (expression.symbol == kPropertySymbol) {
                    used = true
                }
            }
        }

        return analyzer.apply {
            visitFunction(function)
        }.used
    }
}

private class ReturnConstructorCollector : IrElementVisitorVoid {
    val returns = mutableListOf<IrExpression>()


    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitReturn(expression: IrReturn) {
        val value = expression.value
        if (value is IrCall) {
            value.symbol.owner.accept(this, null)
        } else {
            returns.add(value)
        }
    }
}