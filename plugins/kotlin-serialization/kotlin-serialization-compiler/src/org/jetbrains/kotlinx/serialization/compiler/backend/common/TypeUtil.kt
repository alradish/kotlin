/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages.internalPackageFqName

open class SerialTypeInfo(
    val property: SerializableProperty,
    val elementMethodPrefix: String,
    val serializer: ClassDescriptor? = null
)

fun AbstractSerialGenerator.findAddOnSerializer(propertyType: KotlinType, module: ModuleDescriptor): ClassDescriptor? {
    additionalSerializersInScopeOfCurrentFile[propertyType.toClassDescriptor to propertyType.isMarkedNullable]?.let { return it }
    if (propertyType in contextualKClassListInCurrentFile)
        return module.getClassFromSerializationPackage(SpecialBuiltins.contextSerializer)
    if (propertyType.toClassDescriptor?.annotations?.hasAnnotation(SerializationAnnotations.polymorphicFqName) == true)
        return module.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer)
    if (propertyType.isMarkedNullable) return findAddOnSerializer(propertyType.makeNotNullable(), module)
    return null
}

fun KotlinType.isGeneratedSerializableObject() =
    toClassDescriptor?.run { kind == ClassKind.OBJECT && hasSerializableAnnotationWithoutArgs } == true

@Suppress("FunctionName", "LocalVariableName")
fun AbstractSerialGenerator.getSerialTypeInfo(property: SerializableProperty): SerialTypeInfo {
    fun SerializableInfo(serializer: ClassDescriptor?) =
        SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", serializer)

    val T = property.type
    property.serializableWith?.toClassDescriptor?.let { return SerializableInfo(it) }
    findAddOnSerializer(T, property.module)?.let { return SerializableInfo(it) }
    T.overridenSerializer?.toClassDescriptor?.let { return SerializableInfo(it) }
    return when {
        T.isTypeParameter() -> SerialTypeInfo(property, if (property.type.isMarkedNullable) "Nullable" else "", null)
        T.isPrimitiveNumberType() or T.isBoolean() -> SerialTypeInfo(
            property,
            T.getJetTypeFqName(false).removePrefix("kotlin.") // i don't feel so good about it...
//          alternative:  KotlinBuiltIns.getPrimitiveType(T)!!.typeName.identifier
        )
        KotlinBuiltIns.isString(T) -> SerialTypeInfo(property, "String")
        KotlinBuiltIns.isNonPrimitiveArray(T.toClassDescriptor!!) -> {
            val serializer = property.serializableWith?.toClassDescriptor ?: property.module.findClassAcrossModuleDependencies(
                referenceArraySerializerId
            )
            SerializableInfo(serializer)
        }
        else -> {
            val serializer =
                findTypeSerializerOrContext(property.module, property.type, property.descriptor.findPsi())
            SerializableInfo(serializer)
        }
    }
}

fun AbstractSerialGenerator.allSealedSerializableSubclassesFor(
    klass: ClassDescriptor,
    module: ModuleDescriptor
): Pair<List<KotlinType>, List<ClassDescriptor>> {
    assert(klass.modality == Modality.SEALED)
    fun recursiveSealed(klass: ClassDescriptor): Collection<ClassDescriptor> {
        return klass.sealedSubclasses.flatMap { if (it.modality == Modality.SEALED) recursiveSealed(it) else setOf(it) }
    }

    val serializableSubtypes = recursiveSealed(klass).map { it.toSimpleType() }
    return serializableSubtypes.mapNotNull { subtype ->
        findTypeSerializerOrContextUnchecked(module, subtype)?.let { Pair(subtype, it) }
    }.unzip()
}

fun KotlinType.serialName(): String {
    val serializableDescriptor = this.toClassDescriptor!!
    return serializableDescriptor.serialName()
}

fun ClassDescriptor.serialName(): String {
    return annotations.serialNameValue ?: fqNameUnsafe.asString()
}

internal val ClassDescriptor.isStaticSerializable: Boolean get() = this.declaredTypeParameters.isEmpty()

/**
 * Returns class descriptor for ContextSerializer or PolymorphicSerializer
 * if [annotations] contains @Contextual or @Polymorphic annotation
 */
