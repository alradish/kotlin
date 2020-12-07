package a
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "get"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

    }
}
class A {
    var a by Delegate()
}