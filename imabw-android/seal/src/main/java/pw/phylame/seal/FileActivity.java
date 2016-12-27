package pw.phylame.seal;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import lombok.val;
import pw.phylame.android.listng.AbstractItem;
import pw.phylame.android.listng.Adapter;
import pw.phylame.android.listng.ListSupport;
import pw.phylame.android.util.BaseActivity;
import pw.phylame.android.util.CheckableButton;
import pw.phylame.android.util.IOs;
import pw.phylame.android.util.UIs;

import static android.text.TextUtils.isEmpty;
import static android.text.TextUtils.join;

public class FileActivity extends BaseActivity implements View.OnClickListener {
    public static final String PATH_KEY = "_path_key_";

    public static final int PROGRESS_LIMIT = 64;

    private boolean forFile = true;
    private boolean multiple = true;
    private boolean showHidden = false;

    private FileList fileList;

    private HorizontalScrollView pathBarHolder;
    private ViewGroup pathBar;
    private float pathBarFontSize;
    private int pathBarPadding;

    private TextView tvDetail;
    private Button btnChoose;
    private CheckableButton btnSelectAll;

    private File initFile;
    private boolean canGoUp = false;
    private FileItem topItem;

    private ItemFilter filter;
    private ItemSorter sorter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        pathBarHolder = (HorizontalScrollView) findViewById(R.id.sv_path_bar);
        pathBar = (ViewGroup) findViewById(R.id.path_bar);
        pathBarFontSize = UIs.dimenFor(getResources(), R.dimen.file_path_bar_font_size);
        pathBarPadding = getResources().getDimensionPixelSize(R.dimen.file_path_bar_margin);

        tvDetail = (TextView) findViewById(R.id.tv_detail);

        btnSelectAll = (CheckableButton) findViewById(R.id.btn_check_all);
        btnSelectAll.normalIconId = R.drawable.ic_checkbox_normal;
        btnSelectAll.selectedIconId = R.drawable.ic_checkbox_selected;
        btnSelectAll.setOnClickListener(this);

        findViewById(R.id.btn_cancel).setOnClickListener(this);
        btnChoose = (Button) findViewById(R.id.btn_choose);
        btnChoose.setOnClickListener(this);

        fetchParams();
        fileList = new FileList((ListView) findViewById(R.id.list), new FileItem(initFile, false));
        fileList.refresh(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file, menu);
        menu.findItem(R.id.action_show_hidden).setChecked(showHidden);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        val id = item.getItemId();
        if (id == R.id.action_home) {
            finish();
        } else if (id == R.id.action_show_hidden) {
            filter.showHidden = !filter.showHidden;
            item.setChecked(!item.isChecked());
            fileList.refresh(true);
        } else if (id == R.id.action_by_name) {
            sorter.sortType = ItemSorter.BY_NAME;
            fileList.refresh(true);
        } else if (id == R.id.action_by_type) {
            sorter.sortType = ItemSorter.BY_TYPE;
            fileList.refresh(true);
        } else if (id == R.id.action_by_size_asc) {
            sorter.sortType = ItemSorter.BY_SIZE_ASC;
            fileList.refresh(true);
        } else if (id == R.id.action_by_size_desc) {
            sorter.sortType = ItemSorter.BY_SIZE_DESC;
            fileList.refresh(true);
        } else if (id == R.id.action_by_date_asc) {
            sorter.sortType = ItemSorter.BY_DATE_ASC;
            fileList.refresh(true);
        } else if (id == R.id.action_by_date_desc) {
            sorter.sortType = ItemSorter.BY_DATE_DESC;
            fileList.refresh(true);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        val id = v.getId();
        if (id == R.id.btn_cancel) {
            finishChoosing(null);
        } else if (id == R.id.btn_choose) {
            finishChoosing(!forFile && !multiple
                    ? Collections.singleton(fileList.getCurrent())
                    : fileList.getSelections());
        } else if (id == R.id.btn_check_all) {
            fileList.toggleAll(!btnSelectAll.isSelected());
        }
    }

