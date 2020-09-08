/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts.transformation

import org.jetbrains.kotlin.gradle.provider.InternalProjectSchemaEntry
import org.jetbrains.kotlin.gradle.provider.InternalSchemaType
import org.jetbrains.kotlin.gradle.provider.InternalTypedProjectSchema

class GradleBuildContext(
    val internalTypedProjectSchema: InternalTypedProjectSchema
) {
    fun getTaskByName(name: String): InternalProjectSchemaEntry<InternalSchemaType>? {
        return internalTypedProjectSchema.tasks.find { it.name == name }
    }

}