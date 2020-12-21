/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

val propertyDelegationPhase = makeIrFilePhase(
    ::DelegationLowering,
    name = "PropertyDelegationLowering",
    description = "For property delegation lowering"
)

class DelegationLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val delegates: Map<IrClass, KPropertyUsages> = KPropertyUsageAnalyzer(context).let {
            irFile.acceptChildrenVoid(it)
            it.analyzedDelegates
        }


        for ((delegate, usages) in delegates) {
            println(delegate.name.toString() + " " + usages.toString())
            if (!usages.inGet) {
                copyFunWithoutProperty(delegate, usages.get!!)
            }
            if (!usages.inSet) {
                copyFunWithoutProperty(delegate, usages.set!!)
            }
        }
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

private data class KPropertyUsages(
    val get: IrSimpleFunction?,
    val inGet: Boolean,
    val set: IrSimpleFunction?,
    val inSet: Boolean
)

private class KPropertyUsageAnalyzer(val context: JvmBackendContext) : IrElementVisitorVoid {
    val analyzedDelegates: MutableMap<IrClass, KPropertyUsages> = mutableMapOf()


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
            else -> TODO("Backing field was initialize not with constructor call")
        }

        analyzedDelegates.getOrPut(delegateClass) {
            analyzeDelegate(delegateClass)
        }

        super.visitProperty(declaration)
    }

    private fun analyzeDelegate(delegate: IrClass): KPropertyUsages {
        val getValue = delegate.symbol.getSimpleFunction("getValue")?.owner
        val setValue = delegate.symbol.getSimpleFunction("setValue")?.owner

        val usedInGet = getValue?.let { KPropertyUsageFuncAnalyzer(getValue).used } ?: false
        val usedInSet = setValue?.let { KPropertyUsageFuncAnalyzer(setValue).used } ?: false

        return KPropertyUsages(getValue, usedInGet, setValue, usedInSet)
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