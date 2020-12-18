// FILE: Delegate.kt
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val a = 2
        return "get"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

    }
}

// FILE: A.kt

class A {
    var a by Delegate()
}