package pw.phylame.android.listng;

import android.widget.BaseAdapter;

public abstract class Adapter<T extends Item> extends BaseAdapter {
    private ListSupport<T> list;

    public Adapter(ListSupport<T> list) {
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public T getItem(int position) {
        return list.itemAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
