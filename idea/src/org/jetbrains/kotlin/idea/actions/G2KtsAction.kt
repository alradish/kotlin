/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.actions.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.ui.layout.panel
import kastree.ast.Node
import org.jetbrains.kotlin.g2kts.GradleToKotlin
import org.jetbrains.kotlin.g2kts.KotlinWriter
import org.jetbrains.kotlin.g2kts.GradleBuildContext
import org.jetbrains.kotlin.g2kts.gradleAstBuilder.G2KtsBuilder
import org.jetbrains.kotlin.g2kts.transformation.GradleTransformer
import org.jetbrains.kotlin.gradle.provider.InternalTypedProjectSchema
import org.jetbrains.kotlin.idea.configuration.externalProjectPath
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import javax.swing.Action
import javax.swing.JComponent

//import org.jetbrains.kotlin.idea.configuration.type

class G2KtsAction : AnAction() {
    companion object {
//        val KEY = Key.create<List<ContainerData>>("FOR_ME")
    }

    private fun findGradleProjectStructure(module: Module): DataNode<ProjectData>? {
        val externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
        val projectInfo = ExternalSystemUtil.getExternalProjectInfo(
            module.project,
            GRADLE_SYSTEM_ID,
            externalProjectPath
        ) ?: return null
        return projectInfo.externalProjectStructure
    }

    private fun findModuleDataForFile(file: VirtualFile, project: Project): DataNode<ModuleData> {
        val module =
            ModuleUtilCore.findModuleForFile(file, project) ?: error("null module for file $file")
        val projectData =
            findGradleProjectStructure(module) ?: error("null project structure for module $module")
        @Suppress("UNCHECKED_CAST")
        return projectData.children.find {
            (it.data as? ModuleData)?.id == module.name
        } as? DataNode<ModuleData> ?: error("no module data for this module")
    }

    data class Task(val name: String, val type: String, val target: String)

    private fun getGradleTasks(file: VirtualFile, e: AnActionEvent): List<Task> {
        val module = ModuleUtilCore.findModuleForFile(file, e.project!!) ?: error("module")
        val projectData = findGradleProjectStructure(module)
        val mm = GradleProjectResolverUtil.findModule(
            projectData,
            module.externalProjectPath!!
        ) ?: error("gradle project resolver return null")
        return mm.children.toList().map { it.data }.filterIsInstance<TaskData>()
            .map { Task(it.name, it.type!!.substringAfterLast('.'), it.linkedExternalProjectPath) }
    }


    override fun actionPerformed(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val project = e.project ?: error("null project")
        val manager = PsiManager.getInstance(project)

        if (!ConvertGroovyGradleScriptDialog().showAndGet()) return

        virtualFiles?.forEach { file ->
            val groovyFileBase = manager.findFile(file) as? GroovyFileBase ?: return
            val moduleData = findModuleDataForFile(file, project)

            val key = Key.findKeyByName("TYPED_PROJECT_SCHEMA")
            if (key == null) {
                NeedImportProjectDialog().show()
                return
            }
            @Suppress("UNCHECKED_CAST")
            val internalTypedProjectSchema =
                moduleData.getCopyableUserData(key) as InternalTypedProjectSchema
            val ttt = getGradleTasks(file, e)
            val context = GradleBuildContext(
                internalTypedProjectSchema
            )


            val g2ktsBuilder = G2KtsBuilder(context)
            val gradleTransformer = GradleTransformer(context)
            val gradleTree = gradleTransformer.doApply(g2ktsBuilder.buildTree(groovyFileBase).copy())
            val gradle2kotlin = GradleToKotlin()
            val kotlinAST = with(gradle2kotlin) { gradleTree.toKotlin() }
            val extras = gradle2kotlin.extrasMap
            val kotlin = (kotlinAST as Node.Block).stmts.joinToString(separator = "\n") { KotlinWriter.write(it, extras) }

            val psiDocumentManager = PsiDocumentManager.getInstance(project)

            WriteCommandAction.runWriteCommandAction(project) {
                file.copy(this, file.parent, file.name + ".back")
                try {
                    val newName = file.name + ".kts"
                    file.rename(this, newName)
                } catch (e: NullPointerException) {
                    // TODO why.
                }
                psiDocumentManager.getDocument(groovyFileBase)!!.apply {
                    replaceString(0, textLength, kotlin)
                }
                var processor: AbstractLayoutCodeProcessor = ReformatCodeProcessor(manager.findFile(file)!!, false)
                processor = OptimizeImportsProcessor(processor)
                processor = RearrangeCodeProcessor(processor)
                processor = CodeCleanupCodeProcessor(processor)
                processor.run()

            }

        }
    }
}

class NeedImportProjectDialog : DialogWrapper(true) {
    init {
        init()
        title = "Need to import Gradle project"
    }

    override fun createCenterPanel(): JComponent? {
        return panel {
            row {
                label("Before translation you need to import gradle project")
            }
        }
    }

    override fun createActions(): Array<Action> {
        // TODO Add action which perform importing
        return arrayOf(okAction)
    }

}


class ConvertGroovyGradleScriptDialog : DialogWrapper(true) {
    init {
        init()
        title = "Convert gradle script to kts"
    }

    override fun createCenterPanel(): JComponent? {
        return panel {
            row {
                label("Are you sure you want to convert the current file?")
            }
        }
    }
}