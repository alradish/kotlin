// WITH_RUNTIME

fun f(): Int {
    var sum = 0
    for(i in 0..10-1) {
        sum += i
    }
    return sum
}

fun g(): Int {
    var sum = 0
    for(i in 0 until 10) {
        sum += i
    }
    return sum
}