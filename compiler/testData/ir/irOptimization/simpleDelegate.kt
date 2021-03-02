// WITH_RUNTIME
// FILE: ReadOnlyDelegate.kt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class RODelegate : ReadOnlyProperty<Any?, Int> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 2
    }
}

fun simpleProvide(): ReadOnlyProperty<Any?, Int> = RODelegate()

// FILE: A.kt
class A {
    val i by simpleProvide()
//    val i by RODelegate()
}