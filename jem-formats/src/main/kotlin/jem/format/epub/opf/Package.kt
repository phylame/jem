package jem.format.epub.opf

import jclp.text.ifNotEmpty
import jclp.xml.attribute
import jclp.xml.endTag
import jclp.xml.startTag
import jem.format.epub.EPUB
import jem.format.epub.Taggable
import org.xmlpull.v1.XmlSerializer

class Package(val version: String, var uniqueIdentifier: String = "", id: String = "") : Taggable(id) {
    val metadata = Metadata()

    val manifest = Manifest()

    val spine = Spine()

    override fun renderTo(xml: XmlSerializer) {
        with(xml) {
            startTag("", EPUB.XMLNS_OPF, "package")
            attribute("version", version)
            attribute("unique-identifier", uniqueIdentifier)
            id.ifNotEmpty { attribute("id", it) }
            attr.forEach { attribute(it.key, it.value) }
            metadata.renderTo(this)
            manifest.renderTo(this)
            spine.renderTo(this)
            endTag()
        }
    }
}
