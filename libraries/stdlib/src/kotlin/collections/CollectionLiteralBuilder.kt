/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

public interface ListCollectionLiteralBuilder<T, E> {
    public abstract fun add(element: E): kotlin.Unit

    public abstract fun build(): T
}

public interface MapCollectionLiteralBuilder<T, K, V> {
    public abstract fun add(key: K, value: V): kotlin.Unit

    public abstract fun build(): T
}