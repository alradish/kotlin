// WITH_RUNTIME
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class RODelegate : ReadOnlyProperty<Any?, Int> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 2
    }
}

fun simpleProvide(): ReadOnlyProperty<Any?, Int> = RODelegate()

class A {
    val i by simpleProvide()
}


// 0 INVOKEVIRTUAL RODelegate.getValue \(Ljava/lang/Object;Lkotlin/reflect/KProperty;\)I;
// 1 INVOKEVIRTUAL RODelegate.getValue \(Ljava/lang/Object;Ljava/lang/String;\)I