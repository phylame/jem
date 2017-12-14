package jem.format.epub.v3

import jclp.ifNotEmpty
import jclp.text.ifNotEmpty
import jclp.xml.attribute
import jclp.xml.endTag
import jclp.xml.lang
import jclp.xml.startTag
import jem.format.epub.EPUB
import org.xmlpull.v1.XmlSerializer

class Package(val id: String = "", manifestId: String = "", spineId: String = "") {
    var uniqueIdentifier: String = ""

    var prefix: String = ""

    var language: String = ""

    var textDirection: String = ""

    val metadata = Metadata()

    val manifest = Manifest(manifestId)

    val spine = Spine(spineId)

    val bindings = Bindings()
}

fun Package.renderTo(xml: XmlSerializer) {
    with(xml) {
        // <package/>
        startTag("", EPUB.XMLNS_OPF, "package")
        attribute("version", "3.0")
        attribute("unique-identifier", uniqueIdentifier)
        prefix.ifNotEmpty { attribute("prefix", it) }
        textDirection.ifNotEmpty { attribute("dir", it) }
        id.ifNotEmpty { attribute("id", it) }
        language.ifNotEmpty { lang(it) }

        // <metadata/>
        startTag("dc", EPUB.XMLNS_DCME, "metadata")
        metadata.items.values.forEach {
            when (it) {
                is DCME -> it.renderTo(this)
                is Meta -> it.renderTo(this)
                is Link -> it.renderTo(this)
            }
        }
        endTag()

        // <manifest/>
        startTag("manifest")
        manifest.id.ifNotEmpty { attribute("id", it) }
        manifest.items.values.forEach { it.renderTo(this) }
        endTag()

        // <spine/>
        startTag("spine")
        spine.toc.ifNotEmpty { attribute("toc", it) }
        spine.pageProgressionDirection.ifNotEmpty {
            attribute("page-progression-direction", it)
        }
        spine.id.ifNotEmpty { attribute("id", it) }
        spine.refs.forEach { it.renderTo(this) }
        endTag()

        // <bindings/>
        bindings.items.ifNotEmpty {
            startTag("bindings")
            it.forEach {
                startTag("mediaType")
                attribute("handler", it.handler)
                attribute("media-type", it.mediaType)
                endTag()
            }
            endTag()
        }

        endTag()
    }
}

private fun Item.renderBase(xml: XmlSerializer) {
    with(xml) {
        id.ifNotEmpty { attribute("id", it) }
        language.ifNotEmpty { lang(it) }
        textDirection.ifNotEmpty { attribute("dir", it) }
    }
}

private fun DCME.renderTo(xml: XmlSerializer) {
    with(xml) {
        setPrefix("dc", EPUB.XMLNS_DCME)
        startTag(EPUB.XMLNS_DCME, this@renderTo.name)
        renderBase(xml)
        text(text)
        endTag()
    }
}

private fun Meta.renderTo(xml: XmlSerializer) {
    with(xml) {
        startTag("meta")
        refines.ifNotEmpty { attribute("refines", it) }
        attribute("property", property)
        scheme.ifNotEmpty { attribute("scheme", it) }
        renderBase(xml)
        text(text)
        endTag()
    }
}

private fun Link.renderTo(xml: XmlSerializer) {
    with(xml) {
        startTag("link")
        attribute("rel", rel)
        attribute("href", href)
        refines.ifNotEmpty { attribute("refines", it) }
        mediaType.ifNotEmpty { attribute("media-type", it) }
        renderBase(xml)
        endTag()
    }
}

private fun Resource.renderTo(xml: XmlSerializer) {
    with(xml) {
        startTag("item")
        attribute("id", id)
        attribute("href", href)
        attribute("media-type", mediaType)
        properties.ifNotEmpty { attribute("properties", it) }
        fallback.ifNotEmpty { attribute("fallback", it) }
        mediaOverlay.ifNotEmpty { attribute("mediaOverlay", it) }
        endTag()
    }
}

private fun ItemRef.renderTo(xml: XmlSerializer) {
    with(xml) {
        startTag("itemref")
        attribute("idref", idref)
        if (!linear) attribute("linear", "no")
        properties.ifNotEmpty { attribute("properties", it) }
        id.ifNotEmpty { attribute("id", it) }
        endTag()
    }
}
