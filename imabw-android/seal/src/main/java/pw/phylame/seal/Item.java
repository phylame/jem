package pw.phylame.seal;

import java.io.File;
import java.lang.ref.WeakReference;

class Item {
    File file;
    int count;
    boolean checked;
    private WeakReference<FileAdapter.Holder> holder;

    Item(File file, boolean checked) {
        this.file = file;
        this.checked = checked;
    }

    FileAdapter.Holder getHolder() {
        return holder != null ? holder.get() : null;
    }

    void setHolder(FileAdapter.Holder holder) {
        this.holder = new WeakReference<>(holder);
    }

    @Override
    public String toString() {
        return "Item{" +
                "file=" + file +
                ", count=" + count +
                ", checked=" + checked +
                ", holder=" + holder +
                '}';
    }
}
