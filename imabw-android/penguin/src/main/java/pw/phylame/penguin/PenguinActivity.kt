package pw.phylame.penguin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import pw.phylame.ancotols.*
import java.util.*

class PenguinActivity : BaseActivity(), AdapterView.OnItemClickListener {
    companion object {
        const val RESULT_KEY = "_result_key_"
        const val INIT_PATH_KEY = "_init_path_key_"
        const val DIR_MODE_KEY = "_dir_mode_key_"
        const val MULTIPLE_KEY = "_multiple_key"
        const val PATTERN_KEY = "_pattern_key"
        const val SHOW_ROOT_KEY = "_show_root_key_"
        const val REQUEST_CHOOSE = 56789

        fun perform(invoker: Activity,
                    requestCode: Int,
                    initPath: String? = null,
                    dirMode: Boolean = false,
                    multiple: Boolean = false,
                    pattern: String? = null,
                    showRoot: Boolean = false) {
            invoker.startActivityForResult(PenguinActivity::class.java, requestCode) {
                putExtra(INIT_PATH_KEY, initPath)
                putExtra(DIR_MODE_KEY, dirMode)
                putExtra(MULTIPLE_KEY, multiple)
                putExtra(PATTERN_KEY, pattern)
                putExtra(SHOW_ROOT_KEY, showRoot)
            }
        }
    }

    val adapter: DeviceAdapter by lazy {
        val intent = intent
        DeviceAdapter(this,
                intent.getBooleanExtra(SHOW_ROOT_KEY, false),
                intent.getStringExtra("sdcardPath"),
                intent.getStringExtra("otgPath"))
    }

    var isSingletonDevice = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val device = adapter.devices.filter { it.isAvailable }.singleOrNull()
        if (device != null) { // just one available item
            choose(device)
            isSingletonDevice = true
            return
        }
        isSingletonDevice = true

        setContentView(R.layout.activity_penguin)
        setSupportActionBar(this[R.id.toolbar])

        val list: ListView = this[R.id.list]
        list.adapter = adapter
        list.onItemClickListener = this

        setTitle(R.string.penguin_title)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val device = adapter.getItem(position)
        if (device.isAvailable) {
            choose(device)
        } else {
            shortToast(getString(R.string.unavailable_device, device.name))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CHOOSE) {
            if (resultCode == Activity.RESULT_OK || isSingletonDevice) {
                setResult(resultCode, data)
                finish()
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun choose(device: Device) {
        startActivityForResult(DirectoryActivity::class.java, REQUEST_CHOOSE) {
            putExtras(this@PenguinActivity.intent)
            putExtra(INIT_PATH_KEY, device.path)
            putExtra(DirectoryActivity.DEVICE_NAME_KEY, device.name)
        }
    }

    class Device(val icon: Int, val name: CharSequence, val path: String) {
        var isAvailable: Boolean = path.isNotEmpty()

        var info: CharSequence = ""
    }

    class DeviceAdapter(val ctx: Context, root: Boolean, sdcard: String?, otg: String?) : BaseAdapter() {
        val devices = ArrayList<Device>()

        init {
            if (root) {
                devices.add(Device(R.drawable.ic_device_root,
                        ctx.getString(R.string.device_root),
                        "/"))
            }
            devices.add(Device(R.drawable.ic_device_phone,
                    ctx.getString(R.string.device_phone),
                    Environment.getExternalStorageDirectory().path))
            devices.add(Device(R.drawable.ic_device_sdcard,
                    ctx.getString(R.string.device_sdcard),
                    sdcard.or { IOs.sdcardPath }))
            devices.add(Device(R.drawable.ic_device_otg,
                    ctx.getString(R.string.device_otg),
                    otg.or { IOs.otgPath }))
        }

        override fun getItem(position: Int): Device = devices[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getCount(): Int = devices.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val (view, holder) = reusedView(convertView, ctx, R.layout.icon_list_item, ::ViewHolder)
            val device = devices[position]
            holder.icon1.setImageResource(device.icon)
            holder.text1.text = device.name
            if (device.info.isEmpty()) {
                if (!device.isAvailable) {
                    device.info = ctx.getString(R.string.unavailable_tip)
                } else if (IOs.isUnmounted(device.path)) {
                    device.info = ctx.getString(R.string.unmounted_tip)
                    device.isAvailable = false
                } else {
                    val (total, free) = IOs.statOf(device.path)
                    device.info = ctx.getString(R.string.device_info, IOs.readable(free), IOs.readable(total))
                }
            }
            holder.text2.text = device.info
            if (device.isAvailable) {
                holder.icon2.visibility = View.VISIBLE
                holder.icon2.setImageResource(R.drawable.ic_arrow)
            } else {
                holder.icon2.visibility = View.GONE
            }
            return view
        }

        class ViewHolder(view: View) {
            val icon1: ImageView = view[R.id.icon1]
            val text1: TextView = view[R.id.text1]
            val text2: TextView = view[R.id.text2]
            val icon2: ImageView = view[R.id.icon2]
        }
    }
}
