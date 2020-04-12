/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.gradle

import org.gradle.api.NamedDomainObjectCollectionSchema
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.HasPublicType
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import java.io.Serializable

data class ProjectSchemaEntry<out T>(
    val target: T,
    val name: String,
    val type: T
) : Serializable {

    fun <U> map(f: (T) -> U) =
        ProjectSchemaEntry(f(target), name, f(type))
}

val Class<*>.firstNonSyntheticOrNull: Class<*>?
    get() = takeIf { !isSynthetic } ?: superclass?.firstNonSyntheticOrNull
val java.lang.Class<*>.firstNonSyntheticOrSelf
    get() = firstNonSyntheticOrNull ?: this

inline fun <reified T> typeOf(): TypeOf<T> =
    object : TypeOf<T>() {}
//    TypeOf.typeOf(T::class.java)

fun kotlinTypeStringFor(type: TypeOf<*>): String = type.run {
    when {
        isArray ->
            "Array<${kotlinTypeStringFor(componentType!!)}>"
        isParameterized ->
            "$parameterizedTypeDefinition<${actualTypeArguments.joinToString(transform = ::kotlinTypeStringFor)}>"
        isWildcard ->
//            (upperBound ?: lowerBound)?.let(::kotlinTypeStringFor) ?: "Any"
            (upperBound)?.let(::kotlinTypeStringFor) ?: "Any"
        else ->
            toString().let { primitiveTypeStrings[it] ?: it }
    }
}


internal
val primitiveTypeStrings =
    mapOf(
        "java.lang.Object" to "Any",
        "java.lang.String" to "String",
        "java.lang.Character" to "Char",
        "char" to "Char",
        "java.lang.Boolean" to "Boolean",
        "boolean" to "Boolean",
        "java.lang.Byte" to "Byte",
        "byte" to "Byte",
        "java.lang.Short" to "Short",
        "short" to "Short",
        "java.lang.Integer" to "Int",
        "int" to "Int",
        "java.lang.Long" to "Long",
        "long" to "Long",
        "java.lang.Float" to "Float",
        "float" to "Float",
        "java.lang.Double" to "Double",
        "double" to "Double"
    )


val primitiveKotlinTypeNames = primitiveTypeStrings.values.toHashSet()

class MySchemaType(value: TypeOf<*>) : Serializable {
    val value = kotlinTypeStringFor(value)
    val kotlinString = kotlinTypeStringFor(value)

    override fun toString(): String = kotlinString
}

data class SchemaType(val value: TypeOf<*>) : Serializable {

//    companion object {
//        inline fun <reified T> of() = SchemaType(typeOf<T>())
//    }

    val kotlinString = kotlinTypeStringFor(value)

    override fun toString(): String = kotlinString
}

interface ContainerData : Serializable {
    val name: String
    val target: String
    val type: String
}

typealias TypedProjectSchema = ProjectSchema<SchemaType>

data class ConfigurationEntry<T>(
    val target: T,
    val dependencyDeclarationAlternatives: List<String> = listOf()
) : Serializable {

    fun hasDeclarationDeprecations() = dependencyDeclarationAlternatives.isNotEmpty()

    fun <U> map(f: (T) -> U) =
        ConfigurationEntry(f(target), dependencyDeclarationAlternatives)
}

data class ProjectSchema<out T>(
    val extensions: List<ProjectSchemaEntry<T>>,
    val conventions: List<ProjectSchemaEntry<T>>,
    val tasks: List<ProjectSchemaEntry<T>>,
    val containerElements: List<ProjectSchemaEntry<T>>
//    val configurations: List<ConfigurationEntry<String>>
) : Serializable {

    fun <U> map(f: (T) -> U) = ProjectSchema(
        extensions.map { it.map(f) },
        conventions.map { it.map(f) },
        tasks.map { it.map(f) },
        containerElements.map { it.map(f) }
//        configurations
    )

    fun isNotEmpty(): Boolean =
        extensions.isNotEmpty()
                || conventions.isNotEmpty()
                || tasks.isNotEmpty()
                || containerElements.isNotEmpty()
//                || configurations.isNotEmpty()
}

internal
data class TargetTypedSchema(
    val extensions: List<ProjectSchemaEntry<TypeOf<*>>>,
    val conventions: List<ProjectSchemaEntry<TypeOf<*>>>,
    val tasks: List<ProjectSchemaEntry<TypeOf<*>>>,
    val containerElements: List<ProjectSchemaEntry<TypeOf<*>>>
)

