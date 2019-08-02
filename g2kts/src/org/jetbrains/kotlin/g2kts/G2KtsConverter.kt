/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts


import kastree.ast.Node
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.GroovyCodeVisitor
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.*
import org.codehaus.groovy.classgen.BytecodeExpression
import org.codehaus.groovy.transform.sc.transformers.CompareIdentityExpression
import org.codehaus.groovy.transform.sc.transformers.CompareToNullExpression
import org.jetbrains.kotlin.utils.addToStdlib.cast

class G2KtsConverter : GroovyCodeVisitor {
    var debug = false

    fun debug(str: String) {
        if (debug)
            println(str)
    }

    fun convert(ast: ASTNode): Node {
        accept(ast)
        return pop()
    }

    fun accept(ast: ASTNode) {
        ast.visit(this)
    }

    var result: Node? = null

    private fun pop(): Node {
        debug("Converter.pop()")
        val node = result!!
        result = null
        debug("Converter.pop() = $node")
        return node
    }

    private fun push(node: Node) {
        debug("Converter.push(node = [$node])")
        require(result == null)
        result = node
    }

    override fun visitBlockStatement(block: BlockStatement) {
        debug("Converter.visitBlockStatement(statement = [${block.text}])")
        val nodes = block.statements.map(::convert)
        push(Node.Block(nodes.map { node ->
            when (node) {
                is Node.Decl -> Node.Stmt.Decl(node)
                is Node.Expr -> Node.Stmt.Expr(node)
                else -> error("visit block stmt")
            }
        }))
    }

    override fun visitExpressionStatement(expr: ExpressionStatement) {
        debug("Converter.visitExpressionStatement(statement = [${expr.text}])")
        accept(expr.expression)
    }

    override fun visitMethodCallExpression(call: MethodCallExpression) {
        debug("Converter.visitMethodCallExpression(call = [${call.text}])")
        val expr = when (val m = call.method) {
            is ConstantExpression -> Node.Expr.Name(m.text)
            else -> convert(m) as Node.Expr
        }.let {
            val obj = convert(call.objectExpression) as Node.Expr
            if (obj is Node.Expr.Name && obj.name == "this")
                it
            else obj dot it
        }
        val typeArgs = call.genericsTypes?.map { convert(it) as Node.Type } ?: emptyList()

        val args = when (val a = call.arguments) {
            is TupleExpression -> a.expressions.map(::convert)
            else -> listOf(convert(a))
        }.map {
            Node.ValueArg(null, false, it as Node.Expr)
        }.toMutableList()
        val lambda = args.lastOrNull()?.let { arg ->
            val e = arg.expr
            if (e is Node.Expr.Brace) {
                args.remove(arg)
                Node.Expr.Call.TrailLambda(emptyList(), null, e)
            } else {
                null
            }
        }
        push(Node.Expr.Call(expr, typeArgs, args, lambda))
    }

    override fun visitClosureExpression(clos: ClosureExpression) {
        debug("Converter.visitClosureExpression(expression = [${clos.text}])")
        val params = clos.parameters.map { convert(it) as Node.Expr.Brace.Param } // GTODO Нужно будет создавать их тут
        val block = convert(clos.code) as Node.Block
        push(Node.Expr.Brace(params, block))
    }

    override fun visitConstantExpression(const: ConstantExpression) {
        debug("Converter.visitConstantExpression(expression = [${const.value}])")
        push(
            when (val value = const.value) {
                is String -> convertConstString(const)
                else -> Node.Expr.Const(
                    const.text, when (value) {
                        is Boolean -> Node.Expr.Const.Form.BOOLEAN
                        is Char -> Node.Expr.Const.Form.CHAR
                        is Int -> Node.Expr.Const.Form.INT
                        is Float -> Node.Expr.Const.Form.FLOAT
                        else -> Node.Expr.Const.Form.NULL
                    }
                )

            }
        )
    }

    fun convertConstString(str: ConstantExpression): Node.Expr.StringTmpl {
        // GTODO parse esc characters
        val value = str.value as String
        return Node.Expr.StringTmpl(listOf(Node.Expr.StringTmpl.Elem.Regular(value)), str.text.startsWith("\"\"\""))
    }

    override fun visitGStringExpression(str: GStringExpression) {
        debug("Converter.visitGStringExpression(expression = [${str.text}])")
        debug(".strings = ${str.strings}")
        debug(".values = ${str.values}")

        val elems = mutableListOf<Node.Expr.StringTmpl.Elem>()
        val s = str.strings
        val v = str.values
        var si = 0
        if (s.firstOrNull()?.isEmptyStringExpression != true) {
            elems.add(Node.Expr.StringTmpl.Elem.Regular(s[si].text)) // GTODO check esc character
        }
        si++
        for (vi in 0 until v.size) {
            elems.add(Node.Expr.StringTmpl.Elem.LongTmpl(convert(v[vi]) as Node.Expr))
            elems.add(Node.Expr.StringTmpl.Elem.Regular(s[si++].text)) // GTODO check esc character
        }
        push(Node.Expr.StringTmpl(elems, false))
    }

    override fun visitMapExpression(map: MapExpression) {
        debug("Converter.visitMapExpression(expression = [${map.text}])")
        val typeArgs = emptyList<Node.Type?>()
        val args = map.mapEntryExpressions.map { Node.ValueArg(null, false, convert(it) as Node.Expr) }
        val lambda = null
        push(Node.Expr.Call(mapOf, typeArgs, args, lambda))
    }

    override fun visitMapEntryExpression(ent: MapEntryExpression) {
        debug("Converter.visitMapEntryExpression(expression = [${ent.text}])")
        val lhs = convert(ent.keyExpression) as Node.Expr
        val rhs = convert(ent.valueExpression) as Node.Expr
        push(lhs to rhs)
    }

