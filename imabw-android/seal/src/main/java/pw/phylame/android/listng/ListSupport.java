package pw.phylame.android.listng;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.AppCompatTextView;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.HorizontalScrollView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import lombok.val;

public abstract class ListSupport<T extends Item> implements ListView.OnItemClickListener {
    public static int fastScrollLimit = 20;

    private ListView listView;
    private Adapter<T> adapter;

    private boolean forItem;
    private boolean multiple;
    private boolean asyncFetch;

    private T current;
    private final List<T> items = new ArrayList<>();
    private final Set<T> selections = new LinkedHashSet<>();
    private final LinkedList<Pair<Integer, Integer>> positions = new LinkedList<>();

    public ListSupport(ListView listView, boolean asyncFetch, boolean forItem, boolean multiple, T current) {
        this.listView = listView;
        this.forItem = forItem;
        this.multiple = multiple;
        this.asyncFetch = asyncFetch;
        this.current = current;
        listView.setOnItemClickListener(this);
    }

    @MainThread
    protected abstract Adapter<T> makeAdapter();

    @WorkerThread
    protected abstract void makeItems(T current, List<T> items);

    protected abstract void onTopReached();

    /**
     * Handle for item is chosen.
     *
     * @param item the item
     */
    protected abstract void onItemChosen(T item);

    protected final void setPathBar(T top,
                                    ViewGroup pathBar,
                                    final HorizontalScrollView pathBarHolder,
                                    float fontSize,
                                    int padding) {
        val items = new LinkedList<Pair<T, Pair<Integer, Integer>>>();
        @SuppressWarnings("unchecked")
        val iterator = ((LinkedList<Pair<Integer, Integer>>) positions.clone()).descendingIterator();
        for (T item = current; item != null && !item.equals(top); item = item.getParent()) {
            items.addFirst(new Pair<>(item, iterator.hasNext() ? iterator.next() : null));
        }
        pathBar.removeAllViews();
        val context = pathBar.getContext();
        int end = items.size() - 1, i = 0;
        View button;
        for (val it : items) {
            button = createPathButton(context, it, fontSize, padding);
            button.setTag(i);
            pathBar.addView(button);
            if (i++ != end) {
                pathBar.addView(createPathSeparator(context, fontSize));
            }
        }
        if (pathBarHolder.getVisibility() != View.VISIBLE) {
            pathBarHolder.setVisibility(View.VISIBLE);
        }
        pathBarHolder.post(new Runnable() {
            @Override
            public void run() {
                pathBarHolder.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        });
    }

    private View createPathButton(Context context,
                                  final Pair<T, Pair<Integer, Integer>> item,
                                  float fontSize,
                                  int padding) {
        val button = new AppCompatTextView(context);
        button.setTextSize(fontSize);
        button.setText(item.first.toString());
        button.setPadding(0, padding, 0, padding);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (item.first != current) {
                    // remove unused positions
                    Pair<Integer, Integer> position = null;
                    for (int i = (int) v.getTag(), end = positions.size(); i < end; ++i) {
                        position = positions.removeLast();
                    }
                    selections.clear();
                    current = item.first;
                    onLevelChanged(position);
                }
            }
        });
        return button;
    }

    private View createPathSeparator(Context context, float fontSize) {
        val text = new AppCompatTextView(context);
        text.setTextSize(fontSize);
        text.setText(" > ");
        return text;
    }

    @MainThread
    public void refresh(boolean keepPosition) {
        onLevelChanged(keepPosition ? null : new Pair<Integer, Integer>(0, null));
    }

    public void toggleSelection(T item, Boolean selected) {
        item.setSelected(selected != null ? selected : !item.isSelected());
        if (item.isSelected()) {
            selections.add(item);
        } else {
            selections.remove(item);
        }
    }

    public void toggleAll(boolean selected) {
        for (val item : items()) {
            if (forItem) {
                if (item.isItem()) {
                    toggleSelection(item, selected);
                }
            } else if (item.isGroup()) {
                toggleSelection(item, selected);
            }
        }
    }

    @MainThread
    public final void gotoItem(T item) {
        selections.clear();
        positions.addLast(currentPosition());
        this.current = item;
        refresh(false);
    }

    @MainThread
    public final void backTop() {
        current = current.getParent();
        if (current == null) {
            onTopReached();
        } else {
            selections.clear();
            onLevelChanged(positions.removeLast());
        }
    }

    /**
     * Does something before fetching items.
     */
    @MainThread
    protected void beforeFetching() {
    }

    /**
     * Does something after fetching items.
     *
     * @param position previous position of current item is list view
     */
    @MainThread
    protected void afterFetching(Pair<Integer, Integer> position) {
        if (adapter == null) {
            listView.setAdapter(adapter = makeAdapter());
        } else {
            adapter.notifyDataSetChanged();
        }
        listView.setFastScrollEnabled(size() >= fastScrollLimit);
        gotoPosition(position);
    }

    public final int size() {
        return items.size();
    }

    public final List<T> items() {
        return Collections.unmodifiableList(items);
    }

    public final T itemAt(int index) {
        return items.get(index);
    }

    public final T getCurrent() {
        return current;
    }

    public final void setCurrent(T current) {
        positions.clear();
        selections.clear();
        this.current = current;
        refresh(false);
    }

    public final Set<T> getSelections() {
        return Collections.unmodifiableSet(selections);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        val item = items.get(position);
        if (item.isGroup()) {
            gotoItem(item);
        } else if (!multiple) {
            onItemChosen(item);
        } else {
            toggleSelection(item, null);
        }
    }

    private void fetchItems() {
        items.clear();
        makeItems(current, items);
    }

    @SuppressWarnings("unchecked")
    private void onLevelChanged(Pair<Integer, Integer> position) {
        if (asyncFetch) {
            new FetchTask().execute(position);
        } else {
            beforeFetching();
            try {
                fetchItems();
                afterFetching(position);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private Pair<Integer, Integer> currentPosition() {
        int index = listView.getFirstVisiblePosition();
        View view = listView.getChildAt(0);
        int top = (view == null) ? 0 : view.getTop();
        if (top < 0 && listView.getChildAt(1) != null) {
            index++;
            view = listView.getChildAt(1);
            top = view.getTop();
        }
        return new Pair<>(index, top);
    }

    private void gotoPosition(Pair<Integer, Integer> position) {
        if (position != null) {
            if (position.second == null) {
                listView.setSelection(position.first);
            } else {
                listView.setSelectionFromTop(position.first, position.second);
            }
        }
    }

    private class FetchTask extends AsyncTask<Pair<Integer, Integer>, Void, Void> {
        private Pair<Integer, Integer>[] positions;

        @Override
        @SafeVarargs
        protected final Void doInBackground(Pair<Integer, Integer>... positions) {
            this.positions = positions;
            fetchItems();
            return null;
        }

        @Override
        protected void onPreExecute() {
            beforeFetching();
        }

        @Override
        protected void onPostExecute(Void ignored) {
            afterFetching(positions.length > 0 ? positions[0] : null);
        }
    }
}
