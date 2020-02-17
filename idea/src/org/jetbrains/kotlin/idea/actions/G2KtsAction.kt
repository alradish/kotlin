/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.ui.layout.panel
import kastree.ast.Node
import kastree.ast.Writer
import org.jetbrains.kotlin.g2kts.gradleAstBuilder.buildTree
import org.jetbrains.kotlin.g2kts.toKotlin
import org.jetbrains.kotlin.g2kts.transformation.GradleBuildContext
import org.jetbrains.kotlin.g2kts.transformation.GradleTransformer
import org.jetbrains.kotlin.g2kts.transformation.Task
import org.jetbrains.kotlin.idea.configuration.externalProjectPath
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import javax.swing.JComponent

//import

class G2KtsAction : AnAction() {

    fun findGradleProjectStructure(file: VirtualFile, project: Project) =
        ModuleUtilCore.findModuleForFile(file, project)?.let { findGradleProjectStructure(it) }

    fun findGradleProjectStructure(module: Module): DataNode<ProjectData>? {
        val externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
        val projectInfo = ExternalSystemUtil.getExternalProjectInfo(module.project, GRADLE_SYSTEM_ID, externalProjectPath) ?: return null
        return projectInfo.externalProjectStructure
    }

    fun getGradleTasks(file: VirtualFile, e: AnActionEvent): List<Task> {
        //            val d = findGradleProjectStructure(file, e.project!!)
//            ModuleUtilCore.findModuleForFile(file, e.project!!)?.let { findGradleProjectStructure(it) }
//            val key = Key.findKeyByName("GRADLE_TASKS") ?: Key.create<Map<String, String>>("GRADLE_TASKS").also { println("I LOST MY KEY") }
//            val mydata = e.project?.getUserData(key) as? Map<String, String>
        val module = ModuleUtilCore.findModuleForFile(file, e.project!!) ?: error("module")
        val d = findGradleProjectStructure(module)
        val mm = GradleProjectResolverUtil.findModule(d, module.externalProjectPath!!) ?: error("uuu")
        return mm.children.toList().map { it.data }.filterIsInstance<TaskData>()
            .map { Task(it.name, it.type!!, it.linkedExternalProjectPath) }
//            d.children.find {  }
//            GradleProjectResolverUtil.findTask(d!!, module.externalProjectPath!!, "build")

    }

    override fun actionPerformed(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val manager = PsiManager.getInstance(e.project!!)
        if (!TestDialog().showAndGet()) return
        virtualFiles?.forEach { file ->
            val groovyFileBase = manager.findFile(file) as? GroovyFileBase ?: return

            val tasks = getGradleTasks(file, e)
            val context = GradleBuildContext(tasks)

            val groovyGradleTree = buildTree(groovyFileBase)
            val gradleTree = GradleTransformer.doApply(listOf(groovyGradleTree.copy()), context).first()
            val kotlinAST = gradleTree.toKotlin()
            //val converted = ((buildTree(file) as GProject).toKotlin() as Node.Block).stmts.joinToString(separator = "\n") { Writer.write(it) }
            val kotlin = (kotlinAST as Node.Block).stmts.joinToString(separator = "\n") { Writer.write(it) }
//            println("-----------------")
//            println(Writer.write(groovyGradleTree.toKotlin()))

            val psiDocumentManager = PsiDocumentManager.getInstance(e.project!!)
//            psiDocumentManager.commitDocument(edito)
            WriteCommandAction.runWriteCommandAction(e.project) {
                groovyFileBase.findExistingEditor()?.document?.apply {
                    replaceString(0, textLength, kotlin)
                }
            }
//            runWriteAction {
//                groovyFileBase.findExistingEditor()?.document?.apply {
//                    replaceString(0, textLength, kotlin)
//                }
//            }
//            println(kotlin)

        }
    }
}

class TestDialog : DialogWrapper(true) {
    init {
        init()
        title = "test"
    }

    override fun createCenterPanel(): JComponent? {
        return panel {
            row {
                label("TEST OK")
            }
        }
    }
}