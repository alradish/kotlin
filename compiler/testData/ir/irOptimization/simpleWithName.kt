// WITH_RUNTIME
// FILE: Delegate1.kt
import kotlin.reflect.KProperty

class Delegate1(var v: Int = 0) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return property.name
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        if (property.name == value) {
            v = v + 1
        }
    }
}

// FILE: A.kt

class A {
    var a by Delegate1()
}

