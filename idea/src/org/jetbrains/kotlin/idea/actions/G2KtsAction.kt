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
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Key
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
import org.jetbrains.kotlin.gradle.ContainerData
import org.jetbrains.kotlin.idea.configuration.externalProjectPath
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase
import javax.swing.JComponent

class G2KtsAction : AnAction() {
    companion object {
        val KEY = Key.create<List<ContainerData>>("FOR_ME")
    }

    private fun findGradleProjectStructure(module: Module): DataNode<ProjectData>? {
        val externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
//        val moduleNode = ExternalSystemApiUtil.findParent(sourceSetNode, ProjectKeys.MODULE) ?: continue
        val projectInfo = ExternalSystemUtil.getExternalProjectInfo(module.project, GRADLE_SYSTEM_ID, externalProjectPath) ?: return null
        return projectInfo.externalProjectStructure
    }

    private fun getGradleTasks(file: VirtualFile, e: AnActionEvent): List<Task> {
        val module = ModuleUtilCore.findModuleForFile(file, e.project!!) ?: error("module")
//        module.
        val d = findGradleProjectStructure(module)
        // ExternalSystemApiUtil.findParent(sourceSetNode, ProjectKeys.MODULE) ?: continue
        val t = ExternalSystemApiUtil.find<ModuleData>(d!!, ProjectKeys.MODULE)
//        t?.let {
//            it.con
//        }
        val mm = GradleProjectResolverUtil.findModule(d, module.externalProjectPath!!) ?: error("uuu")
        return mm.children.toList().map { it.data }.filterIsInstance<TaskData>()
            .map { Task(it.name, it.type!!.substringAfterLast('.'), it.linkedExternalProjectPath) }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val project = e.project ?: error("null project")
        val manager = PsiManager.getInstance(project)

        if (!ConvertGroovyGradleScriptDialog().showAndGet()) return

        virtualFiles?.forEach { file ->
            //            val module = ModuleManager.getInstance(project).findModuleByName("project") ?: error("null module")
            val module = ModuleUtilCore.findModuleForFile(file, project) ?: error("null module")
            println("action module name ${module.name}")
            if (module.isDisposed) error("is disposed")

            val groovyFileBase = manager.findFile(file) as? GroovyFileBase ?: return

            val tasks = getGradleTasks(file, e)
            val containerElements = project.getUserData(KEY) ?: emptyList()
            println(containerElements.joinToString(prefix = "\t", separator = "\n") {
                "${it.name}\t${it.target}\t${it.type}"
            })
            val context = GradleBuildContext(
                tasks,
                containerElements.map { org.jetbrains.kotlin.g2kts.transformation.ContainerData(it.name, it.target, it.type) }
            )

            val groovyGradleTree = buildTree(groovyFileBase)
            val gradleTree = GradleTransformer.doApply(listOf(groovyGradleTree.copy()), context).first()
            val kotlinAST = gradleTree.toKotlin()
            val kotlin = (kotlinAST as Node.Block).stmts.joinToString(separator = "\n") { Writer.write(it) }

            val psiDocumentManager = PsiDocumentManager.getInstance(project!!)

//            ExternalSystemApiUtil.findModuleData() TODO ВАЖНО кажется
//            ExternalSystemApiUtil.findModuleData(e.project!!.allModules().first(), )
//            ExternalSystemApiUtil.findParent(sourceSetNode, ProjectKeys.MODULE) ?: continue
//            ExternalSystemApiUtil.findParent(e.project.data)

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
//                groovyFileBase.findExistingEditor()!!.document.apply {
//                    replaceString(0, textLength, kotlin)
//                }
//                groovyFileBase.name = groovyFileBase.name + ".back"
//                groovyFileBase.findExistingEditor()?.document?.apply {
//                    replaceString(0, textLength, kotlin)
//                }
            }
//            println("kotlin")
            /*
            ((((((((manager.findFile(file) as KtFile).script.blockExpression.children.get(1) as KtScriptInitializer).body
            .children.get(1) as KtLambdaArgument)
            .children.get(0).children.get(0) as KtFunctionLiteral)
            .children.get(0).children.get(0) as KtCallExpression).valueArguments.get(0) as KtValueArgument)
            .getArgumentExpression() as KtDotQualifiedExpression).receiverExpression as KtNameReferenceExpression).reference
             */
        }
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