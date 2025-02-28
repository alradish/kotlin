/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3.test

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.SystemInfoRt
import org.jetbrains.kotlin.kapt3.base.util.isJava9OrLater
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit

interface CustomJdkTestLauncher {
    fun doTestWithJdk11(mainClass: Class<*>, arg: String) {
        if (isJava9OrLater()) return
        KtTestUtil.getJdk11Home().let { doTestCustomJdk(mainClass, arg, it) }
    }

    private fun doTestCustomJdk(mainClass: Class<*>, arg: String, javaHome: File) {
        //TODO unmute after investigation (tests are failing on TeamCity)
        if (SystemInfoRt.isWindows) return

        val javaExe = File(javaHome, "bin/java.exe").takeIf { it.exists() } ?: File(javaHome, "bin/java")
        assert(javaExe.exists()) { "Can't find 'java' executable in $javaHome" }

        val currentJavaHome = System.getProperty("java.home")

        val classpath = collectClasspath(AbstractClassFileToSourceStubConverterTest::class.java.classLoader)
            .filter { it.protocol.lowercase() == "file" && it.path != null && !it.path.startsWith(currentJavaHome) }
            .map { it.path }

        val command = arrayOf(
            javaExe.absolutePath,
            "--illegal-access=warn",
            "-ea",
            "-D${PathManager.PROPERTY_HOME_PATH}=${PathManager.getHomePath()}",
            "-classpath",
            classpath.joinToString(File.pathSeparator),
            mainClass.name,
            arg
        )

        println("Process arguments: [${command.joinToString()}]")

        val process = ProcessBuilder(*command).inheritIO().start()

        process.waitFor(3, TimeUnit.MINUTES)
        if (process.exitValue() != 0) {
            throw AssertionError("Java $javaHome test process exited with exit code ${process.exitValue()} \n")
        }
    }

    private fun collectClasspath(classLoader: ClassLoader?): List<URL> = when (classLoader) {
        is URLClassLoader -> classLoader.urLs.asList() + collectClasspath(classLoader.parent)
        is ClassLoader -> collectClasspath(classLoader.parent)
        else -> emptyList()
    }

}
