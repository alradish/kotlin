// WITH_RUNTIME
// FILE: Delegate1.kt
import kotlin.reflect.KProperty

class Delegate1(var v: String = "s") {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val a = 2
        return v
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

// FILE: Mut.kt
import kotlin.reflect.KProperty

class Mut<T>(var _value: T) {

    inline operator fun getValue(thisRef: Any?, kProperty: Any) = _value

    inline operator fun setValue(thisRef: Any?, kProperty: Any, newValue: T) {
        _value = newValue
    }
}


// FILE: ReadOnlyDelegate.kt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class RODelegate : ReadOnlyProperty<Any, Int> {
    override operator fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return 2
    }
}

fun simpleProvide(): ReadOnlyProperty<Any, Int> = RODelegate()

// FILE: Heir.kt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

open class Delegate1H {
    open operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 1
    }
}

class Delegate2H : Delegate1H() {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return property.name.length
    }
}

fun h(): Delegate1H = Delegate2H()

// FILE: A.kt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> delegate(initializer: () -> T): Delegate1 = Delegate1()

var topLevelD by Delegate2()

class A {
//    var a by Delegate1()
//    var b by Delegate2()
//    val c: String by delegate {
//        42
//    }
//    val x: Int by object {
//        operator fun getValue(thisRef: Any?, data: KProperty<*>): Nothing = null!!
//    }

    var delegatedVar by Mut(2)

//    val i by simpleProvide()
//    val h by h()

}