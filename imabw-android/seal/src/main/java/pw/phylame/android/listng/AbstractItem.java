package pw.phylame.android.listng;

public abstract class AbstractItem implements Item {
    private boolean selected;

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
