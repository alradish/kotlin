/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.provider

import org.gradle.api.reflect.TypeOf
import org.gradle.kotlin.dsl.accessors.*
import java.io.Serializable

fun SchemaType.toInternal() = InternalSchemaType.from(value)

fun InternalSchemaType.fromInternal(): SchemaType {
    fun <T> from(v: TypeOf<T>) = SchemaType(object : TypeOf<T>() {})
    return from(value)
}

fun ProjectSchemaEntry<SchemaType>.toInternal() =
    InternalProjectSchemaEntry(target.toInternal(), name, type.toInternal())

fun InternalProjectSchemaEntry<InternalSchemaType>.fromInternal() =
    ProjectSchemaEntry(target.fromInternal(), name, type.fromInternal())

fun <T> ConfigurationEntry<T>.toInternal() =
    InternalConfigurationEntry(target, dependencyDeclarationAlternatives)

fun <T> InternalConfigurationEntry<T>.fromInternal() =
    ConfigurationEntry(target, dependencyDeclarationAlternatives)

fun TypedProjectSchema.toInternal(): InternalTypedProjectSchema {
    return InternalProjectSchema(
        extensions.map { it.toInternal() },
        conventions.map { it.toInternal() },
        tasks.map { it.toInternal() },
        containerElements.map { it.toInternal() },
        configurations.map { it.toInternal() }
    )
}

fun InternalTypedProjectSchema.fromInternal(): TypedProjectSchema {
    return TypedProjectSchema(
        extensions.map { it.fromInternal() },
        conventions.map { it.fromInternal() },
        tasks.map { it.fromInternal() },
        containerElements.map { it.fromInternal() },
        configurations.map { it.fromInternal() }
    )
}

data class InternalSchemaType(val value: TypeOf<*>) : Serializable {
    companion object {
        inline fun <reified T> of() = InternalSchemaType(object : TypeOf<T>(), Serializable {})

        //        inline fun <reified T> of(value: TypeOf<T>) = of<T>()
        fun <T> from(v: TypeOf<T>) = InternalSchemaType(object : TypeOf<T>(), Serializable {})
    }

    val kotlinString = kotlinTypeStringFor(value)

    override fun toString(): String = kotlinString
}

typealias InternalTypedProjectSchema = InternalProjectSchema<InternalSchemaType>


data class InternalProjectSchema<out T>(
    val extensions: List<InternalProjectSchemaEntry<T>>,
    val conventions: List<InternalProjectSchemaEntry<T>>,
    val tasks: List<InternalProjectSchemaEntry<T>>,
    val containerElements: List<InternalProjectSchemaEntry<T>>,
    val configurations: List<InternalConfigurationEntry<String>>
) : Serializable {

    fun <U> map(f: (T) -> U) = InternalProjectSchema(
        extensions.map { it.map(f) },
        conventions.map { it.map(f) },
        tasks.map { it.map(f) },
        containerElements.map { it.map(f) },
        configurations
    )

    fun isNotEmpty(): Boolean =
        extensions.isNotEmpty()
                || conventions.isNotEmpty()
                || tasks.isNotEmpty()
                || containerElements.isNotEmpty()
                || configurations.isNotEmpty()
}

data class InternalProjectSchemaEntry<out T>(
    val target: T,
    val name: String,
    val type: T
) : Serializable {
    fun <U> map(f: (T) -> U) =
        InternalProjectSchemaEntry(f(target), name, f(type))
}

data class InternalConfigurationEntry<T>(
    val target: T,
    val dependencyDeclarationAlternatives: List<String> = listOf()
) : Serializable {

    fun hasDeclarationDeprecations() = dependencyDeclarationAlternatives.isNotEmpty()

    fun <U> map(f: (T) -> U) =
        InternalConfigurationEntry(f(target), dependencyDeclarationAlternatives)
}

