package pw.phylame.android.listng;

import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import lombok.val;

public abstract class ListSupport<T extends Item> implements ListView.OnItemClickListener {
    private ListView listView;
    private Adapter<T> adapter;

    private boolean forItem;
    private boolean multiple;
    private boolean asyncFetch;

    private T current;
    private final List<T> items = new ArrayList<>();
    private final Set<T> selections = new LinkedHashSet<>();
    private final Queue<Pair<Integer, Integer>> positions = new LinkedList<>();

    @MainThread
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

    /**
     * Handle for item is chosen.
     *
     * @param item the item
     */
    protected abstract void onChoosing(T item);

    @MainThread
    public void refresh() {
        onLevelChanged(null);
    }

    @MainThread
    public void refresh(T current) {
        this.current = current;
        onLevelChanged(null);
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
    public final void gotoItem(int index) {
        gotoItem(items.get(index));
    }

    @MainThread
    public final void gotoItem(T item) {
        item.setParent(current);
        current = item;
        selections.clear();
        positions.offer(getPosition());
        onLevelChanged(new Pair<Integer, Integer>(0, null));
    }

    @MainThread
    public void backTop() {
        current = current.getParent();
        selections.clear();
        onLevelChanged(positions.poll());
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

    public T getCurrent() {
        return current;
    }

    public Set<T> getSelections() {
        return selections;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        val item = items.get(position);
        if (item.size() > 0) {
            gotoItem(item);
        } else if (!multiple) {
            onChoosing(item);
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
            } catch (RuntimeException ignore) {
            }
        }
    }

    private Pair<Integer, Integer> getPosition() {
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
