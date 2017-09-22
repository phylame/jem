package test;

import jem.format.epub.Guide
import jem.format.epub.Item
import jem.format.epub.Spine
import java.nio.file.Paths
import java.util.*

class NCXBuilder(dir: String) {
    val root = Paths.get(dir)

    val resources = LinkedHashMap<String, Item>()

    val guides = LinkedList<Guide>()

    val spines = LinkedList<Spine>()
}

fun main(args: Array<String>) {
}
