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
        if (!declaration.isDelegated || declaration.isFakeOverride) {
            super.visitProperty(declaration)
            return
        }

        val backingFieldExpression = declaration.backingField?.initializer?.expression ?: error("Backing field is null") // FIXME
        val delegateClass = when (backingFieldExpression) {
            is IrConstructorCall -> backingFieldExpression.annotationClass
//            is IrCall -> backingFieldExpression.type.classOrNull?.owner ?: return // Могут вернуть общий интерфейс
//            is IrCall -> return // Могут вернуть общий интерфейс
            is IrCall -> {
                val f = backingFieldExpression.symbol.owner
                if (f.isOperator && f.name.identifier == "provideDelegate") {
                    // TODO Проверить возвращает ли провайдДелегат конструктор
                    val returns = ReturnConstructorCollector().let {
                        f.accept(it, null)
                        it.returns
                    }
                    if (returns.isNotEmpty() && returns.all { it is IrConstructorCall } && returns.same { (it as IrConstructorCall).type }) {
                        (returns.first() as IrConstructorCall).annotationClass
                    } else {
                        return
                    }
                } else {
                    return
                }
            }
            is IrBlock -> backingFieldExpression.type.takeIf { it is IrClassSymbol }?.classOrNull?.owner ?: return
            is IrConst<*> -> return
            is IrGetObjectValue -> backingFieldExpression.symbol.owner
            is IrPropertyReference -> TODO()
            else -> TODO("Backing field was initialize with ${backingFieldExpression::class.simpleName}\n${backingFieldExpression.dumpKotlinLike()}")
        }

        if (delegateClass.isInterface)
            return

        val (get, set) = analyzedDelegates.getOrPut(delegateClass) {
            analyzeDelegateAndGenerate(delegateClass)
        }

        if (!get.used && declaration.getter != null && get.newF != null) {
            declaration.getter!!.replaceCallInReturn(get.newF)
        }
        if (!set.used && declaration.setter != null && set.newF != null) {
            declaration.setter!!.replaceCallInReturn(set.newF)
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
                            if (i != 1)
//                            if (kProperty !in valueArgument!!.type.allSuperTypes().map { it.classOrNull })
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
        val usedInGet = getValue?.let { KPropertyUsageFuncAnalyzer(getValue).used } ?: false
        val usedInSet = setValue?.let { KPropertyUsageFuncAnalyzer(setValue).used } ?: false

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

    private fun copyFunWithoutProperty(delegate: IrClass, f: IrSimpleFunction): IrSimpleFunction {
        return delegate.addFunction {
            this.startOffset = f.startOffset
            this.endOffset = f.endOffset
            this.name = Name.identifier(f.name.identifier)
            this.returnType = f.returnType
            this.modality = f.modality
            this.visibility = f.visibility
            this.isSuspend = f.isSuspend
            this.isFakeOverride = f.isFakeOverride
            this.origin = f.origin
            this.isOperator = false
        }.apply {
            val f = f
            f.typeParameters.forEach {
                addTypeParameter(it.name.identifier, it.representativeUpperBound, it.variance)
            }


            // TODO: Не уверен, что явно задавать тип правильно
//            dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
//            dispatchReceiverParameter = parentAsClass.thisReceiver?.copyTo(this, type = parentAsClass.defaultType)
            dispatchReceiverParameter = f.dispatchReceiverParameter?.copyTo(this)
//            dispatchReceiverParameter = parentAsClass.thisReceiver
//            createDispatchReceiverParameter()


            f.valueParameters.forEach { valueArgument ->
                if (valueArgument.index != 1 && valueArgument.index != -1) { // FIXME определять по типу
                    addValueParameter(valueArgument.name.identifier, valueArgument.type)
                }
            }


            val oldParameters = f.valueParameters + f.dispatchReceiverParameter!!
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

//                override fun visitReturn(expression: IrReturn): IrExpression {
////                    expression.transformChildren(this, null)
//                    expression.value = expression.value.transform(this, null)
//                    return IrReturnImpl(expression.startOffset, expression.endOffset, expression.type, symbol, expression.value)
//                }
            }
            body = f.body?.deepCopyWithSymbols(this)?.also {
                it.transform(parameterTransformer, null)
            }
            overriddenSymbols = f.overriddenSymbols.toList()
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