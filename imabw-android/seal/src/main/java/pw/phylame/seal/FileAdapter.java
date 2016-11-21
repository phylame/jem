package pw.phylame.seal;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

import lombok.val;

class FileAdapter extends BaseAdapter {

    private final FileActivity activity;

    FileAdapter(FileActivity activity) {
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return activity.getItemSize();
    }

    @Override
    public Item getItem(int position) {
        return activity.getItemAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final Holder holder;
        val item = getItem(position);

        if (convertView == null) {
            view = View.inflate(activity, R.layout.icon_list_item, null);
            holder = new Holder();
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.name = (TextView) view.findViewById(R.id.text1);
            holder.name.setMaxLines(2);
            holder.detail = (TextView) view.findViewById(R.id.text2);
            holder.option = (ImageView) view.findViewById(R.id.option);
            if (activity.multiple) {
                holder.option.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.toggleSelection((Item) v.getTag(), null);
                    }
                });
            }
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (Holder) view.getTag();
        }
        item.setHolder(holder);
        holder.option.setTag(item);
        val file = item.file;
        holder.name.setText(file.getName());
        val date = new Date(file.lastModified());
        if (file.isDirectory()) {
            item.count = activity.itemsOfFile(file).length;
            holder.icon.setImageResource(item.count != 0 ? R.drawable.ic_folder : R.drawable.ic_folder_empty);
            holder.detail.setText(activity.getResources().getQuantityString(
                    R.plurals.item_directory_detail, item.count, item.count, date));
            if (!activity.forFile && activity.multiple) {
                updateCheckboxIcon(holder, item);
            } else {
                holder.option.setImageResource(R.drawable.ic_arrow);
            }
        } else {
            holder.icon.setImageResource(R.drawable.ic_file);
            holder.detail.setText(activity.getString(R.string.item_file_detail,
                    IOs.readableSize(file.length(), activity.getString(R.string.item_file_size_suffix)),
                    date));
            if (activity.multiple) {
                updateCheckboxIcon(holder, item);
            } else {
                holder.option.setImageDrawable(null);
            }
        }

        return view;
    }

    static void updateCheckboxIcon(Holder holder, Item item) {
        if (holder != null) {
            holder.option.setImageResource(
                    item.checked
                            ? R.drawable.ic_checkbox_selected
                            : R.drawable.ic_checkbox_normal);
        }
    }

    /* package */ class Holder {
        ImageView icon;
        TextView name;
        TextView detail;
        ImageView option;
    }
}
