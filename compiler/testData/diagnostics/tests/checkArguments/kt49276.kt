// WITH_RUNTIME

fun <E> SmartList(x: E) {}
fun <E> SmartList(x: Collection<E>) {}

fun append(x: Any?) {}
fun append(x: Collection<*>) {}

fun append2(x: Iterable<*>) {}
fun append2(x: Collection<*>) {}

class In<in T>(x: T)

@JvmName("append31")
fun append3(x: In<Nothing>) {}
fun append3(x: In<Collection<*>>) {}

fun <E> append4(x: E) {}
fun <E: Collection<*>> append4(x: E) {}

fun <T> takes(range: T) {}
fun <T> takes(range: T) where T : Collection<*>, T: ClosedRange<*> {}

fun main() {
    SmartList(<!PROGRESSIONS_CHANGING_RESOLVE_WARNING("fun <E> SmartList(x: Collection<E>): Unit")!>1..2<!>) // warning
    SmartList<IntRange>(1..10) // no warning

    append(<!PROGRESSIONS_CHANGING_RESOLVE_WARNING("fun append(x: Collection<*>): Unit")!>1..10<!>)    // warning
    append((1..10) as Any) // no warning
    append((1..10) as Iterable<Int>) // no warning
    append("a".."z") // no warning, the range is not iterable
    append(1.0..2.0)

    append2(1..10)    // no warning

    append3(In(1..10))    // no warning

    append4(<!PROGRESSIONS_CHANGING_RESOLVE_WARNING("fun <E : Collection<*>> append4(x: E): Unit")!>1..10<!>)    // warning

    append4<IntRange>(1..10)    // warning

    takes(<!PROGRESSIONS_CHANGING_RESOLVE_WARNING("fun <T : Collection<*>> takes(range: T): Unit where T : ClosedRange<*>")!>1..10<!>)    // warning
}
