package pw.phylame.seal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Pattern;

import lombok.val;

import static android.text.TextUtils.isEmpty;
import static android.text.TextUtils.join;

public class FileActivity extends BaseActivity implements View.OnClickListener {
    public static final String PATH_KEY = "_path_key_";

    public static final int PROGRESS_LIMIT = 64;

    /* package */ boolean forFile = true;
    /* package */ boolean multiple = true;
    private boolean showHidden = false;

    private int fileCount, dirCount;

    FileView fileView;

    private TextView tvPath;
    private TextView tvDetail;
    private Button btnChoose;
    private MenuItem miShowHidden;
    private CheckedImageButton btnCheckAll;

    private File initFile;
    private boolean canGoUp = true;

    private ItemFilter filter;
    private ItemSorter sorter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initParams();

        setContentView(R.layout.activity_file);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        tvPath = (TextView) findViewById(R.id.tv_path);

        fileView = new FileView((ListView) findViewById(R.id.list), true, null);

        tvDetail = (TextView) findViewById(R.id.tv_detail);

        btnCheckAll = (CheckedImageButton) findViewById(R.id.btn_check_all);
        btnCheckAll.normalIconId = R.drawable.ic_checkbox_normal;
        btnCheckAll.selectedIconId = R.drawable.ic_checkbox_selected;
        btnCheckAll.setOnClickListener(this);

        findViewById(R.id.btn_cancel).setOnClickListener(this);
        btnChoose = (Button) findViewById(R.id.btn_choose);
        btnChoose.setOnClickListener(this);

