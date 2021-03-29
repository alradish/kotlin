// WITH_REFLECT
import kotlin.reflect.KProperty

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>) = "OK"
}

open class Base {
    val x: String by Delegate
}

class Derived : Base()

fun f() {
    val d = Derived()
}


// 0 INVOKEVIRTUAL Delegate.getValue \(Ljava/lang/Object;Lkotlin/reflect/KProperty;\)Ljava/lang/String;
// 1 INVOKEVIRTUAL Delegate.getValue \(Ljava/lang/Object;Ljava/lang/String;\)Ljava/lang/String;

// 0 INVOKEVIRTUAL Delegate1.setValue \(Ljava/lang/Object;Lkotlin/reflect/KProperty;Ljava/lang/String;\)V
// 0 INVOKEVIRTUAL Delegate1.setValue \(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;\)V