// WITH_RUNTIME
// FILE: Delegate2.kt
import kotlin.reflect.KProperty

class Delegate2 {
    var v: String = ""
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return v
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }
}
// FILE: A.kt

class A {
    var a by Delegate2()
    var b by Delegate2()
    var c by Delegate2()
    var d by Delegate2()
    var e by Delegate2()
    var f by Delegate2()
    var g by Delegate2()
    var h by Delegate2()
    var y by Delegate2()
    var j by Delegate2()
}