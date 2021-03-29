// WITH_REFLECT
import kotlin.reflect.KProperty

class Delegate1(var v: String = "s") {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val a = v + "$v ${property.toString()}"
        return a
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        v = value
    }
}

class A {
    var a by Delegate1()
    var b by Delegate1()
}

// 2 INVOKEVIRTUAL Delegate1.getValue \(Ljava/lang/Object;Lkotlin/reflect/KProperty;\)Ljava/lang/String;
// 0 INVOKEVIRTUAL Delegate1.getValue \(Ljava/lang/Object;Ljava/lang/String;\)Ljava/lang/String;

// 0 INVOKEVIRTUAL Delegate1.setValue \(Ljava/lang/Object;Lkotlin/reflect/KProperty;Ljava/lang/String;\)V
// 2 INVOKEVIRTUAL Delegate1.setValue \(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;\)V