    @Override
    public void onBackPressed() {
        val file = fileList.getCurrent().file;
        if (!file.getPath().equals("/") && (!file.equals(initFile) || canGoUp)) {
            fileList.backTop();
        } else {
            super.onBackPressed();
        }
    }

    private void finishChoosing(Collection<FileItem> items) {
        if (items == null) {
            setResult(RESULT_CANCELED);
        } else if (items.size() > 1) {
            val paths = new String[items.size()];
            int i = 0;
            for (val item : items) {
                paths[i++] = item.file.getAbsolutePath();
            }
            val data = new Intent();
            data.putExtra(SealActivity.RESULT_KEY, join(SealActivity.PATH_SEPARATOR, paths));
            setResult(RESULT_OK, data);
        } else {
            val data = new Intent();
            data.setData(Uri.fromFile(items.toArray(new FileItem[1])[0].file));
            setResult(RESULT_OK, data);
        }
        finish();
    }

    private void fetchParams() {
        val intent = getIntent();
        forFile = intent.getBooleanExtra(SealActivity.MODE_KEY, forFile);
        multiple = intent.getBooleanExtra(SealActivity.MULTIPLE_KEY, multiple);
        showHidden = intent.getBooleanExtra(SealActivity.SHOW_HIDDEN_KEY, showHidden);
        canGoUp = intent.getBooleanExtra(SealActivity.CAN_GO_UP_KEY, canGoUp);
        initFile = new File(intent.getStringExtra(PATH_KEY));

        topItem = new FileItem(canGoUp ? null : initFile.getParentFile(), false);

        val regex = intent.getCharSequenceExtra(SealActivity.PATTERN_KEY);
        Pattern pattern = null;
        if (!TextUtils.isEmpty(regex)) {
            pattern = Pattern.compile(regex.toString().replace(".", "\\.").replace("*", ".*"));
        }
        filter = new ItemFilter(forFile, isEmpty(regex) ? null : pattern, showHidden);
        sorter = new ItemSorter();
    }

    private TextView tvTip;

    private TextView getTipView() {
        if (tvTip == null) {
            tvTip = (TextView) findViewById(R.id.tv_tip);
        }
        return tvTip;
    }

