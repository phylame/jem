package pw.phylame.imabw.activity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.unnamed.b.atv.model.TreeNode

import lombok.`val`
import pw.phylame.imabw.R
import pw.phylame.jem.core.Attributes

class ChapterHolder(context: Context) : TreeNode.BaseNodeViewHolder<ChapterItem>(context) {

    override fun createNodeView(node: TreeNode, value: ChapterItem): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.chapter_item, null, false)

        val chapter = value.chapter
        val icon = view.findViewById(R.id.icon) as ImageView
        //        icon.setImageResource(chapter.isSection() ? android.R.drawable.ic_menu_add : android.R.drawable.ic_menu_delete);

        val text = view.findViewById(R.id.value) as TextView
        text.text = Attributes.getTitle(chapter)

        return view
    }

    override fun getContainerStyle(): Int {
        return R.style.TreeNodeStyleCustom
    }
}
