/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.expressions.CollectionLiteralKind
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.TransformData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition

internal object CheckCollectionLiteralBuilderStage : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        require(callInfo.callKind is CallKind.CollectionLiteral)
        // TODO проверить что билдер выполняет все условия. Например: тип возвращаемого значения совпадает с классом на котором он объявлен

        val builder = (candidate.symbol.fir as? FirFunction) ?: return sink.reportDiagnostic(InapplicableCandidate)
        val returnTypeClassId = (builder.returnTypeRef as? FirResolvedTypeRef)?.type?.classId ?: return // TODO report Diag
        val receiverClassId = (builder.receiverTypeRef as? FirResolvedTypeRef)?.type?.classId ?: return // TODO report diag
        val classIdIsSame = if (receiverClassId.shortClassName == Name.identifier("Companion")) {
            receiverClassId.relativeClassName.parent().shortName() == returnTypeClassId.shortClassName
        } else {
            TODO("Not companion")
        }
        if (!classIdIsSame) {
            return sink.reportDiagnostic(InapplicableCandidate)
        }
    }
}

internal object CheckCollectionLiteralArgumentsStage : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        require(callInfo.callKind is CallKind.CollectionLiteral)
        candidate.symbol.ensureResolved(FirResolvePhase.STATUS)

        when ((callInfo.callSite as FirCollectionLiteral).kind) {
            CollectionLiteralKind.LIST_LITERAL -> processSeqArguments(candidate, callInfo, sink, context)
            CollectionLiteralKind.MAP_LITERAL -> processDictArguments(candidate, callInfo, sink, context)
        }
    }

    private fun processDictArguments(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val moduleData = candidate.symbol.fir.moduleData
        val (keyType, valueType) = candidate.getKeyValueTypeOfCollectionLiteral()

        val syntheticKeyParameter = buildSyntheticParameter(
            moduleData,
            Name.identifier("syntheticKey"),
            keyType
        )
        val syntheticValueParameter = buildSyntheticParameter(
            moduleData,
            Name.identifier("syntheticValue"),
            valueType
        )
        candidate.resolveExpressions(callInfo.keyExpressions, syntheticKeyParameter, sink, context)
        candidate.resolveExpressions(callInfo.valueExpressions, syntheticValueParameter, sink, context)

    }

    private fun processSeqArguments(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val argumentType = candidate.getValueTypeOfCollectionLiteral()
        val parameterName = Name.identifier("synthetic")
        val syntheticParameter = buildSyntheticParameter(candidate.symbol.fir.moduleData, parameterName, argumentType)
        candidate.resolveExpressions(callInfo.valueExpressions, syntheticParameter, sink, context)
    }

    private fun Candidate.resolveExpressions(
        expressions: List<FirExpression>,
        parameter: FirValueParameter,
        sink: CheckerSink,
        context: ResolutionContext
    ) {
//        if (expressions.isEmpty()) {
//            resolveArgument(
//                callInfo,
//                buildExpressionStub {
//                    typeRef = buildResolvedTypeRef {
//                        type = StandardClassIds.Nothing.defaultType(emptyList())
//                    }
//                },
//                parameter,
//                isReceiver = false,
//                sink = sink,
//                context = context,
//            )
//        }
        for (argument in expressions) {
            resolveArgument(
                callInfo,
                argument,
                parameter,
                isReceiver = false,
                sink = sink,
                context = context,
            )
        }
    }

    private fun buildSyntheticParameter(moduleData: FirModuleData, parameterName: Name, type: ConeKotlinType): FirValueParameter {
        return buildValueParameter {
            this.moduleData = moduleData
            resolvePhase = FirResolvePhase.RAW_FIR // FIXME скорее всего нужно поменять на что-то другое
            origin = FirDeclarationOrigin.Synthetic
            returnTypeRef = buildResolvedTypeRef { this.type = type }
            name = parameterName
            symbol = FirValueParameterSymbol(parameterName)
            isCrossinline = false
            isNoinline = false
            isVararg = true // TODO не уверен нужно ли это
        }
    }
}