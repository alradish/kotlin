/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.builder.buildLabel
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.diagnostics.ConeNoBuilderForCollectionLiteralOfType
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.VariableWithConstraints
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.util.OperatorNameConventions

class FirCollectionLiteralResolver(
    private val transformer: FirBodyResolveTransformer,
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents
) {
    private val session: FirSession get() = components.session
    private inline val inferenceComponents: InferenceComponents get() = session.inferenceComponents

    private val buildersCache: MutableMap<FirCollectionLiteral, MutableSet<Candidate>> = mutableMapOf()

    fun preprocessCollectionLiteral(collectionLiteral: FirCollectionLiteral, expectedType: FirTypeRef?): FirExpression {
        val builders = components.callResolver.collectAvailableBuildersForCollectionLiteral(collectionLiteral)

        if (builders.isEmpty()) {
            return buildErrorExpression(
                collectionLiteral.source,
                ConeSimpleDiagnostic(
                    "Collection literal has no builders in the current scope",
                    DiagnosticKind.NoBuildersForCollectionLiteralFound
                )
            )
        }

        buildersCache.getOrPut(collectionLiteral, ::mutableSetOf).addAll(builders)

        val types = builders.map { builder ->
            val c = inferenceComponents.createConstraintSystem()
            c.addOtherSystem(builder.system.currentStorage())
            if (collectionLiteral.expressions.isEmpty()) {
                c.addConstraintForArgumentType(collectionLiteral, builder, expectedType)
            }
            fixVariables(c, components.initialTypeOfCandidate(builder))

            c.buildCurrentSubstitutor().safeSubstitute(c, components.initialTypeOfCandidate(builder)) as ConeKotlinType
        }
        val type = ConeTypeIntersector.intersectTypes(
            session.typeContext,
            types
//            builders.map { components.returnTypeCalculator.tryCalculateReturnType(it.symbol as FirCallableSymbol<*>).type }
//            builders.map { components.initialTypeOfCandidate(it) }
        )
        collectionLiteral.resultType = collectionLiteral.resultType.resolvedTypeFromPrototype(type)

        return collectionLiteral
    }

    private fun NewConstraintSystemImpl.addConstraintForArgumentType(
        collectionLiteral: FirCollectionLiteral,
        candidate: Candidate,
        expectedType: FirTypeRef?
    ) {
        fun check(type: ConeKotlinType, initialType: ConeKotlinType) {
            val typeVariable = candidate.substitutor.substituteOrSelf(type)
            addSubtypeConstraintIfCompatible(
                initialType,
                (expectedType as FirResolvedTypeRef).type,
                ConeExpectedTypeConstraintPosition(false)
            )
            fixVariables(this, initialType)
//                        fixVariables(this, expectedType.type)
            if (!currentStorage().fixedTypeVariables.containsKey(typeVariable.typeConstructor())) {
                addNothingConstraint(typeVariable)
            }
        }
        when (expectedType) {
            is FirImplicitTypeRef, null -> addNothingConstraint(collectionLiteral, candidate)
            is FirResolvedTypeRef -> {
                val initialType = components.initialTypeOfCandidate(candidate)
                when (collectionLiteral.kind) {
                    CollectionLiteralKind.LIST_LITERAL -> {
                        val valueType = candidate.getValueTypeOfCollectionLiteral()
                        check(valueType, initialType)
                    }
                    CollectionLiteralKind.MAP_LITERAL -> {
                        val (keyType, valueType) = candidate.getKeyValueTypeOfCollectionLiteral()
                        check(keyType, initialType)
                        check(valueType, initialType)
                    }
                }
            }
            is FirTypeRefWithNullability -> TODO()
        }
    }

    private fun ConstraintSystemBuilder.addNothingConstraint(typeVariable: ConeKotlinType): Boolean {
        return addSubtypeConstraintIfCompatible(
            StandardClassIds.Nothing.defaultType(emptyList()),
            typeVariable,
            SimpleConstraintSystemConstraintPosition
        )
    }

    private fun ConstraintSystemBuilder.addNothingConstraint(collectionLiteral: FirCollectionLiteral, candidate: Candidate): Boolean {
        return when (collectionLiteral.kind) {
            CollectionLiteralKind.LIST_LITERAL -> addNothingConstraint(
                candidate.substitutor.substituteOrSelf(candidate.getValueTypeOfCollectionLiteral())
            )
            CollectionLiteralKind.MAP_LITERAL -> {
                val (key, value) = candidate.getKeyValueTypeOfCollectionLiteral()
                addNothingConstraint(candidate.substitutor.substituteOrSelf(key))
                        || addNothingConstraint(candidate.substitutor.substituteOrSelf(value))
            }
        }
    }

    private fun chooseCandidates(
        collectionLiteral: FirCollectionLiteral,
        expectedType: FirTypeRef,
        otherSystem: NewConstraintSystemImpl? = null
    ): List<Candidate> {
        val acceptable = mutableListOf<Candidate>()
        val type = expectedType.toResolved(collectionLiteral).type
        for (builder in buildersCache.getOrDefault(collectionLiteral, mutableSetOf())) {
            otherSystem?.let {
                builder.system.addOtherSystem(it.currentStorage())
            }
            if (collectionLiteral.expressions.isEmpty()) {
                if (expectedType is FirImplicitTypeRef) {
                    builder.system.addNothingConstraint(collectionLiteral, builder)
                }
            }
            val initialType = components.initialTypeOfCandidate(builder)
            if (builder.system.addSubtypeConstraintIfCompatible(
                    initialType,
                    type,
                    ConeExpectedTypeConstraintPosition(true)
                )
            ) {
                acceptable.add(builder)
            }
        }

        if (acceptable.size == 0) {
            return acceptable
        }

        val min = acceptable.minOf { it.system.notFixedTypeVariables.size }
        return acceptable.filter { it.system.notFixedTypeVariables.size == min }
    }

    fun expandCollectionLiteral(
        collectionLiteral: FirCollectionLiteral,
//        expectedType: ConeKotlinType,
        expectedType: FirTypeRef,
        additionalSystem: NewConstraintSystemImpl? = null
    ): FirExpression {
        val candidates = chooseCandidates(collectionLiteral, expectedType, additionalSystem)

        return when (candidates.size) {
            0 -> return buildErrorExpression(
                collectionLiteral.source,
                ConeNoBuilderForCollectionLiteralOfType(expectedType.toResolved(collectionLiteral).type.render())
            )
            1 -> {
                val candidate = candidates.single()
                if (collectionLiteral.expressions.isEmpty() && expectedType is FirImplicitTypeRef) {
                    candidate.system.addNothingConstraint(collectionLiteral, candidate)
                }
                fixVariables(candidate)
                expandWithCandidate(collectionLiteral, candidate)
            }
            else -> cantChooseBuilder(collectionLiteral)
        }
    }

    private fun fixVariables(candidate: Candidate) {
        fixVariables(candidate.system, components.initialTypeOfCandidate(candidate))
        val currentSubs = candidate.substitutor
        candidate.substitutor = ConeComposedSubstitutor(candidate.csBuilder.buildCurrentSubstitutor() as ConeSubstitutor, currentSubs)
    }

    private fun fixVariables(system: NewConstraintSystemImpl, topLevelType: KotlinTypeMarker) {
        while (true) {
            val variableForFixation = inferenceComponents.variableFixationFinder.findFirstVariableForFixation(
                system,
                system.asConstraintSystemCompletionContext().notFixedTypeVariables.keys.toList(),
                emptyList(),
                ConstraintSystemCompletionMode.FULL,
                topLevelType
            ) ?: break
            if (!variableForFixation.hasProperConstraint)
                break

            val variableWithConstraints = system.notFixedTypeVariables.getValue(variableForFixation.variable)

            fixVariable(system.asConstraintSystemCompletionContext(), topLevelType, variableWithConstraints, emptyList())
        }
    }

    fun expandCollectionLiteralsInCall(functionCall: FirFunctionCall): FirFunctionCall {
        if (!functionCall.argumentList.arguments.any { it is FirCollectionLiteral }) {
            return functionCall
        }
        if (functionCall.calleeReference is FirErrorReferenceWithCandidate) {
            return functionCall
        }

        val candidate = (functionCall.calleeReference as? FirNamedReferenceWithCandidate)?.candidate ?: error("")
        val argumentMapping = candidate.argumentMapping ?: error("cant get argument mapping for $candidate")

        val replacer = object : FirTransformer<Unit>() {
            override fun <E : FirElement> transformElement(element: E, data: Unit): E {
                @Suppress("UNCHECKED_CAST")
                return (element.transformChildren(this, data) as E)
            }

            override fun transformCollectionLiteral(collectionLiteral: FirCollectionLiteral, data: Unit): FirStatement {
                val param = argumentMapping[collectionLiteral]
                    ?: error("Empty argument mapping for $collectionLiteral in ${candidate.symbol.fir.render()}")

                return when (val type = param.returnTypeRef) {
                    is FirImplicitTypeRef -> TODO()
                    is FirResolvedTypeRef -> {
//                        for (builder in buildersCache.getOrDefault(collectionLiteral, mutableSetOf())) {
//                            val initialType = components.initialTypeOfCandidate(builder)
//                            candidate.system.addOtherSystem(builder.system.currentStorage())
//                            if (candidate.system.addSubtypeConstraintIfCompatible(initialType, type.type, SimpleConstraintSystemConstraintPosition)) {
//                                TODO()
//                            } else {
//                                continue
//                            }
//                        }
//                        val clType = checkTypeOfParameter(candidate, type) ?: return cantChooseBuilder(collectionLiteral).also {
//                            argumentMapping[it] = param
//                            argumentMapping.remove(collectionLiteral)
//                        }
//                        expandCollectionLiteral(collectionLiteral, clType.toFirResolvedTypeRef(), candidate.system)
                        expandCollectionLiteral(collectionLiteral, type.type.toFirResolvedTypeRef(), candidate.system)
                    }
                    is FirTypeRefWithNullability -> TODO()
                }.transformSingle(
                    transformer,
                    ResolutionMode.ContextDependent
                ).also {
                    argumentMapping[it] = param
                    argumentMapping.remove(collectionLiteral)
                }
            }
        }
        return functionCall.transformSingle(replacer, Unit)
    }

    fun expandWithCandidate(collectionLiteral: FirCollectionLiteral, candidate: Candidate): FirExpression {
        val adds = collectionLiteral.expressions.map {
            buildFunctionCall {
                calleeReference = buildSimpleNamedReference { name = Name.identifier("add") }
                argumentList = when (collectionLiteral.kind) {
                    CollectionLiteralKind.LIST_LITERAL -> buildUnaryArgumentList(
                        (it as FirCollectionLiteralEntrySingle).expression
                    )
                    CollectionLiteralKind.MAP_LITERAL -> (it as FirCollectionLiteralEntryPair).let { entry ->
                        buildBinaryArgumentList(entry.key, entry.value)
                    }
                }
            }
        }
        val lambda = buildLambdaArgumentExpression {
            expression = buildAnonymousFunctionExpression {
                anonymousFunction = buildAnonymousFunction {
                    origin = FirDeclarationOrigin.Synthetic
                    moduleData = session.moduleData
                    hasExplicitParameterList = false
                    body = buildBlock {
                        statements.addAll(adds)
                    }
                    returnTypeRef = buildImplicitTypeRef()
                    receiverTypeRef = buildImplicitTypeRef()
                    symbol = FirAnonymousFunctionSymbol()
                    isLambda = true
                    label = buildLabel {
                        // TODO check for name of candidate
                        name = when (collectionLiteral.kind) {
                            CollectionLiteralKind.LIST_LITERAL -> OperatorNameConventions.BUILD_LIST_CL
                            CollectionLiteralKind.MAP_LITERAL -> OperatorNameConventions.BUILD_MAP_CL
                        }.identifier
                    }

                }
            }
        }
        val explicitReceiver = buildPropertyAccessExpression {
            calleeReference = buildSimpleNamedReference {
                val fir = (candidate.symbol as FirNamedFunctionSymbol).fir
                name = fir.receiverTypeRef?.let { it.firClassLike(session)?.classId?.relativeClassName?.parent()?.shortName() }
                    ?: fir.dispatchReceiverType?.classId?.relativeClassName?.parent()?.shortName()
                            ?: error("Cant find name for explicit receiver")
            }
        }
        return buildFunctionCall {
            this.explicitReceiver = explicitReceiver
            calleeReference = buildSimpleNamedReference {
                name = when (collectionLiteral.kind) {
                    CollectionLiteralKind.LIST_LITERAL -> OperatorNameConventions.BUILD_LIST_CL
                    CollectionLiteralKind.MAP_LITERAL -> OperatorNameConventions.BUILD_MAP_CL
                }
            }
            val function = candidate.symbol.fir as FirSimpleFunction
            typeArguments.addAll(function.typeParameters.map {
                buildTypeProjectionWithVariance {
                    typeRef = buildResolvedTypeRef {
                        type = candidate.substitutor.substituteOrSelf(it.toConeType())
                    }
                    variance = Variance.INVARIANT
                }
            })
            argumentList = buildBinaryArgumentList(
                buildConstExpression(null, ConstantValueKind.Int, collectionLiteral.expressions.size),
                lambda
            )
        }
    }

    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        topLevelType: KotlinTypeMarker,
        variableWithConstraints: VariableWithConstraints,
        postponedResolveKtPrimitives: List<PostponedResolvedAtom>
    ) {
        val direction = TypeVariableDirectionCalculator(c, postponedResolveKtPrimitives, topLevelType).getDirection(variableWithConstraints)
        val resultType = inferenceComponents.resultTypeResolver.findResultType(c, variableWithConstraints, direction)
        val variable = variableWithConstraints.typeVariable
        c.fixVariable(variable, resultType, ConeFixVariableConstraintPosition(variable)) // TODO: obtain atom for diagnostics
    }

    private fun cantChooseBuilder(cl: FirCollectionLiteral): FirExpression {
        return buildErrorExpression(
            cl.source,
            ConeSimpleDiagnostic(
                "Cant choose builder",
                DiagnosticKind.CantChooseBuilder
            )
        )
    }

    private fun checkTypeOfParameter(candidate: Candidate, type: FirResolvedTypeRef): ConeKotlinType? {
        val substitute = candidate
            .substitutor
            .substituteOrSelf(type.type)
        val typeConstructor = substitute
            .typeConstructor(candidate.system)
        val typeVariable = candidate.system.currentStorage().notFixedTypeVariables[typeConstructor]
        return if (typeVariable != null) {
            val resultType = inferenceComponents.resultTypeResolver.findResultType(
                candidate.system,
                typeVariable,
                TypeVariableDirectionCalculator.ResolveDirection.TO_SUBTYPE
            )
            val intersectionType = resultType as? ConeIntersectionType ?: error("$resultType is not ConeIntersectionType")
            val clType = intersectionType.alternativeType
            if (clType !in intersectionType.intersectedTypes) {
                return null
            }
            clType?.let {
                candidate.system.fixVariable(
                    typeVariable.typeVariable,
                    it,
                    ConeFixVariableConstraintPosition(typeVariable.typeVariable)
                )
            }
            clType!!
        } else substitute
    }

    private fun FirTypeRef.toResolved(collectionLiteral: FirCollectionLiteral): FirResolvedTypeRef = when (this) {
        is FirImplicitTypeRef -> buildResolvedTypeRef {
            type = collectionLiteral.kind.toClassId().constructClassLikeType(arrayOf(ConeStarProjection), false)
        }
        is FirResolvedTypeRef -> this
        is FirTypeRefWithNullability -> TODO()
    }
}