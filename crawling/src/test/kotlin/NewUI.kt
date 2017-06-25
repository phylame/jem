import qaf.ixin.x
import qaf.swing.*
import java.awt.*
import java.util.*
import javax.swing.*


class ScalableIcon(val icon: Icon, val keepRatio: Boolean = false) : Icon {
    override fun getIconWidth(): Int = icon.iconWidth

    override fun getIconHeight(): Int = icon.iconHeight

    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
        val width = c.width
        val height = c.height
        val iconWidth = icon.iconWidth
        val iconHeight = icon.iconHeight

        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        if (!keepRatio) {
            g2d.scale((width / iconWidth.toDouble()), (height / iconHeight.toDouble()))
        } else {
            val sx = width / iconWidth.toDouble()
            val sy = height / iconHeight.toDouble();
            val scale = Math.min(sx, sy)
            g2d.scale(scale, scale)
        }
        icon.paintIcon(c, g2d, 0, 0)
    }
}

class Task(val id: Any)

class TaskModel : AbstractListModel<Task>() {
    val tasks = ArrayList<Task>()

    override fun getSize(): Int = tasks.size

    override fun getElementAt(index: Int): Task = tasks[index]
}

class TaskRenderer : JPanel(), ListCellRenderer<Task> {
    init {
        borderLayout {
            west = label(false) {
                preferredSize = 54 x 72
                icon = ScalableIcon(ImageIcon("E:/code/jem/crawling/src/main/resources/jem/crawling/info.png"), true)
            }

            center = label(false) {
                text = "Haha"
            }

            east = pane(false) {
                button {
                    text = "Ok"
                }
            }
        }
    }

    override fun getListCellRendererComponent(list: JList<out Task>?, value: Task?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        return this
    }
}

fun createUI(): JFrame {
    val model = TaskModel()
    for (i in 1..16) {
        model.tasks.add(Task(i))
    }
    return frame {
        center = scrollPane {
            content = JList(model).apply {
                cellRenderer = TaskRenderer()
            }
        }
        size = 400 x 300
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    }
}

fun main(args: Array<String>) {
    createUI().isVisible = true
}
