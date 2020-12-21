// FILE: Delegate1.kt
import kotlin.reflect.KProperty

class Delegate1 {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val a = 2
        return "get"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

    }
}

// FILE: Delegate2.kt
import kotlin.reflect.KProperty

class Delegate2 {
    var v: String = ""
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "$v get + ${property.name}"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
//        v = value
    }
}


// FILE: A.kt

class A {
    var a by Delegate1()
    var b by Delegate2()
}