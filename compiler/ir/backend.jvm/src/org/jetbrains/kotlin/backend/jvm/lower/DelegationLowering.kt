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
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.codegen.state.getManglingSuffixBasedOnKotlinSignature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
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
    val symbol: IrFunctionSymbol?,
    val function: IrSimpleFunction?,
    val used: Boolean,
    val newF: IrSimpleFunction?
)


private class DelegationLowering(val context: JvmBackendContext) : IrElementVisitorVoid, FileLoweringPass {
    val analyzedDelegates: MutableMap<IrClass, KPropertyUsages> = mutableMapOf()

    override fun lower(irFile: IrFile) {
        return
//        error("lowering")
//        println("1")
        irFile.acceptChildrenVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        if (!declaration.isDelegated || declaration.isFakeOverride) {
            return
        }
//        error("stop")
//        Thread.sleep(10000)

        val backingFieldExpression = declaration
            .backingField
            ?.initializer
            ?.expression ?: return

        val delegateClass = when (backingFieldExpression) {
            is IrConstructorCall -> backingFieldExpression.annotationClass
            is IrGetObjectValue -> backingFieldExpression.symbol.owner
            is IrCall -> getDelegatedClassFromCall(backingFieldExpression, declaration) ?: return
            is IrBlock -> backingFieldExpression.type.takeIf { it is IrClassSymbol }?.classOrNull?.owner ?: return
            is IrConst<*> -> return
            is IrPropertyReference -> return // TODO Подробнее изучить случай делегирования проперти. Есть предположение что там всё само оптимизируется. Проверить что с именем
            is IrGetValue -> backingFieldExpression.type.classOrNull?.owner ?: return
            else -> TODO("Backing field was initialize with ${backingFieldExpression::class.simpleName}\n${backingFieldExpression.dumpKotlinLike()}")
        }

        if (delegateClass.isInterface)
            return

        analyzeAndReplaceCalls(declaration, delegateClass)
    }

    private fun getDelegatedClassFromCall(call: IrCall, property: IrProperty): IrClass? {
        val function = call.symbol.owner

        val returnExpressionsCollector = object : IrElementVisitorVoid {
            val returns = mutableListOf<IrExpression>()
            var currentScope: IrFunction = function

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitReturn(expression: IrReturn) {
                val value = expression.value
                val expressions = checkExpression(value)
                expressions.forEach(returns::add)
            }

            private fun checkExpression(expression: IrExpression): List<IrExpression> {
                return when (expression) {
                    is IrWhen -> expression.branches.flatMap { checkExpression(it.result) }
                    is IrBlock -> checkExpression(expression.statements.last() as IrExpression)
                    is IrCall -> expression.symbol.owner.let { f ->
                        if (f == currentScope) {
                            emptyList()
                        } else {
                            val old = currentScope
                            currentScope = f
                            emptyList<IrExpression>().also {
                                f.accept(this, null)
                                currentScope = old
                            }
                        }
                    }
                    else -> listOf(expression)
                }
            }
        }

        val expressions = returnExpressionsCollector.let {
            function.accept(returnExpressionsCollector, null)
            it.returns
        }

        val delegate =
            if (expressions.size == 1 && expressions.first() is IrGetField) {
                (expressions.first() as IrGetField).type.classOrNull?.owner
            } else if (expressions.isNotEmpty() && expressions.map { it as? IrConstructorCall }
                    .same { it?.type } && expressions.first() is IrConstructorCall) {
                (expressions.first() as IrConstructorCall).annotationClass
            } else {
                null
            }

        if (delegate != null && function.isOperator && function.name.identifier == "provideDelegate") {
            // Не работаем с provideDelegate, т.к. предполагаем что kProperty необходима
//            val provideDelegate = analyzeAndCopyOperator(function.parentAsClass, function)
//            if (provideDelegate.newF != null) {
//                property.replaceAllCalls(function, provideDelegate.newF)
//            }
            return null
        }

        if (delegate != null && delegate != property.backingField?.type?.classOrNull) {
            val builder = context.createIrBuilder(property.symbol)
            val backingField = property.backingField ?: return delegate
            property.backingField = property.factory.buildField {
                updateFrom(backingField)
                name = Name.identifier(backingField.name.asString())
                type = delegate.defaultType
            }.apply {
                initializer = context.createJvmIrBuilder(symbol).run {
                    irExprBody(irImplicitCast(backingField.initializer!!.expression, delegate.defaultType))
                }
                parent = property.parent
            }
        }

        return delegate
    }

