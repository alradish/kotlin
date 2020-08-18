/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.g2kts

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import java.io.File

abstract class AbstractG2KtsConverterTest : GradleImportingTestCase() {

    open fun doTest(path: String) {
        val file = File(path)
        val content = FileUtil.loadFile(file)
        importProject(content)
    }

}