fun analyzeSpecialSerializers(
    moduleDescriptor: ModuleDescriptor,
    annotations: Annotations
): ClassDescriptor? = when {
    annotations.hasAnnotation(SerializationAnnotations.contextualFqName) || annotations.hasAnnotation(SerializationAnnotations.contextualOnPropertyFqName) ->
        moduleDescriptor.getClassFromSerializationPackage(SpecialBuiltins.contextSerializer)
    // can be annotation on type usage, e.g. List<@Polymorphic Any>
    annotations.hasAnnotation(SerializationAnnotations.polymorphicFqName) ->
        moduleDescriptor.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer)
    else -> null
}

fun AbstractSerialGenerator.findTypeSerializerOrContextUnchecked(
    module: ModuleDescriptor,
    kType: KotlinType
): ClassDescriptor? {
    val annotations = kType.annotations
    if (kType.isTypeParameter()) return null
    annotations.serializableWith(module)?.let { return it.toClassDescriptor }
    additionalSerializersInScopeOfCurrentFile[kType.toClassDescriptor to kType.isMarkedNullable]?.let { return it }
    if (kType.isMarkedNullable) return findTypeSerializerOrContextUnchecked(module, kType.makeNotNullable())
    if (kType in contextualKClassListInCurrentFile) return module.getClassFromSerializationPackage(SpecialBuiltins.contextSerializer)
    return analyzeSpecialSerializers(module, annotations) ?: findTypeSerializer(module, kType)
}

fun AbstractSerialGenerator.findTypeSerializerOrContext(
    module: ModuleDescriptor,
    kType: KotlinType,
    sourceElement: PsiElement? = null
): ClassDescriptor? {
    if (kType.isTypeParameter()) return null
    return findTypeSerializerOrContextUnchecked(module, kType) ?: throw CompilationException(
        "Serializer for element of type $kType has not been found.\n" +
                "To use context serializer as fallback, explicitly annotate element with @Contextual",
        null,
        sourceElement
    )
}

fun findTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val userOverride = kType.overridenSerializer
    if (userOverride != null) return userOverride.toClassDescriptor
    if (kType.isTypeParameter()) return null
    if (KotlinBuiltIns.isArray(kType)) return module.getClassFromInternalSerializationPackage(SpecialBuiltins.referenceArraySerializer)
    if (kType.isGeneratedSerializableObject()) return module.getClassFromInternalSerializationPackage(SpecialBuiltins.objectSerializer)
    val stdSer = findStandardKotlinTypeSerializer(module, kType) // see if there is a standard serializer
        ?: findEnumTypeSerializer(module, kType)
    if (stdSer != null) return stdSer
    if (kType.isInterface() && kType.toClassDescriptor?.isSealedSerializableInterface == false) return module.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer)
    return kType.toClassDescriptor?.classSerializer // check for serializer defined on the type
}