internal
fun targetSchemaFor(target: Any, targetType: TypeOf<*>): TargetTypedSchema {

    val extensions = mutableListOf<ProjectSchemaEntry<TypeOf<*>>>()
    val conventions = mutableListOf<ProjectSchemaEntry<TypeOf<*>>>()
    val tasks = mutableListOf<ProjectSchemaEntry<TypeOf<*>>>()
    val containerElements = mutableListOf<ProjectSchemaEntry<TypeOf<*>>>()

    fun collectSchemaOf(target: Any, targetType: TypeOf<*>) {
        if (target is ExtensionAware) {
            accessibleContainerSchema(target.extensions.extensionsSchema).forEach { schema ->
                extensions.add(ProjectSchemaEntry(targetType, schema.name, schema.publicType))
                collectSchemaOf(target.extensions.getByName(schema.name), schema.publicType)
            }
        }
        if (target is Project) {
            accessibleConventionsSchema(target.convention.plugins).forEach { (name, type) ->
                conventions.add(ProjectSchemaEntry(targetType, name, type))
                collectSchemaOf(target.convention.plugins[name]!!, type)
            }
            accessibleContainerSchema(target.tasks.collectionSchema).forEach { schema ->
                tasks.add(ProjectSchemaEntry(typeOfTaskContainer, schema.name, schema.publicType))
            }
            collectSchemaOf(target.dependencies, typeOfDependencyHandler)
            // WARN eagerly realize all source sets
            sourceSetsOf(target)?.forEach { sourceSet ->
                collectSchemaOf(sourceSet, typeOfSourceSet)
            }
        }
        if (target is NamedDomainObjectContainer<*>) {
            accessibleContainerSchema(target.collectionSchema).forEach { schema ->
                containerElements.add(ProjectSchemaEntry(targetType, schema.name, schema.publicType))
            }
        }
    }

    collectSchemaOf(target, targetType)

    return TargetTypedSchema(
        extensions,
        conventions,
        tasks,
        containerElements
    )
}

//fun accessibleConfigurationsOf(project: Project) =
//    project.configurations
//        .filter { isPublic(it.name) }
//        .map(::toConfigurationEntry)

//fun toConfigurationEntry(configuration: Configuration) = (configuration as DeprecatableConfiguration).run {
//    ConfigurationEntry(name, declarationAlternatives ?: listOf())
//}

fun accessibleContainerSchema(collectionSchema: NamedDomainObjectCollectionSchema) =
    collectionSchema.elements
        .filter { isPublic(it.name) }
        .map(NamedDomainObjectCollectionSchema.NamedDomainObjectSchema::toFirstKotlinPublicOrSelf)

fun inferPublicTypeOfConvention(instance: Any) =
    if (instance is HasPublicType) instance.publicType
    else TypeOf.typeOf(instance::class.java.firstNonSyntheticOrSelf)

fun accessibleConventionsSchema(plugins: Map<String, Any>) =
    plugins.filterKeys(::isPublic).mapValues { inferPublicTypeOfConvention(it.value) }

fun isPublic(name: String): Boolean =
    !name.startsWith("_")

fun NamedDomainObjectCollectionSchema.NamedDomainObjectSchema.toFirstKotlinPublicOrSelf() = this
//                publicType.concreteClass.let { schemaType ->
//                    // Because a public Java class might not correspond necessarily to a
//                    // public Kotlin type due to Kotlin `internal` semantics, we check
//                    // whether the public Java class is also the first public Kotlin type,
//                    // otherwise we compute a new schema entry with the correct Kotlin type.
//                    val firstPublicKotlinType = schemaType.firstPublicKotlinAccessorTypeOrSelf
//                    when {
//                        firstPublicKotlinType === schemaType -> this
//                        else -> ProjectSchemaNamedDomainObjectSchema(
//                            name,
//                            TypeOf.typeOf(firstPublicKotlinType)
//                        )
//                    }
//                }

val typeOfTaskContainer = typeOf<TaskContainer>()
val typeOfProject = typeOf<Project>()
val typeOfSourceSet = typeOf<SourceSet>()
val typeOfDependencyHandler = typeOf<DependencyHandler>()

fun sourceSetsOf(project: Project) =
    project.extensions.findByName("sourceSets") as? SourceSetContainer
