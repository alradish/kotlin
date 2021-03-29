package pvar

open class PVar<T>(private var value: T) {
    protected operator fun getValue(thisRef: Any?, prop: Any?) = value

    protected operator fun setValue(thisRef: Any?, prop: Any?, newValue: T) {
        value = newValue
    }
}

class C : PVar<Long>(42L) {
    inner class Inner {
        var x by this@C
    }
}

// 1 FAIL