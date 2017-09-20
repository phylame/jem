package jem.format.epub

import jclp.setting.Settings
import jclp.vdm.VDMWriter
import jem.Chapter

interface HtmlRender {
    fun renderChapter(chapter: Chapter, writer: VDMWriter, settings: Settings?, data: OpfData): String
}

object SimpleHtmlRender : HtmlRender {
    override fun renderChapter(chapter: Chapter, writer: VDMWriter, settings: Settings?, data: OpfData): String {
        if (chapter.isSection) {
            return renderSection(chapter, writer, settings, data)
        }
        return "x"
    }

    private fun renderSection(chapter: Chapter, writer: VDMWriter, settings: Settings?, data: OpfData): String {
        return ""
    }
}