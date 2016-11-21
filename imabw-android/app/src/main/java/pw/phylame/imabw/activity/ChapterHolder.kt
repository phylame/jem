package pw.phylame.imabw.activity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.unnamed.b.atv.model.TreeNode

import pw.phylame.imabw.R
import pw.phylame.jem.core.Attributes

class ChapterHolder(context: Context) : TreeNode.BaseNodeViewHolder<ChapterItem>(context) {

    override fun createNodeView(node: TreeNode, value: ChapterItem): View {
        val view = View.inflate(context, R.layout.chapter_item, null)

        val chapter = value.chapter
        val icon = view.findViewById(R.id.icon) as ImageView
        //        icon.setImageResource(chapter.isSection() ? android.R.drawable.ic_menu_add : android.R.drawable.ic_menu_delete);

        val title = view.findViewById(R.id.chapter_title) as TextView
        title.text = Attributes.getTitle(chapter)

        val overview = view.findViewById(R.id.chapter_overview) as TextView
        overview.text = if (chapter.isSection) "${chapter.size()} sub chapters" else Attributes.getIntro(chapter)?.text ?: ""

        return view
    }

    override fun getContainerStyle(): Int {
        return R.style.TreeNodeStyleCustom
    }
}
