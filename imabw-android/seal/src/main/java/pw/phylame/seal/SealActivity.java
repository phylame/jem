package pw.phylame.seal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import lombok.val;
import pw.phylame.android.util.BaseActivity;
import pw.phylame.android.util.IOs;
import pw.phylame.android.util.UIs;

public class SealActivity extends BaseActivity implements AdapterView.OnItemClickListener {
    public static final String RESULT_KEY = "_result_key_";
    public static final String PATH_SEPARATOR = ":";
    public static final String SDCARD_PATH_KEY = "_sdcard_path_key_";
    public static final String OTG_PATH_KEY = "_otg_path_key_";
    public static final String SHOW_ROOT_KEY = "_show_root_key_";
    public static final String CAN_GO_UP_KEY = "_can_go_up_key_";

    static final String MODE_KEY = "_mode_key_";
    static final String MULTIPLE_KEY = "_multiple_key";
    static final String SHOW_HIDDEN_KEY = "_show_hidden_key_";
    static final String PATTERN_KEY = "_pattern_key";

    private boolean showRoot;
    private String sdcardPath;
    private String otgPath;

    public static void choose(Activity activity,
                              int requestCode,
                              boolean forFile,
                              boolean multiple,
                              boolean showHidden,
                              boolean showRoot,
                              CharSequence pattern) {
        val intent = new Intent(activity, SealActivity.class);
        intent.putExtra(MODE_KEY, forFile);
        intent.putExtra(MULTIPLE_KEY, multiple);
        intent.putExtra(SHOW_HIDDEN_KEY, showHidden);
        intent.putExtra(PATTERN_KEY, pattern);
        intent.putExtra(SHOW_ROOT_KEY, showRoot);
        activity.startActivityForResult(intent, requestCode);
    }

    private DeviceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seal);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        val listView = (ListView) findViewById(R.id.list);
        listView.setOnItemClickListener(this);

        val intent = getIntent();
        showRoot = intent.getBooleanExtra(SHOW_ROOT_KEY, false);
        sdcardPath = intent.getStringExtra(SDCARD_PATH_KEY);
        otgPath = intent.getStringExtra(OTG_PATH_KEY);

        listView.setAdapter(adapter = new DeviceAdapter());

        setTitle(R.string.seal_title);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        val item = adapter.getItem(position);
        if (item.available) {
            doChoosing(item.path);
        } else {
            UIs.shortToast(this, getString(R.string.seal_device_prohibit, getString(item.nameId)));
        }
    }

    private void doChoosing(String path) {
        val intent = new Intent(this, FileActivity.class);
        intent.putExtras(getIntent());
        intent.putExtra(FileActivity.PATH_KEY, path);
        startActivityForResult(intent, REQUEST_CODE);
    }

    private static final int REQUEST_CODE = 56789;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            setResult(resultCode, data);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class Item {
        int iconId;
        int nameId;
        String path;
        String details;
        boolean available;

        Item(int iconId, int nameId, String path) {
            this.iconId = iconId;
            this.nameId = nameId;
            this.path = path;
        }
    }

    private class DeviceAdapter extends BaseAdapter {
        private List<Item> items = new ArrayList<>();

        DeviceAdapter() {
            if (TextUtils.isEmpty(sdcardPath)) {
                sdcardPath = IOs.getSDCardPath();
            }
            if (TextUtils.isEmpty(otgPath)) {
                otgPath = IOs.getOTGPath();
            }
            if (showRoot) {
                val item = new Item(R.drawable.ic_device_root, R.string.seal_device_root, "/");
                item.details = getString(R.string.seal_device_root_details);
                item.available = true;
                items.add(item);
            }
            items.add(new Item(
                    R.drawable.ic_device_phone,
                    R.string.seal_device_phone,
                    Environment.getExternalStorageDirectory().getPath()));
            items.add(new Item(R.drawable.ic_device_sdcard, R.string.seal_device_sdcard, sdcardPath));
            items.add(new Item(R.drawable.ic_device_otg, R.string.seal_device_otg, otgPath));
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Item getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null) {
                convertView = View.inflate(SealActivity.this, R.layout.icon_list_item, null);
                holder = new Holder();
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.name = (TextView) convertView.findViewById(R.id.text1);
                holder.details = (TextView) convertView.findViewById(R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }
            val item = items.get(position);
            holder.icon.setImageResource(item.iconId);
            holder.name.setText(item.nameId);
            String details = item.details;
            if (item.details == null) {
                val path = item.path;
                if (TextUtils.isEmpty(path)) {
                    details = getString(R.string.seal_device_unavailable);
                    item.available = false;
                } else if (!IOs.isMounted(path)) {
                    details = getString(R.string.seal_device_unmounted);
                    item.available = false;
                } else {
                    val stat = IOs.statOf(path);
                    details = getString(R.string.seal_device_details, IOs.readableSize(stat.second), IOs.readableSize(stat.first));
                    item.available = true;
                }
            }
            holder.details.setText(details);
            ((ImageView) convertView.findViewById(R.id.option)).setImageResource(R.drawable.ic_arrow);
            return convertView;
        }

        private class Holder {
            ImageView icon;
            TextView name;
            TextView details;
        }
    }
}