    private View progressBar;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean shown) {
        if (progressBar == null) {
            progressBar = findViewById(R.id.fetch_progress);
        }
        UIs.showProgress(this, progressBar, shown);
        if (shown) {
            setTitle(R.string.file_fetching);
        } else if (forFile) {
            setTitle(getResources().getQuantityString(R.plurals.file_title_for_file, multiple ? 0 : 1));
        } else {
            setTitle(getResources().getQuantityString(R.plurals.file_title_for_directory, multiple ? 0 : 1));
        }
    }

    private int imageFor(boolean selected) {
        return selected ? R.drawable.ic_checkbox_selected : R.drawable.ic_checkbox_normal;
    }

    private class ItemHolder {
        ImageView icon;
        TextView name;
        TextView detail;
        ImageView option;
    }

    private class FileItem extends AbstractItem {
        private File file;
        private int count;
        private ItemHolder holder;

        FileItem(File file, boolean selected) {
            this.file = file;
            setSelected(selected);
        }

        @Override
        public boolean isItem() {
            return file.isFile();
        }

        @Override
        public boolean isGroup() {
            return file.isDirectory();
        }

        @Override
        public int size() {
            return count;
        }

        @Override
        @SuppressWarnings("unchecked")
        public FileItem getParent() {
            return new FileItem(file.getParentFile(), false);
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof FileItem && equals(((FileItem) obj).file, file);
        }

        @Override
        public String toString() {
            val name = file.getName();
            return name.isEmpty() ? "/" : name;
        }
    }

    private class FileAdapter extends Adapter<FileItem> {
        FileAdapter(ListSupport<FileItem> list) {
            super(list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ItemHolder holder;
            val item = getItem(position);

            if (convertView == null) {
                convertView = View.inflate(FileActivity.this, R.layout.icon_list_item, null);
                convertView.setTag(holder = new ItemHolder());
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.name = (TextView) convertView.findViewById(R.id.text1);
                holder.name.setMaxLines(2);
                holder.detail = (TextView) convertView.findViewById(R.id.text2);
                holder.option = (ImageView) convertView.findViewById(R.id.option);
                if (multiple) {
                    holder.option.setTag(item);
                    holder.option.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fileList.toggleSelection((FileItem) v.getTag(), null);
                        }
                    });
                }
            } else {
                holder = (ItemHolder) convertView.getTag();
            }
            item.holder = holder;

            val file = item.file;
            holder.name.setText(file.getName());
            val detail = holder.detail;
            val option = holder.option;

            val date = new Date(file.lastModified());
            val count = item.count = IOs.itemsOf(file, filter).length;
            if (file.isDirectory()) {
                holder.icon.setImageResource(count != 0 ? R.drawable.ic_folder : R.drawable.ic_folder_empty);
                detail.setText(getResources().getQuantityString(R.plurals.item_directory_detail, count, count, date));
                if (!forFile && multiple) {
                    option.setImageResource(imageFor(item.isSelected()));
                } else {
                    option.setImageResource(R.drawable.ic_arrow);
                }
            } else {
                holder.icon.setImageResource(R.drawable.ic_file);
                detail.setText(getString(R.string.item_file_detail, IOs.readableSize(file.length()), date));
                if (multiple) {
                    option.setImageResource(imageFor(item.isSelected()));
                } else {
                    option.setImageDrawable(null);
                }
            }

            return convertView;
        }
    }

    private class FileList extends ListSupport<FileItem> {
        private int fileCount;
        private int directoryCount;

        private boolean progressShown;

        FileList(ListView listView, FileItem current) {
            super(listView, true, forFile, multiple, current);
            IOs.sizeSuffix = getString(R.string.item_file_size_suffix);
        }

        @Override
        protected Adapter<FileItem> makeAdapter() {
            return new FileAdapter(this);
        }

        @Override
        protected void makeItems(FileItem current, List<FileItem> items) {
            val files = IOs.itemsOf(current.file, filter);
            current.count = files.length;
            fileCount = directoryCount = 0;
            if (files.length > 0) {
                Arrays.sort(files, sorter);
                for (val file : files) {
                    if (file.isFile()) {
                        ++fileCount;
                    }
                    if (file.isDirectory()) {
                        ++directoryCount;
                    }
                    items.add(new FileItem(file, false));
                }
            }
        }

        @Override
        public void toggleSelection(FileItem item, Boolean selected) {
            super.toggleSelection(item, selected);
            item.holder.option.setImageResource(imageFor(item.isSelected()));
            val count = getSelections().size();
            tvDetail.setText(getResources().getQuantityString(R.plurals.file_selection_detail, count, count));
            if (count != 0) {
                if (forFile) {
                    btnSelectAll.setSelected(count == fileCount);
                } else {
                    btnSelectAll.setSelected(count == directoryCount);
                }
            } else {
                btnSelectAll.setSelected(false);
            }
            btnChoose.setEnabled(count != 0);
        }

        @Override
        protected void onTopReached() {
            setResult(RESULT_CANCELED);
            finish();
        }

        @Override
        protected void onItemChosen(FileItem item) {
            finishChoosing(Collections.singleton(item));
        }

        @Override
        protected void beforeFetching() {
            val current = getCurrent();
            if (current == null || current.size() <= 0 || current.size() > PROGRESS_LIMIT) {
                progressShown = true;
                showProgress(true);
            } else {
                progressShown = false;
            }
        }

        @Override
        protected void afterFetching(Pair<Integer, Integer> position) {
            super.afterFetching(position);
            setPathBar(topItem, pathBar, pathBarHolder, pathBarFontSize, pathBarPadding);
            getTipView().setVisibility(size() == 0 ? View.VISIBLE : View.GONE);
            tvDetail.setText(getResources().getQuantityString(R.plurals.file_selection_detail, 0, 0));
            btnSelectAll.setVisibility(multiple ? View.VISIBLE : View.GONE);
            btnSelectAll.setSelected(false);
            btnChoose.setEnabled(!forFile && !multiple);
            if (progressShown) {
                showProgress(false);
            }
        }
    }
}
