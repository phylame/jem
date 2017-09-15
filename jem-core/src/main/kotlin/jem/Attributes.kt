package jem

import jclp.value.Types
import jclp.value.VariantMap
import jclp.io.getProperties
import jclp.log.Log
import jclp.putAll
import jclp.value.Flob
import jem.util.M
import jclp.value.Text
import java.io.IOException
import java.time.LocalDate
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

const val AUTHOR = "author"
const val COVER = "cover"
const val DATE = "date"
const val GENRE = "genre"
const val INTRO = "intro"
const val ISBN = "isbn"
const val KEYWORDS = "keywords"
const val LANGUAGE = "language"
const val PRICE = "price"
const val PUBDATE = "pubdate"
const val PUBLISHER = "publisher"
const val RIGHTS = "rights"
const val SERIES = "series"
const val STATE = "state"
const val TITLE = "title"
const val VENDOR = "vendor"
const val WORDS = "words"

object Attributes {
    private val types = HashMap<String, String>()

    init {
        initBuiltins()
    }

    fun newAttributes() = VariantMap { name, value ->
        getType(name)?.let {
            require(Types.getClass(it)?.isInstance(value) != false) { "attribute '$name' must be '$it', found '${Types.getType(value)}'" }
        }
    }

    val names get() = types.keys

    fun getType(name: String) = types[name]

    fun mapType(name: String, typeId: String) = types.put(name, typeId)

    fun getName(name: String) = M.optTr("attribute.$name", "")

    private fun initBuiltins() {
        var props: Properties? = null
        try {
            props = getProperties("!jem/attributes.properties")
        } catch (e: IOException) {
            Log.e("Types", e) { "cannot load attribute mapping" }
        }
        if (props == null || props.isEmpty) {
            return
        }
        types.putAll(props)
    }
}

private class AttributeDelegate<T : Any?>(private val default: T) : ReadWriteProperty<Chapter, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Chapter, property: KProperty<*>): T {
        return thisRef.attributes[property.name] as? T ?: default
    }

    override fun setValue(thisRef: Chapter, property: KProperty<*>, value: T) {
        thisRef.attributes[property.name] = value!!
    }
}

var Chapter.title by AttributeDelegate("")
var Chapter.author by AttributeDelegate("")
var Chapter.cover by AttributeDelegate(null as Flob?)
var Chapter.intro by AttributeDelegate(null as Text?)
var Chapter.genre by AttributeDelegate("")
var Chapter.date by AttributeDelegate(null as LocalDate?)
var Chapter.state by AttributeDelegate("")
var Chapter.language by AttributeDelegate(null as Locale?)
var Chapter.publisher by AttributeDelegate("")
var Chapter.rights by AttributeDelegate("")
var Chapter.vendor by AttributeDelegate("")
var Chapter.words by AttributeDelegate("")