    override fun visitVariableExpression(variable: VariableExpression) {
        debug("Converter.visitVariableExpression(expression = [${variable.text}])")
        push(Node.Expr.Name(variable.name))
    }

    override fun visitListExpression(list: ListExpression) {
        debug("Converter.visitListExpression(expression = [${list.text}])")
        val typeArgs = emptyList<Node.Type?>()
        val args = list.expressions.map { Node.ValueArg(null, false, convert(it) as Node.Expr) }
        val lambda = null
        push(Node.Expr.Call(listOf, typeArgs, args, lambda))
    }

    override fun visitBinaryExpression(bin: BinaryExpression) {
        debug("Converter.visitBinaryExpression(expression = [${bin.text}])")
        when (bin) {
            is CompareToNullExpression -> TODO("comare to null")
            is CompareIdentityExpression -> TODO("compare iden")
            is DeclarationExpression -> TODO("decl expr")
            else -> {
                val lhs = convert(bin.leftExpression) as Node.Expr
                val oper = Node.Expr.BinaryOp.Oper.Token(Node.Expr.BinaryOp.Token.ASSN)
                val rhs = convert(bin.rightExpression) as Node.Expr
                push(Node.Expr.BinaryOp(lhs, oper, rhs))
            }
        }
    }

    override fun visitContinueStatement(statement: ContinueStatement?) {
        debug("Converter.visitContinueStatement(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitArrayExpression(expression: ArrayExpression?) {
        debug("Converter.visitArrayExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitIfElse(ifElse: IfStatement?) {
        debug("Converter.visitIfElse(ifElse = [${ifElse?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitPrefixExpression(expression: PrefixExpression?) {
        debug("Converter.visitPrefixExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitDeclarationExpression(expression: DeclarationExpression?) {
        debug("Converter.visitDeclarationExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitBitwiseNegationExpression(expression: BitwiseNegationExpression?) {
        debug("Converter.visitBitwiseNegationExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitForLoop(forLoop: ForStatement?) {
        debug("Converter.visitForLoop(forLoop = [${forLoop?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitMethodPointerExpression(expression: MethodPointerExpression?) {
        debug("Converter.visitMethodPointerExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitArgumentlistExpression(expression: ArgumentListExpression?) {
        debug("Converter.visitArgumentlistExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitClosureListExpression(closureListExpression: ClosureListExpression?) {
        debug("Converter.visitClosureListExpression(closureListExpression = [${closureListExpression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitThrowStatement(statement: ThrowStatement?) {
        debug("Converter.visitThrowStatement(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitAssertStatement(statement: AssertStatement?) {
        debug("Converter.visitAssertStatement(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitCastExpression(expression: CastExpression?) {
        debug("Converter.visitCastExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitPostfixExpression(expression: PostfixExpression?) {
        debug("Converter.visitPostfixExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitBytecodeExpression(expression: BytecodeExpression?) {
        debug("Converter.visitBytecodeExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitBreakStatement(statement: BreakStatement?) {
        debug("Converter.visitBreakStatement(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitClassExpression(expression: ClassExpression?) {
        debug("Converter.visitClassExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitWhileLoop(loop: WhileStatement?) {
        debug("Converter.visitWhileLoop(loop = [${loop?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitBooleanExpression(expression: BooleanExpression?) {
        debug("Converter.visitBooleanExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitRangeExpression(expression: RangeExpression?) {
        debug("Converter.visitRangeExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitShortTernaryExpression(expression: ElvisOperatorExpression?) {
        debug("Converter.visitShortTernaryExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitCaseStatement(statement: CaseStatement?) {
        debug("Converter.visitCaseStatement(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitTupleExpression(expression: TupleExpression?) {
        debug("Converter.visitTupleExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitDoWhileLoop(loop: DoWhileStatement?) {
        debug("Converter.visitDoWhileLoop(loop = [${loop?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitFieldExpression(expression: FieldExpression?) {
        debug("Converter.visitFieldExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitUnaryMinusExpression(expression: UnaryMinusExpression?) {
        debug("Converter.visitUnaryMinusExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitTernaryExpression(expression: TernaryExpression?) {
        debug("Converter.visitTernaryExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitPropertyExpression(expression: PropertyExpression?) {
        debug("Converter.visitPropertyExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitTryCatchFinally(finally1: TryCatchStatement?) {
        debug("Converter.visitTryCatchFinally(finally1 = [${finally1?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitReturnStatement(statement: ReturnStatement?) {
        debug("Converter.visitReturnStatement(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitStaticMethodCallExpression(expression: StaticMethodCallExpression?) {
        debug("Converter.visitStaticMethodCallExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitConstructorCallExpression(expression: ConstructorCallExpression?) {
        debug("Converter.visitConstructorCallExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitSpreadMapExpression(expression: SpreadMapExpression?) {
        debug("Converter.visitSpreadMapExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitNotExpression(expression: NotExpression?) {
        debug("Converter.visitNotExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitUnaryPlusExpression(expression: UnaryPlusExpression?) {
        debug("Converter.visitUnaryPlusExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitCatchStatement(statement: CatchStatement?) {
        debug("Converter.visitCatchStatement(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitSynchronizedStatement(statement: SynchronizedStatement?) {
        debug("Converter.visitSynchronizedStatement(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitSwitch(statement: SwitchStatement?) {
        debug("Converter.visitSwitch(statement = [${statement?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitSpreadExpression(expression: SpreadExpression?) {
        debug("Converter.visitSpreadExpression(expression = [${expression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun visitAttributeExpression(attributeExpression: AttributeExpression?) {
        debug("Converter.visitAttributeExpression(attributeExpression = [${attributeExpression?.text}])")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}