import jem.util.Variants

fun main(args: Array<String>) {
    Variants.setDefault("xyz") {
        123
    }
    println(Variants.getDefault("xyz"))
}
