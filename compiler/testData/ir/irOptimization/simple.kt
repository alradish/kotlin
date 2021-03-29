// WITH_REFLECT
import kotlin.reflect.KProperty

class Delegate1(var v: String = "s") {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val a = 2
        return v
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

    }
}

class A {
    var a by Delegate1()
}

// 1 CALL 'public final fun getValue (thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>): kotlin.String [operator] declared in <root>.Delegate1' type=kotlin.String origin=null