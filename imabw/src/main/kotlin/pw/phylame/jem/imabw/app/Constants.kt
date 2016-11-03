/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Imabw.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pw.phylame.jem.imabw.app

import pw.phylame.qaf.ixin.Ixin

// meta data
val APP_NAME = System.getProperty("app.name") ?: "imabw"
val APP_VERSION = System.getProperty("app.version") ?: "3.1.0"
const val DOCUMENT_URL = "https://github.com/phylame/jem"

const val SETTINGS_DIR = "config/"

const val RESOURCE_DIR = "!pw/phylame/jem/imabw/res/"
const val IMAGE_DIR = "gfx"
const val I18N_DIR = "i18n"
const val TRANSLATOR_NAME = "messages"
const val DESIGNER_NAME = "ui/designer.json"

// default config
const val MAX_HISTORY_LIMITS = 28
const val DEFAULT_HISTORY_LIMITS = 18
const val DEFAULT_LAF_THEME = Ixin.DEFAULT_THEME_NAME
const val DEFAULT_ICON_SET = "default"
const val DEFAULT_KEY_BINDINGS = "default"

// view operations
const val SHOW_TOOL_BAR = "showToolbar"
const val LOCK_TOOL_BAR = "lockToolbar"
const val HIDE_TOOL_BAR_TEXT = "hideToolbarText"
const val SHOW_STATUS_BAR = "showStatusbar"
const val SHOW_SIDE_BAR = "showSidebar"

// file operations
const val NEW_FILE = "newFile"
const val OPEN_FILE = "openFile"
const val SAVE_FILE = "saveFile"
const val SAVE_AS_FILE = "saveAsFile"
const val FILE_DETAILS = "viewDetails"

val FILE_COMMANDS = arrayOf(NEW_FILE, OPEN_FILE, SAVE_FILE, SAVE_AS_FILE, FILE_DETAILS)

const val CLEAR_HISTORY = "clearHistory"

// edit operations
const val UNDO = "undo"
const val REDO = "redo"
const val CUT = "cut"
const val COPY = "copy"
const val PASTE = "paste"
const val DELETE = "delete"
const val SELECT_ALL = "selectAll"

val EDIT_COMMANDS = arrayOf(UNDO, REDO, CUT, COPY, PASTE, DELETE, SELECT_ALL)

// find operations
const val GOTO = "goto"
const val FIND = "find"
const val FIND_NEXT = "findNext"
const val FIND_PREVIOUS = "findPrevious"

val FIND_COMMANDS = arrayOf(FIND, FIND_NEXT, FIND_PREVIOUS, GOTO)

// text edit operations
const val REPLACE = "replace"
const val TO_LOWER = "toLower"
const val TO_UPPER = "toUpper"
const val TO_TITLED = "toTitled"
const val TO_CAPITALIZED = "toCapitalized"
const val JOIN_LINES = "joinLines"

val TEXT_COMMANDS = arrayOf(REPLACE, TO_LOWER, TO_UPPER, TO_TITLED, TO_CAPITALIZED, JOIN_LINES)

const val MORE_TOOLS = "moreTools"

// book operations
const val NEW_CHAPTER = "newChapter"
const val INSERT_CHAPTER = "insertChapter"
const val IMPORT_CHAPTER = "importChapter"
const val EXPORT_CHAPTER = "exportChapter"
const val RENAME_CHAPTER = "renameChapter"
const val MERGE_CHAPTER = "mergeChapter"
const val LOCK_CONTENTS = "lockContents"
const val EDIT_ATTRIBUTES = "editAttributes"
const val EDIT_EXTENSIONS = "editExtensions"

val TREE_COMMANDS = arrayOf(NEW_CHAPTER, INSERT_CHAPTER, IMPORT_CHAPTER, EXPORT_CHAPTER, RENAME_CHAPTER, MERGE_CHAPTER, LOCK_CONTENTS, EDIT_ATTRIBUTES, EDIT_EXTENSIONS)

// tab operations
const val GOTO_NEXT_TAB = "nextTab"
const val GOTO_PREVIOUS_TAB = "previousTab"
const val CLOSE_ACTIVE_TAB = "closeActiveTab"
const val CLOSE_OTHER_TABS = "closeOtherTabs"
const val CLOSE_ALL_TABS = "closeAllTabs"
const val CLOSE_UNMODIFIED_TABS = "closeUnmodifiedTabs"

val TAB_COMMANDS = arrayOf(GOTO_NEXT_TAB, GOTO_PREVIOUS_TAB, CLOSE_ACTIVE_TAB, CLOSE_OTHER_TABS, CLOSE_ALL_TABS, CLOSE_UNMODIFIED_TABS)

// application operations
const val ABOUT_APP = "aboutApp"
const val HELP_CONTENTS = "helpContents"
const val EDIT_SETTINGS = "editSettings"
const val EXIT_APP = "exitApp"
