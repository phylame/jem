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
val NAME = System.getProperty("app.name") ?: "imabw"
val VERSION = System.getProperty("app.version") ?: "3.0.0"
const val DOCUMENT = "https://github.com/phylame/jem"

const val SETTINGS_HOME = "config/"

const val RESOURCE_DIR = "!pw/phylame/jem/imabw/res/"
const val IMAGE_DIR = "gfx"
const val I18N_DIR = "i18n"
const val I18N_NAME = "messages"
const val DESIGNER_NAME = "ui/designer.json"

// default config
const val MAX_HISTORY_LIMITS = 28
const val DEFAULT_HISTORY_LIMITS = 18
const val DEFAULT_LAF_THEME = Ixin.DEFAULT_THEME_NAME
const val DEFAULT_ICON_SET = "default"

// viewer operations
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
const val FILE_DETAILS = "fileDetails"
const val CLEAR_HISTORY = "clearHistory"

// edit operations
const val EDIT_UNDO = "editUndo"
const val EDIT_REDO = "editRedo"
const val EDIT_CUT = "editCut"
const val EDIT_COPY = "editCopy"
const val EDIT_PASTE = "editPaste"
const val EDIT_DELETE = "editDelete"
const val EDIT_SELECT_ALL = "editSelectAll"

val EDIT_COMMANDS = arrayOf(EDIT_UNDO, EDIT_REDO, EDIT_CUT, EDIT_COPY, EDIT_PASTE, EDIT_DELETE, EDIT_SELECT_ALL)

// find operations
const val FIND_CONTENT = "findContent"
const val FIND_NEXT = "findNext"
const val FIND_PREVIOUS = "findPrevious"
const val GOTO_POSITION = "gotoPosition"

val FIND_COMMANDS = arrayOf(FIND_CONTENT, FIND_NEXT, FIND_PREVIOUS, GOTO_POSITION)

// text edit operations
const val REPLACE_TEXT = "replaceText"
const val TO_LOWER = "lowerText"
const val TO_UPPER = "upperText"
const val TO_TITLED = "titleText"
const val TO_CAPITALIZED = "capitalizeText"
const val JOIN_LINES = "joinLines"

val TEXT_COMMANDS = arrayOf(REPLACE_TEXT, TO_LOWER, TO_UPPER, TO_TITLED, TO_CAPITALIZED, JOIN_LINES)

// book operations
const val NEW_CHAPTER = "newChapter"
const val INSERT_CHAPTER = "insertChapter"
const val IMPORT_CHAPTER = "importChapter"
const val EXPORT_CHAPTER = "exportChapter"
const val RENAME_CHAPTER = "renameChapter"
const val MERGE_CHAPTER = "mergeChapter"
const val LOCK_CONTENTS = "lockContents"
const val CHAPTER_PROPERTIES = "chapterProperties"
const val BOOK_EXTENSIONS = "bookExtensions"

val TREE_COMMANDS = arrayOf(NEW_CHAPTER, INSERT_CHAPTER, IMPORT_CHAPTER, EXPORT_CHAPTER, RENAME_CHAPTER, MERGE_CHAPTER, LOCK_CONTENTS, CHAPTER_PROPERTIES, BOOK_EXTENSIONS)

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
const val APP_SETTINGS = "appSettings"
const val EXIT_APP = "exitApp"
