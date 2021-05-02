// WITH_RUNTIME

fun <T> f(a: Array<T>): String {
    var sum = ""
    for(i in 0..a.size - 1) {
        sum += i.toString()
    }
    return sum
}

fun <T> g(a: Array<T>): String {
    var sum = ""
    for(i in 0 until a.size) {
        sum += i.toString()
    }
    return sum
}