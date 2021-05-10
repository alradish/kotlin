/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.optimizationFramework

class DSL {
}

class FileMatcher() {

}

class ClassMatcher() {

}

fun matchFile(match: FileMatcher.() -> Unit): FileMatcher {
    return FileMatcher()
}

fun matchClass(match: ClassMatcher.() -> Unit): ClassMatcher {
    return ClassMatcher()
}


fun f() {
    matchFile {
        matchClass {

        }
    }
}