// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// !LANGUAGE: +InlineClasses

var setterInvoked = 0
var backing = 42

inline class Delegate(val ignored: Int) {

    operator fun getValue(thisRef: Any?, prop: Any?) =
        backing

    operator fun setValue(thisRef: Any?, prop: Any?, newValue: Int) {
        setterInvoked++
        backing = newValue
    }
}

var topLevelD by Delegate(0)

fun box(): String {
    if (topLevelD != 42) AssertionError()

    topLevelD = 1234
    if (topLevelD != 1234) throw AssertionError()
    if (backing != 1234) throw AssertionError()

    if (setterInvoked != 1) throw AssertionError()

    return "OK"
}

// 0 INVOKESTATIC Delegate.getValue-impl \(ILjava/lang/Object;Ljava/lang/Object;\)I
// 1 INVOKESTATIC Delegate.getValue-impl \(ILjava/lang/Object;Ljava/lang/String;\)I

// 0 INVOKESTATIC Delegate.setValue-impl \(Ljava/lang/Object;Lkotlin/reflect/KProperty;I\)V
// 1 INVOKESTATIC Delegate.setValue-impl \(ILjava/lang/Object;Ljava/lang/String;I\)V