    private fun analyzeAndReplaceCalls(delegatedProperty: IrProperty, delegate: IrClass) {
        val (get, set) = analyzedDelegates.getOrPut(delegate) {
            analyzeDelegateAndGenerate(delegate)
        }

        // FIXME убрать аргумент name
        if (!get.used && delegatedProperty.getter != null && get.newF != null) {
            delegatedProperty.getter!!.replaceAllCalls(
                get.symbol!!,
                get.function!!,
                get.newF,
                delegatedProperty.name.asString(),
                delegatedProperty
            )
        }
        if (!set.used && delegatedProperty.setter != null && set.newF != null) {
            delegatedProperty.setter!!.replaceAllCalls(
                set.symbol!!,
                set.function!!,
                set.newF,
                delegatedProperty.name.asString(),
                delegatedProperty
            )
        }
    }

    private fun IrElement.replaceAllCalls(
        symbolToReplace: IrFunctionSymbol,
        oldCall: IrSimpleFunction,
        newCall: IrSimpleFunction,
        name: String,
        property: IrProperty
    ) {
        val symbols = oldCall.overriddenSymbols + oldCall.symbol + symbolToReplace
        transform(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol in symbols) {
                    return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                        .run {
                            irCall(newCall as IrFunction).apply {
                                val call = expression
                                var indexOfNewArg = 0
                                (0 until call.valueArgumentsCount).forEach { i ->
                                    val valueArgument = call.getValueArgument(i)
                                    if (i != 1) {
                                        putValueArgument(indexOfNewArg++, valueArgument)
                                    } else {
                                        putValueArgument(
                                            indexOfNewArg++,
                                            IrConstImpl.string(
                                                UNDEFINED_OFFSET,
                                                UNDEFINED_OFFSET,
                                                context.irBuiltIns.stringType,
                                                name
                                            )
                                        )
                                    }
                                }
//                                dispatchReceiver = call.dispatchReceiver
                                val oldDispatchReceiver = call.dispatchReceiver
                                dispatchReceiver = if (oldDispatchReceiver is IrGetField && oldDispatchReceiver.receiver != null) {
                                    val variable = oldDispatchReceiver.receiver as IrGetValue
                                    irGetField(irGet(variable.type, variable.symbol), property.backingField!!)
                                } else {
                                    oldDispatchReceiver
                                }
                            }
                        }
                } else {
                    return super.visitCall(expression)
                }
            }
        }, null)
    }

    private fun analyzeAndCopyOperator(delegate: IrClass, name: String): KPropertyUsageInFun {
        return analyzeAndCopyOperator(delegate, delegate.getSimpleFunction(name))
    }

    private fun analyzeAndCopyOperator(delegate: IrClass, operator: IrSimpleFunction?): KPropertyUsageInFun {
        val used = operator?.let { analyzeOperatorForKPropertyUsage(it) } ?: false
        return KPropertyUsageInFun(
            operator?.symbol,
            operator,
            used,
            if (!used && operator != null) {
                copyFunWithoutProperty(delegate, operator)
            } else {
                null
            }
        )
    }

    private fun analyzeDelegateAndGenerate(delegate: IrClass): KPropertyUsages {
        // FIXME Вложенность суперов может быть больше 1.
        val (getSymbol, getValue) = delegate.getSimpleFunction("getValue")?.let {
            if (it.isFakeOverride)
                it.symbol to delegate.superTypes.mapNotNull { it.classOrNull?.getSimpleFunction("getValue")?.owner }.firstOrNull()
            else it.symbol to it
        } ?: null to null

        val (setSymbol, setValue) = delegate.getSimpleFunction("setValue")?.let {
            if (it.isFakeOverride)
                it.symbol to delegate.superTypes.mapNotNull { it.classOrNull?.getSimpleFunction("setValue")?.owner }.firstOrNull()
            else it.symbol to it
        } ?: null to null

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
                getSymbol,
                getValue,
                usedInGet,
                newGet
            ),
            KPropertyUsageInFun(
                setSymbol,
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
            this.modality = Modality.FINAL
            this.visibility = function.visibility
            this.isSuspend = function.isSuspend
            this.isFakeOverride = false
            this.origin = IrDeclarationOrigin.DEFINED
        }.apply {
            function.typeParameters.forEach {
                addTypeParameter(it.name.identifier, it.representativeUpperBound, it.variance)
            }

            // Я не знаю что лучше и чем они отличаются
//            dispatchReceiverParameter = delegate.thisReceiver?.copyTo(this) // выдаёт ошибку если у делегата есть параметр типа
            dispatchReceiverParameter = function.dispatchReceiverParameter?.copyTo(this) // РАБОТАЕТ В БОЛЬШИНСТВЕ СЛУЧАЕВ


            function.valueParameters.forEach { valueArgument ->
                when {
                    valueArgument.index == 1 -> addValueParameter("name", context.irBuiltIns.stringType)
                    valueArgument.index != 1 && valueArgument.index != -1 -> addValueParameter(
                        valueArgument.name.identifier,
                        valueArgument.type
                    )
                }
            }
            val nameParameter = valueParameters[1]

            val oldParameters = function.valueParameters + function.dispatchReceiverParameter!!
            val newParameters = valueParameters.toMutableList<IrValueParameter?>().apply {
                set(1, null)
                add(dispatchReceiverParameter)
            }
            val oldSymbols: List<IrSymbol> = listOf(function.symbol)
            val newSymbols: List<IrSymbol> = listOf(this.symbol)

            val parameterMapping = oldParameters.zip(newParameters).toMap()
            val symbolsMapping = oldSymbols
                .zip(newSymbols)
                .toMap()
                .plus(parameterMapping.mapNotNull { (k, v) -> v?.let { k.symbol to v.symbol } })

            val parameterTransformer = object : IrElementTransformerVoidWithContext() {
                override fun visitGetValue(expression: IrGetValue): IrGetValue {
                    return parameterMapping[expression.symbol.owner]?.let {
                        expression.run { IrGetValueImpl(startOffset, endOffset, type, it.symbol, origin) }
                    } ?: expression
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    val dispatchReceiver = expression.dispatchReceiver as? IrGetValue ?: return super.visitCall(expression)
                    return if (dispatchReceiver.symbol == function.valueParameters[1].symbol && expression.symbol.owner.name.asString() == "<get-name>") {
                        expression.run {
                            IrGetValueImpl(
                                startOffset,
                                endOffset,
                                nameParameter.type,
                                nameParameter.symbol,
                                origin
                            )
                        }
                    } else {
                        super.visitCall(expression)
                    }
                }

                override fun visitReturn(expression: IrReturn): IrExpression {
                    return super.visitReturn(
                        with(expression) {
                            IrReturnImpl(
                                startOffset,
                                endOffset,
                                type,
                                symbolsMapping[expression.returnTargetSymbol] as? IrReturnTargetSymbol
                                    ?: expression.returnTargetSymbol,
                                value
                            )
                        }
                    )
                }
            }

            body = function.body?.deepCopyWithSymbols(this)?.also {
                it.transform(parameterTransformer, null)
            }

//            overriddenSymbols = function.overriddenSymbols.toList()
        }
    }

    private fun IrClass.getSimpleFunction(name: String): IrSimpleFunction? {
        // FIXME Нет доступа к операторам объявленым внешне(через экстеншн)
        // TODO попробовать заменить на findDeclaration
        return symbol.getSimpleFunction(name)?.owner
    }

    private fun analyzeOperatorForKPropertyUsage(function: IrFunction): Boolean {
        // FIXME Тело может быть пустым. Нужно как-то обработать это
        val kPropertySymbol = function.valueParameters[1].symbol
        val analyzer = object : IrElementVisitorVoid {
            var used = false
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                val dispatchReceiver = expression.dispatchReceiver as? IrGetValue ?: return super.visitCall(expression)
                if (dispatchReceiver.symbol == kPropertySymbol) {
                    if (expression.symbol.owner.name.asString() != "<get-name>") {
                        used = true
                    }
                } else {
                    super.visitCall(expression)
                }
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