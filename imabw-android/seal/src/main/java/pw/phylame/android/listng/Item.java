package pw.phylame.android.listng;

public interface Item {
    int size();

    boolean isSelected();

    void setSelected(boolean selected);

    void setParent(Item parent);

    <T extends Item> T getParent();

    boolean isItem();

    boolean isGroup();
}
