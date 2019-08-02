/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectScript
import org.jetbrains.groovy.compiler.rt.GroovyCompilerWrapper

fun String.canonicalization(debug: Boolean = false): Statement {
    val groovyCompilerWrapper = GroovyCompilerWrapper(mutableListOf(), false)
    val compilationUnit = CompilationUnit().apply {
        configuration = CompilerConfiguration(configuration).apply {
            //            scriptBaseClass = ProjectScript::class.qualifiedName
            scriptBaseClass = Project::class.qualifiedName
        }
        addSource("build.gradle", this@canonicalization)
    }
    groovyCompilerWrapper.compile(compilationUnit, 5)
    return compilationUnit.firstClassNode.getMethod("run", emptyArray()).code
}

enum class MemberType {
    VAR, TASK, EXTENSION
}

val buildscriptBlocks: List<String> = listOf(
    "allprojects",
    "artifacts"

)
val tasks = listOf(
    "assemble",
    "build",
    "buildDependents",
    "buildEnvironment",
    "buildNeeded",
    "check",
    "classes",
    "clean",
    "compileJava",
    "compileTestJava",
    "components",
    "dependencies",
    "dependencyInsight",
    "dependentComponents",
    "g2kts",
    "help",
    "init",
    "jar",
    "javadoc",
    "model",
    "processResources",
    "processTestResources",
    "projects",
    "properties",
    "someTask",
    "tasks",
    "test",
    "testClasses",
    "wrapper"
)
val extensions = listOf(
    "ext",
    "defaultArtifacts",
    "java",
    "sourceSets",
    "g2kts",
    "reporting"
)
val vars = listOf(
    "HELP_TASK",
    "TASKS_TASK",
    "PROJECTS_TASK",
    "STATUS_ATTRIBUTE",
    "DEFAULT_BUILD_FILE",
    "PATH_SEPARATOR",
    "DEFAULT_BUILD_DIR_NAME",
    "GRADLE_PROPERTIES",
    "SYSTEM_PROP_PREFIX",
    "DEFAULT_VERSION",
    "DEFAULT_STATUS",
    "group",
    "version",
    "status"
)