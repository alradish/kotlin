import kotlin.reflect.KProperty

var log = ""

class UserDataProperty<in R>(val key: String) {
    operator fun getValue(thisRef: R, desc: KProperty<*>) = thisRef.toString() + key

    operator fun setValue(thisRef: R, desc: KProperty<*>, value: String?) { log += "set"}
}


var String.calc: String by UserDataProperty("K")

fun box(): String {
    return "O".calc
}

// 0 INVOKEVIRTUAL UserDataProperty.getValue \(Ljava/lang/Object;Lkotlin/reflect/KProperty;\)Ljava/lang/String;
// 1 INVOKEVIRTUAL UserDataProperty.getValue \(Ljava/lang/Object;Ljava/lang/String;\)Ljava/lang/String;

// 0 INVOKEVIRTUAL UserDataProperty.setValue \(Ljava/lang/Object;Lkotlin/reflect/KProperty;Ljava/lang/String;\)V
// 1 INVOKEVIRTUAL UserDataProperty.setValue \(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;\)V