fun findStandardKotlinTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val name = when (kType.getJetTypeFqName(false)) {
        "Z" -> if (kType.isBoolean()) "BooleanSerializer" else return null
        "B" -> if (kType.isByte()) "ByteSerializer" else return null
        "S" -> if (kType.isShort()) "ShortSerializer" else return null
        "I" -> if (kType.isInt()) "IntSerializer" else return null
        "J" -> if (kType.isLong()) "LongSerializer" else return null
        "F" -> if (kType.isFloat()) "FloatSerializer" else return null
        "D" -> if (kType.isDouble()) "DoubleSerializer" else return null
        "C" -> if (kType.isChar()) "CharSerializer" else return null
        "kotlin.Unit" -> "UnitSerializer"
        "kotlin.Boolean" -> "BooleanSerializer"
        "kotlin.Byte" -> "ByteSerializer"
        "kotlin.Short" -> "ShortSerializer"
        "kotlin.Int" -> "IntSerializer"
        "kotlin.Long" -> "LongSerializer"
        "kotlin.Float" -> "FloatSerializer"
        "kotlin.Double" -> "DoubleSerializer"
        "kotlin.Char" -> "CharSerializer"
        "kotlin.UInt" -> "UIntSerializer"
        "kotlin.ULong" -> "ULongSerializer"
        "kotlin.UByte" -> "UByteSerializer"
        "kotlin.UShort" -> "UShortSerializer"
        "kotlin.String" -> "StringSerializer"
        "kotlin.Pair" -> "PairSerializer"
        "kotlin.Triple" -> "TripleSerializer"
        "kotlin.collections.Collection", "kotlin.collections.List",
        "kotlin.collections.ArrayList", "kotlin.collections.MutableList" -> "ArrayListSerializer"
        "kotlin.collections.Set", "kotlin.collections.LinkedHashSet", "kotlin.collections.MutableSet" -> "LinkedHashSetSerializer"
        "kotlin.collections.HashSet" -> "HashSetSerializer"
        "kotlin.collections.Map", "kotlin.collections.LinkedHashMap", "kotlin.collections.MutableMap" -> "LinkedHashMapSerializer"
        "kotlin.collections.HashMap" -> "HashMapSerializer"
        "kotlin.collections.Map.Entry" -> "MapEntrySerializer"
        "kotlin.ByteArray" -> "ByteArraySerializer"
        "kotlin.ShortArray" -> "ShortArraySerializer"
        "kotlin.IntArray" -> "IntArraySerializer"
        "kotlin.LongArray" -> "LongArraySerializer"
        "kotlin.CharArray" -> "CharArraySerializer"
        "kotlin.FloatArray" -> "FloatArraySerializer"
        "kotlin.DoubleArray" -> "DoubleArraySerializer"
        "kotlin.BooleanArray" -> "BooleanArraySerializer"
        "java.lang.Boolean" -> "BooleanSerializer"
        "java.lang.Byte" -> "ByteSerializer"
        "java.lang.Short" -> "ShortSerializer"
        "java.lang.Integer" -> "IntSerializer"
        "java.lang.Long" -> "LongSerializer"
        "java.lang.Float" -> "FloatSerializer"
        "java.lang.Double" -> "DoubleSerializer"
        "java.lang.Character" -> "CharSerializer"
        "java.lang.String" -> "StringSerializer"
        "java.util.Collection", "java.util.List", "java.util.ArrayList" -> "ArrayListSerializer"
        "java.util.Set", "java.util.LinkedHashSet" -> "LinkedHashSetSerializer"
        "java.util.HashSet" -> "HashSetSerializer"
        "java.util.Map", "java.util.LinkedHashMap" -> "LinkedHashMapSerializer"
        "java.util.HashMap" -> "HashMapSerializer"
        "java.util.Map.Entry" -> "MapEntrySerializer"
        else -> return null
    }
    val identifier = Name.identifier(name)
    return module.findClassAcrossModuleDependencies(ClassId(internalPackageFqName, identifier))
        ?: module.findClassAcrossModuleDependencies(ClassId(SerializationPackages.packageFqName, identifier))
}

fun findEnumTypeSerializer(module: ModuleDescriptor, kType: KotlinType): ClassDescriptor? {
    val classDescriptor = kType.toClassDescriptor ?: return null
    return if (classDescriptor.kind == ClassKind.ENUM_CLASS && !classDescriptor.isInternallySerializableEnum())
        module.findClassAcrossModuleDependencies(enumSerializerId)
    else null
}

internal fun KtPureClassOrObject.bodyPropertiesDescriptorsMap(
    bindingContext: BindingContext,
    filterUninitialized: Boolean = true
): Map<PropertyDescriptor, KtProperty> = declarations
    .asSequence()
    .filterIsInstance<KtProperty>()
    // can filter here because it's impossible to create body property w/ backing field w/o explicit delegating or initializing
    .filter { if (filterUninitialized) it.delegateExpressionOrInitializer != null else true }
    .associateBy { (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as? PropertyDescriptor)!! }

internal fun KtPureClassOrObject.primaryConstructorPropertiesDescriptorsMap(bindingContext: BindingContext): Map<PropertyDescriptor, KtParameter> =
    primaryConstructorParameters
        .asSequence()
        .filter { it.hasValOrVar() }
        .associateBy { bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it]!! }

internal fun KtPureClassOrObject.anonymousInitializers() = declarations
    .asSequence()
    .filterIsInstance<KtAnonymousInitializer>()
    .mapNotNull { it.body }
    .toList()
