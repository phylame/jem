package pw.phylame.seal;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.val;

import static android.text.TextUtils.isEmpty;
import static android.text.TextUtils.join;

public class FileActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    public static final String PATH_KEY = "_path_key_";

    public static final int PROGRESS_LIMIT = 64;

    /* package */ boolean forFile = true;
    /* package */ boolean multiple = true;
    private boolean showHidden = false;

    private List<Item> data;
    private int fileCount, dirCount;
    private BaseAdapter adapter;

    private ListView listView;
    private TextView tvPath;
    private TextView tvDetail;
    private Button btnChoose;
    private MenuItem miShowHidden;
    private CheckedImageButton btnCheckAll;

    private File base;
    private Item current;
    private Set<File> selection;

    private ItemFilter filter;
    private ItemSorter sorter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        tvPath = (TextView) findViewById(R.id.tv_path);

        listView = (ListView) findViewById(R.id.list);
        listView.setOnItemClickListener(this);

        tvDetail = (TextView) findViewById(R.id.tv_detail);

        btnCheckAll = (CheckedImageButton) findViewById(R.id.btn_check_all);
        btnCheckAll.normalIconId = R.drawable.ic_checkbox_normal;
        btnCheckAll.selectedIconId = R.drawable.ic_checkbox_selected;
        btnCheckAll.setOnClickListener(this);

        findViewById(R.id.btn_cancel).setOnClickListener(this);
        btnChoose = (Button) findViewById(R.id.btn_choose);
        btnChoose.setOnClickListener(this);

        data = null;

        refresh();
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
            refresh();
            return true;
        } else if (id == R.id.action_by_name) {
            sorter.sortType = ItemSorter.BY_NAME;
            refresh();
            return true;
        } else if (id == R.id.action_by_type) {
            sorter.sortType = ItemSorter.BY_TYPE;
            refresh();
            return true;
        } else if (id == R.id.action_by_size_asc) {
            sorter.sortType = ItemSorter.BY_SIZE_ASC;
            refresh();
            return true;
        } else if (id == R.id.action_by_size_desc) {
            sorter.sortType = ItemSorter.BY_SIZE_DESC;
            refresh();
            return true;
        } else if (id == R.id.action_by_date_asc) {
            sorter.sortType = ItemSorter.BY_DATE_ASC;
            refresh();
            return true;
        } else if (id == R.id.action_by_date_desc) {
            sorter.sortType = ItemSorter.BY_DATE_DESC;
            refresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        val item = data.get(position);
        val file = item.file;
        if (file.isDirectory()) {
            gotoDirectory(item);
        } else if (!multiple) {
            finishChoosing(Collections.singletonList(file));
        } else {
            toggleSelection(item, null);
        }
    }

    @Override
    public void onClick(View v) {
        val id = v.getId();
        if (id == R.id.btn_cancel) {
            finishChoosing(null);
        } else if (id == R.id.btn_choose) {
            finishChoosing(!forFile && !multiple ? Collections.singletonList(current.file) : selection);
        } else if (id == R.id.btn_check_all) {
            val selected = !btnCheckAll.isSelected();
            for (val item : data) {
                if (forFile) {
                    if (item.file.isFile()) {
                        toggleSelection(item, selected);
                    }
                } else if (item.file.isDirectory()) {
                    toggleSelection(item, selected);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!current.file.equals(base)) {
            backDirectory();
        } else {
            super.onBackPressed();
        }
    }

    /* package */ void toggleSelection(Item item, Boolean checked) {
        item.checked = checked != null ? checked : !item.checked;
        FileAdapter.updateCheckboxIcon(item.getHolder(), item);
        if (item.checked) {
            selection.add(item.file);
        } else {
            selection.remove(item.file);
        }
        val count = selection.size();
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

    private void finishChoosing(Collection<File> files) {
        if (files == null) {
            setResult(RESULT_CANCELED);
        } else if (files.size() > 1) {

            val paths = new String[files.size()];
            int i = 0;
            for (val item : files) {
                paths[i++] = item.getAbsolutePath();
            }
            val data = new Intent();
            data.putExtra(SealActivity.RESULT_KEY, join(SealActivity.PATH_SEPARATOR, paths));
            setResult(RESULT_OK, data);
        } else {
            val data = new Intent();
            data.setData(Uri.fromFile(files.toArray(new File[1])[0]));
            setResult(RESULT_OK, data);
        }
        finish();
    }

    private void refresh(Runnable... tasks) {
        new FetchTask().execute(tasks);
    }

    private void initParams() {
        val intent = getIntent();
        forFile = intent.getBooleanExtra(SealActivity.MODE_KEY, forFile);
        multiple = intent.getBooleanExtra(SealActivity.MULTIPLE_KEY, multiple);
        showHidden = intent.getBooleanExtra(SealActivity.SHOW_HIDDEN_KEY, showHidden);
        base = new File(intent.getStringExtra(PATH_KEY));
        current = new Item(base, false);
        selection = new LinkedHashSet<>();

        CharSequence regex = intent.getCharSequenceExtra(SealActivity.PATTERN_KEY);
        Pattern pattern = null;
        if (!TextUtils.isEmpty(regex)) {
            pattern = Pattern.compile(regex.toString().replace(".", "\\.").replace("*", ".*"));
        }
        filter = new ItemFilter(forFile, isEmpty(regex) ? null : pattern, showHidden);
        sorter = new ItemSorter();
    }

    /* package */ int getItemSize() {
        return data == null ? 0 : data.size();
    }

    /* package */ Item getItemAt(int position) {
        return data.get(position);
    }

    private void fetchItems() {
        if (data == null) { // first invoking
            initParams();
            data = new ArrayList<>();
        } else {
            data.clear();
        }
        selection.clear();
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
                data.add(new Item(file, false));
            }
        }
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

    private final Queue<Pair<Integer, Integer>> positions = new LinkedList<>();

    private void gotoDirectory(Item item) {
        current = item;
        selection.clear();
        int index = listView.getFirstVisiblePosition();
        View view = listView.getChildAt(0);
        int top = (view == null) ? 0 : view.getTop();
        if (top < 0 && listView.getChildAt(1) != null) {
            index++;
            view = listView.getChildAt(1);
            top = view.getTop();
        }
        positions.offer(new Pair<>(index, top));
        refresh(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(0);
            }
        });
    }

    private void backDirectory() {
        current = new Item(current.file.getParentFile(), false);
        selection.clear();
        refresh(new Runnable() {
            @Override
            public void run() {
                val pair = positions.poll();
                listView.setSelectionFromTop(pair.first, pair.second);
            }
        });
    }

    private class FetchTask extends AsyncTask<Runnable, Void, Void> {
        private Runnable[] tasks;
        private boolean progressShown;

        @Override
        protected Void doInBackground(Runnable... tasks) {
            this.tasks = tasks;
            fetchItems();
            return null;
        }

        @Override
        protected void onPreExecute() {
            if (current == null || current.count <= 0 || current.count > PROGRESS_LIMIT) {
                progressShown = true;
                showProgress(true);
            } else {
                progressShown = false;
            }
        }

        @Override
        protected void onPostExecute(Void items) {
            tvPath.setText(current.file.getPath());
            if (adapter == null) {
                listView.setAdapter(adapter = new FileAdapter(FileActivity.this));
            } else {
                adapter.notifyDataSetChanged();
            }
            if (progressShown) {
                showProgress(false);
            }
            getTipView().setVisibility(adapter.getCount() == 0 ? TextView.VISIBLE : TextView.INVISIBLE);
            tvDetail.setText(getResources().getQuantityString(R.plurals.file_selection_detail, 0, 0));
            btnCheckAll.setVisibility(multiple ? View.VISIBLE : View.GONE);
            btnCheckAll.setSelected(false);
            btnChoose.setEnabled(!forFile && !multiple);

            // additional tasks
            for (val task : tasks) {
                task.run();
            }
        }
    }
}
