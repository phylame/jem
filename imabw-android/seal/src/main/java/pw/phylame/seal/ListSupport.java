package pw.phylame.seal;

import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
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
    protected ListView listView;
    protected BaseAdapter adapter;

    private boolean multiple;
    private boolean asyncFetch = true;

    protected T current;
    protected final List<T> items = new ArrayList<>();
    protected final Set<T> selections = new LinkedHashSet<>();
    private final Queue<Pair<Integer, Integer>> positions = new LinkedList<>();

    @MainThread
    public ListSupport(ListView listView, boolean asyncFetch, boolean multiple, T current) {
        this.listView = listView;
        this.multiple = multiple;
        this.asyncFetch = asyncFetch;
        if (current != null) {
            enter(current);
        }
        listView.setOnItemClickListener(this);
    }

    @MainThread
    protected abstract BaseAdapter createAdapter(ListSupport me);

    @WorkerThread
    protected abstract void fillItems(T current);

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
    public void enter(int index) {
        enter(items.get(index));
    }

    @MainThread
    public void enter(T item) {
        item.setParent(current);
        current = item;
        selections.clear();
        positions.offer(getPosition());
        onLevelChanged(new Pair<Integer, Integer>(0, null));
    }

    @MainThread
    public void back() {
        current = current.getParent();
        selections.clear();
        onLevelChanged(positions.poll());
    }

    @MainThread
    protected void beforeFetching() {
    }

    @MainThread
    protected void afterFetching(Pair<Integer, Integer> position) {
        if (adapter == null) {
            listView.setAdapter(adapter = createAdapter(this));
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        val item = items.get(position);
        if (item.size() > 0) {
            enter(item);
        } else if (!multiple) {
            onChoosing(item);
        } else {
            toggleSelection(item, null);
        }
    }

    public void toggleSelection(T item, Boolean selected) {
        item.setSelected(selected != null ? selected : !item.isSelected());
        if (item.isSelected()) {
            selections.add(item);
        } else {
            selections.remove(item);
        }
    }

    private void fetchItems() {
        items.clear();
        fillItems(current);
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
        protected void onPostExecute(Void aVoid) {
            afterFetching(positions.length > 0 ? positions[0] : null);
        }
    }
}
