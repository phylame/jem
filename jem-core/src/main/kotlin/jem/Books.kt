package jem

import jem.util.Text

open class Chapter(title: String = "", var text: Text? = null) {

}

open class Book(title: String = "", author: String = "") : Chapter(title) {

}
