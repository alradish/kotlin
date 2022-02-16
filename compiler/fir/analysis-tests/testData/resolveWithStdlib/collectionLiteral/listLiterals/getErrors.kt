package main

class Test<out T>(a: List<T>){
    companion object  {
        operator fun <T> get(a: Int): Test<T> { return Test(listOf()) }
    }
}

fun main() {
    val a = <!NO_BUILDERS_FOR_COLLECTION_LITERAL!>Test<!> [1] // Not enough information to infer type variable T
    val aa: Test<String> = Test [1]

    val b = <!NO_BUILDERS_FOR_COLLECTION_LITERAL!>Test<String><!> [1] // CL
}
