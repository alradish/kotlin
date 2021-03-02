// WITH_RUNTIME
// FILE: Delegate2.kt
import kotlin.reflect.KProperty

class Delegate2 {
    var v: String = ""
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "$v get + ${property.name}"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }
}
// FILE: A.kt

class A {
    var b by Delegate2()
}