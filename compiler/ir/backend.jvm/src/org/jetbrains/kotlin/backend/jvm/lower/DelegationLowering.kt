/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.classIfConstructor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

val propertyDelegationPhase = makeIrFilePhase(
    ::DelegationLowering,
    name = "PropertyDelegationLowering",
    description = "For property delegation lowering"
)

class DelegationLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        KPropertyUsageAnalyzer(context).let(irFile::acceptChildrenVoid)

//        val transformer = PropertyDelegationTransformer(context)
//        irFile.transformChildrenVoid(transformer)
    }
}

private class KPropertyUsageAnalyzer(val context: JvmBackendContext) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        if (!declaration.isDelegated) {
            super.visitProperty(declaration)
            return
        }

        val backingFieldExpression = declaration.backingField?.initializer?.expression ?: error("Backing fiekd is null") // FIXME
        val delegateClass = when (backingFieldExpression) {
            is IrConstructorCall -> backingFieldExpression.annotationClass
            else -> TODO("Backing field was initialize not with constructor call")
        }


        val getValue = delegateClass.symbol.getSimpleFunction("getValue")?.owner
        val setValue = delegateClass.symbol.getSimpleFunction("setValue")?.owner

        val usedInGet = getValue?.let { KPropertyUsageFuncAnalyzer(getValue).used } ?: false
        val usedInSet = setValue?.let { KPropertyUsageFuncAnalyzer(setValue).used } ?: false



        if (!usedInGet && !usedInSet) {
//            val newGet = delegateClass.factory.buildFun {
//                updateFrom(getValue!!)
//                name = Name.identifier("newGetValue")
//                origin = IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER // не понимаю что это
//                this.isFakeOverride = true
//                modality = Modality.FINAL
//                isExternal = false
//                isTailrec = false
//                visibility = DescriptorVisibilities.PUBLIC
//                returnType =
//            }
            delegateClass.addFunction {
                name = Name.identifier("newGetValue")
                returnType = getValue!!.returnType
            }.apply {
                body = IrFactoryImpl.createBlockBody(startOffset, endOffset, emptyList())
                valueParameters
            }
//            delegateClass.addMember(newGet)
        }


        super.visitProperty(declaration)
    }
}

private class KPropertyUsageFuncAnalyzer(val function: IrFunction) : IrElementVisitorVoid {
    var used = false
        private set

    private val propertySymbol = function.valueParameters.lastOrNull() ?: error("") // FIXME почему last сломался?

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


private class PropertyDelegationTransformer(val context: JvmBackendContext) : IrElementTransformerVoidWithContext() {
//    override fun <T> visitConst(expression: IrConst<T>): IrExpression {
//        if (expression.kind is IrConstKind.String) {
//            return context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset).run {
//                irString(expression.value.toString() + " hi")
//            }
//        }
//        return expression
//    }
}