        fileView.current = new FileItem(initFile, false);
        fileView.refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file, menu);
        miShowHidden = menu.findItem(R.id.action_show_hidden);
        miShowHidden.setChecked(showHidden);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        val id = item.getItemId();
        if (id == R.id.action_show_hidden) {
            filter.showHidden = !filter.showHidden;
            miShowHidden.setChecked(!miShowHidden.isChecked());
            fileView.refresh();
            return true;
        } else if (id == R.id.action_by_name) {
            sorter.sortType = ItemSorter.BY_NAME;
            fileView.refresh();
            return true;
        } else if (id == R.id.action_by_type) {
            sorter.sortType = ItemSorter.BY_TYPE;
            fileView.refresh();
            return true;
        } else if (id == R.id.action_by_size_asc) {
            sorter.sortType = ItemSorter.BY_SIZE_ASC;
            fileView.refresh();
            return true;
        } else if (id == R.id.action_by_size_desc) {
            sorter.sortType = ItemSorter.BY_SIZE_DESC;
            fileView.refresh();
            return true;
        } else if (id == R.id.action_by_date_asc) {
            sorter.sortType = ItemSorter.BY_DATE_ASC;
            fileView.refresh();
            return true;
        } else if (id == R.id.action_by_date_desc) {
            sorter.sortType = ItemSorter.BY_DATE_DESC;
            fileView.refresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        val id = v.getId();
        if (id == R.id.btn_cancel) {
            finishChoosing(null);
        } else if (id == R.id.btn_choose) {
            finishChoosing(!forFile && !multiple ? Collections.singleton(fileView.current) : fileView.selections);
        } else if (id == R.id.btn_check_all) {
            val selected = !btnCheckAll.isSelected();
            for (val item : fileView.items()) {
                if (forFile) {
                    if (item.file.isFile()) {
                        fileView.toggleSelection(item, selected);
                    }
                } else if (item.file.isDirectory()) {
                    fileView.toggleSelection(item, selected);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!fileView.current.file.equals(initFile) && canGoUp) {
            fileView.back();
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

    private void initParams() {
        val intent = getIntent();
        forFile = intent.getBooleanExtra(SealActivity.MODE_KEY, forFile);
        multiple = intent.getBooleanExtra(SealActivity.MULTIPLE_KEY, multiple);
        showHidden = intent.getBooleanExtra(SealActivity.SHOW_HIDDEN_KEY, showHidden);
        initFile = new File(intent.getStringExtra(PATH_KEY));

        CharSequence regex = intent.getCharSequenceExtra(SealActivity.PATTERN_KEY);
        Pattern pattern = null;
        if (!TextUtils.isEmpty(regex)) {
            pattern = Pattern.compile(regex.toString().replace(".", "\\.").replace("*", ".*"));
        }
        filter = new ItemFilter(forFile, isEmpty(regex) ? null : pattern, showHidden);
        sorter = new ItemSorter();
    }

    /* package */ File[] itemsOfFile(File file) {
        return IOs.itemsOf(file, filter);
    }

    private TextView tvTip;

    private TextView getTipView() {
        if (tvTip == null) {
            tvTip = (TextView) findViewById(R.id.tv_tip);
        }
        return tvTip;
    }

    private View progressView;

    private void showProgress(final boolean shown) {
        if (progressView == null) {
            progressView = findViewById(R.id.fetch_progress);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            progressView.setVisibility(shown ? View.VISIBLE : View.GONE);
            progressView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(shown ? 0.0F : 1.0F)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            progressView.setVisibility(shown ? View.VISIBLE : View.GONE);
                        }
                    });
        } else {
            progressView.setVisibility(shown ? View.VISIBLE : View.GONE);
        }
        if (shown) {
            setTitle(R.string.file_fetching);
        } else if (forFile) {
            setTitle(getResources().getQuantityString(R.plurals.file_title_for_file, multiple ? 0 : 1));
        } else {
            setTitle(getResources().getQuantityString(R.plurals.file_title_for_directory, multiple ? 0 : 1));
        }
    }


    private class FileItem extends AbstractItem {
        /**
         * Associated file
         */
        private File file;

        /**
         * Number of sub files
         */
        private int count;

        private FileItem parent;

        private Holder holder;

        FileItem(File file, boolean selected) {
            this.file = file;
            setSelected(selected);
        }

        @Override
        public int size() {
            return count;
        }

        @Override
        public void setParent(Item parent) {
            if (!(parent instanceof FileItem)) {
                throw new IllegalArgumentException("parent must be " + getClass());
            }
            this.parent = (FileItem) parent;
        }

        @Override
        public FileItem getParent() {
            return parent;
        }
    }

    /* package */ class Holder {
        ImageView icon;
        TextView name;
        TextView detail;
        ImageView option;
    }

    void updateCheckboxIcon(Holder holder, FileItem item) {
        if (holder != null) {
            holder.option.setImageResource(item.isSelected() ? R.drawable.ic_checkbox_selected : R.drawable.ic_checkbox_normal);
        }
    }

    private class FileView extends ListSupport<FileItem> {
        private boolean progressShown;

        FileView(ListView listView, boolean asyncFetch, FileItem current) {
            super(listView, asyncFetch, multiple, current);
        }

        @Override
        protected BaseAdapter createAdapter(ListSupport me) {
            return new FileAdapter();
        }

        @Override
        protected void fillItems(FileItem current) {
            val files = itemsOfFile(current.file);
            current.count = files.length;
            fileCount = dirCount = 0;
            if (files.length > 0) {
                Arrays.sort(files, sorter);
                for (val file : files) {
                    if (file.isFile()) {
                        ++fileCount;
                    }
                    if (file.isDirectory()) {
                        ++dirCount;
                    }
                    items.add(new FileItem(file, false));
                }
            }
        }

        @Override
        public void toggleSelection(FileItem item, Boolean selected) {
            super.toggleSelection(item, selected);
            updateCheckboxIcon(item.holder, item);
            val count = selections.size();
            tvDetail.setText(getResources().getQuantityString(R.plurals.file_selection_detail, count, count));
            if (count != 0) {
                if (forFile) {
                    btnCheckAll.setSelected(count == fileCount);
                } else {
                    btnCheckAll.setSelected(count == dirCount);
                }
            } else {
                btnCheckAll.setSelected(false);
            }
            btnChoose.setEnabled(count != 0);
        }

        @Override
        protected void onChoosing(FileItem item) {
            finishChoosing(Collections.singleton(item));
        }

        @Override
        protected void beforeFetching() {
            if (current == null || current.count <= 0 || current.count > PROGRESS_LIMIT) {
                progressShown = true;
                showProgress(true);
            } else {
                progressShown = false;
            }
        }

        @Override
        protected void afterFetching(Pair<Integer, Integer> position) {
            super.afterFetching(position);
            tvPath.setText(current.file.getPath());
            getTipView().setVisibility(size() == 0 ? View.VISIBLE : View.GONE);
            tvDetail.setText(getResources().getQuantityString(R.plurals.file_selection_detail, 0, 0));
            btnCheckAll.setVisibility(multiple ? View.VISIBLE : View.GONE);
            btnCheckAll.setSelected(false);
            btnChoose.setEnabled(!forFile && !multiple);
            if (progressShown) {
                showProgress(false);
            }
        }
    }

    private class FileAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return fileView.size();
        }

        @Override
        public FileItem getItem(int position) {
            return fileView.itemAt(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Holder holder;
            val item = getItem(position);
            if (convertView == null) {
                convertView = View.inflate(FileActivity.this, R.layout.icon_list_item, null);
                holder = new Holder();
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.name = (TextView) convertView.findViewById(R.id.text1);
                holder.name.setMaxLines(2);
                holder.detail = (TextView) convertView.findViewById(R.id.text2);
                holder.option = (ImageView) convertView.findViewById(R.id.option);
                if (multiple) {
                    holder.option.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            fileView.toggleSelection((FileItem) v.getTag(), null);
                        }
                    });
                }
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }
            item.holder = holder;
            holder.option.setTag(item);
            val file = item.file;
            holder.name.setText(file.getName());
            val date = new Date(file.lastModified());
            if (file.isDirectory()) {
                item.count = itemsOfFile(file).length;
                holder.icon.setImageResource(item.count != 0 ? R.drawable.ic_folder : R.drawable.ic_folder_empty);
                holder.detail.setText(getResources().getQuantityString(R.plurals.item_directory_detail, item.count, item.count, date));
                if (!forFile && multiple) {
                    updateCheckboxIcon(holder, item);
                } else {
                    holder.option.setImageResource(R.drawable.ic_arrow);
                }
            } else {
                holder.icon.setImageResource(R.drawable.ic_file);
                holder.detail.setText(getString(R.string.item_file_detail, IOs.readableSize(file.length(), getString(R.string.item_file_size_suffix)), date));
                if (multiple) {
                    updateCheckboxIcon(holder, item);
                } else {
                    holder.option.setImageDrawable(null);
                }
            }

            return convertView;
        }


    }
}
