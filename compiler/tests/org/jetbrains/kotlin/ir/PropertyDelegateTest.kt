/*
* Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.GenerationUtils.compileFiles
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.types.impl.IrTypeBase
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File


class PropertyDelegateTest : AbstractIrTextTestCase() {

    private var generationState: GenerationState? = null
    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        myEnvironment.configuration.put(JVMConfigurationKeys.IR, true)
        generationState = compileFiles(
            myFiles.psiFiles, myEnvironment, classBuilderFactory,
            NoScopeRecordCliBindingTrace()
        )

        val irModuleLower = (generationState?.codegenFactory as? JvmIrCodegenFactory)?.irModule
        println(
            irModuleLower?.dump(true)
        )
    }

    fun testFile() {
        KotlinTestUtils.runTest(this::doTest, this, "compiler/testData/ir/irOptimization/test.kt")
    }
}

private class DebugVisitor(val withType: Boolean = false) : IrElementVisitorVoid {
    private var deep = 0

    private fun deepPrint(str: String) {
        println("    ".repeat(deep) + str)
    }

    override fun visitElement(element: IrElement) {
        deepPrint(element::class.simpleName.toString())
        acceptChildrenWithDeep(element)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        super.visitConstructorCall(expression)
    }

    override fun <T> visitConst(expression: IrConst<T>) {
        deepPrint((if (withType) "Const: " else "") + expression.value.toString())
        acceptChildrenWithDeep(expression)
    }

    override fun visitClass(declaration: IrClass) {
        val str =
            declaration.kind.toString() + " " + declaration.thisReceiver?.toPString() + ": " + declaration.typeParameters.joinToString { it.toString() }
        deepPrint(str)
        deep += 1
        declaration.declarations.forEach { it.accept(this, null) }
        deep -= 1
    }

    override fun visitConstructor(declaration: IrConstructor) {
        val str = "constructor  " + "(" + declaration.dispatchReceiverParameter?.toPString() + ")|" +
                declaration.extensionReceiverParameter?.toPString() + " " +
                declaration.valueParameters.joinToString(prefix = "(", postfix = ")") { it.toPString() }
        deepPrint(str)
        deep += 1
        declaration.body?.accept(this, null)
        deep -= 1
    }

    override fun visitFunction(declaration: IrFunction) {
        if (declaration.isPropertyAccessor) {
            declaration as IrSimpleFunction
            deepPrint(declaration.correspondingPropertySymbol.toString())
        } else {

            /*

        dispatchReceiverParameter?.accept(visitor, data)
        extensionReceiverParameter?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }

        body?.accept(visitor, data)
             */
            val str = "fun " + "(" + declaration.dispatchReceiverParameter?.toPString() + ")|" +
//                    declaration.extensionReceiverParameter?.toPString() + " " +
                    declaration.name.identifier + " " +
                    declaration.valueParameters.joinToString(prefix = "(", postfix = ")") { it.toPString() }
            deepPrint(str)
        }
        deep += 1
        declaration.body?.accept(this, null)
        deep -= 1
    }

    override fun visitProperty(declaration: IrProperty) {
        super.visitProperty(declaration)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        deepPrint((if (withType) "ValueParameter: " else "") + declaration.toPString())
        acceptChildrenWithDeep(declaration)
    }


    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        super.visitInstanceInitializerCall(expression)
    }

    private fun acceptChildrenWithDeep(element: IrElement) {
        deep += 1
        element.acceptChildrenVoid(this)
        deep -= 1
    }
}

private fun IrValueParameter.toPString(): String {
    return "${name}: ${(type as IrTypeBase).kotlinType}"
}

