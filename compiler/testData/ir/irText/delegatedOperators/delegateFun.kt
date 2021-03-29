// WITH_RUNTIME

import kotlin.reflect.KProperty
import kotlin.properties.ReadOnlyProperty

class Delegate1: ReadOnlyProperty<Any, String> {
    override operator fun getValue(instance: Any, property: KProperty<*>) = "OK"
}

class Delegate2: ReadOnlyProperty<Any, String> {
    override operator fun getValue(instance: Any, property: KProperty<*>) = property.name
}

fun another(a: Boolean): ReadOnlyProperty<Any, String> {
    return if (a) {
        Delegate1()
    } else {
        Delegate2()
    }
}

fun delegate(a: Boolean): ReadOnlyProperty<Any, String> {
    return if (a) {
        another(!a)
    } else {
        Delegate2()
    }
}


class A {
    val a by delegate(true)
    val b by delegate(false)
}

// 0 INVOKEVIRTUAL Delegate1.getValue \(Ljava/lang/Object;Ljava/lang/String;\)Ljava/lang/String;
// 0 INVOKEVIRTUAL Delegate2.getValue \(Ljava/lang/Object;Ljava/lang/String;\)Ljava/lang/String;
