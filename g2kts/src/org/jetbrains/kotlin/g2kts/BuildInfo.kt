/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

val taskException: List<String> = listOf(
    "dependencies"
)

val tasks = listOf(
    "assemble" to org.gradle.api.DefaultTask::class,
    "build" to org.gradle.api.DefaultTask::class,
    "buildDependents" to org.gradle.api.DefaultTask::class,
    "buildEnvironment" to org.gradle.api.tasks.diagnostics.BuildEnvironmentReportTask::class,
    "buildNeeded" to org.gradle.api.DefaultTask::class,
    "check" to org.gradle.api.DefaultTask::class,
    "classes" to org.gradle.api.DefaultTask::class,
    "clean" to org.gradle.api.tasks.Delete::class,
    "compileJava" to org.gradle.api.tasks.compile.JavaCompile::class,
    "compileTestJava" to org.gradle.api.tasks.compile.JavaCompile::class,
    "components" to org.gradle.api.reporting.components.ComponentReport::class,
    "dependencies" to org.gradle.api.tasks.diagnostics.DependencyReportTask::class,
    "dependencyInsight" to org.gradle.api.tasks.diagnostics.DependencyInsightReportTask::class,
    "dependentComponents" to org.gradle.api.reporting.dependents.DependentComponentsReport::class,
    "g2kts" to org.gradle.api.DefaultTask::class,
    "help" to org.gradle.configuration.Help::class,
    "init" to org.gradle.buildinit.tasks.InitBuild::class,
    "jar" to org.gradle.api.tasks.bundling.Jar::class,
    "javadoc" to org.gradle.api.tasks.javadoc.Javadoc::class,
    "model" to org.gradle.api.reporting.model.ModelReport::class,
    "processResources" to org.gradle.language.jvm.tasks.ProcessResources::class,
    "processTestResources" to org.gradle.language.jvm.tasks.ProcessResources::class,
    "projects" to org.gradle.api.tasks.diagnostics.ProjectReportTask::class,
    "properties" to org.gradle.api.tasks.diagnostics.PropertyReportTask::class,
    "someTask" to org.gradle.api.DefaultTask::class,
    "tasks" to org.gradle.api.tasks.diagnostics.TaskReportTask::class,
    "test" to org.gradle.api.tasks.testing.Test::class,
    "testClasses" to org.gradle.api.DefaultTask::class,
    "wrapper" to org.gradle.api.tasks.wrapper.Wrapper::class
).toMap().minus(taskException)

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