package jem.format.epub.v3

import jclp.xml.attribute
import jclp.xml.endTag
import jclp.xml.lang
import jclp.xml.startTag
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
        startTag("", XML_NAMESPACE_OPF, "package")
        attribute("version", "3.0")
        attribute("unique-identifier", uniqueIdentifier)
        prefix.takeIf { it.isNotEmpty() }?.let { attribute("prefix", it) }
        textDirection.takeIf { it.isNotEmpty() }?.let { attribute("dir", it) }
        id.takeIf { it.isNotEmpty() }?.let { attribute("id", it) }
        language.takeIf { it.isNotEmpty() }?.let { lang(it) }

        // <metadata/>
        startTag("dc", XML_NAMESPACE_DCMES, "metadata")
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
        manifest.id.takeIf { it.isNotEmpty() }?.let { attribute("id", it) }
        manifest.items.values.forEach { it.renderTo(this) }
        endTag()

        // <spine/>
        startTag("spine")
        spine.toc.takeIf { it.isNotEmpty() }?.let { attribute("toc", it) }
        spine.pageProgressionDirection.takeIf { it.isNotEmpty() }?.let {
            attribute("page-progression-direction", it)
        }
        spine.id.takeIf { it.isNotEmpty() }?.let { attribute("id", it) }
        spine.refs.forEach { it.renderTo(this) }
        endTag()

        // <bindings/>
        bindings.items.takeIf { it.isNotEmpty() }?.let {
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
        id.takeIf { it.isNotEmpty() }?.let { attribute("id", it) }
        language.takeIf { it.isNotEmpty() }?.let { lang(it) }
        textDirection.takeIf { it.isNotEmpty() }?.let { attribute("dir", it) }
    }
}

private fun DCME.renderTo(xml: XmlSerializer) {
    with(xml) {
        setPrefix("dc", XML_NAMESPACE_DCMES)
        startTag(XML_NAMESPACE_DCMES, this@renderTo.name)
        renderBase(xml)
        text(text)
        endTag()
    }
}

private fun Meta.renderTo(xml: XmlSerializer) {
    with(xml) {
        startTag("meta")
        refines.takeIf { it.isNotEmpty() }?.let { attribute("refines", it) }
        attribute("property", property)
        scheme.takeIf { it.isNotEmpty() }?.let { attribute("scheme", it) }
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
        refines.takeIf { it.isNotEmpty() }?.let { attribute("refines", it) }
        mediaType.takeIf { it.isNotEmpty() }?.let { attribute("media-type", it) }
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
        properties.takeIf { it.isNotEmpty() }?.let { attribute("properties", it) }
        fallback.takeIf { it.isNotEmpty() }?.let { attribute("fallback", it) }
        mediaOverlay.takeIf { it.isNotEmpty() }?.let { attribute("mediaOverlay", it) }
        endTag()
    }
}

private fun ItemRef.renderTo(xml: XmlSerializer) {
    with(xml) {
        startTag("itemref")
        attribute("idref", idref)
        if (!linear) attribute("linear", "no")
        properties.takeIf { it.isNotEmpty() }?.let { attribute("properties", it) }
        id.takeIf { it.isNotEmpty() }?.let { attribute("id", it) }
        endTag()
    }
}
