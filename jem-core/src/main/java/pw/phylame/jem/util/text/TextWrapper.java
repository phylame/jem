package pw.phylame.jem.util.text;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TextWrapper implements Text {
    @NonNull
    private final Text text;

    public final Text getTarget() {
        return text;
    }

    @Override
    public Iterator<String> iterator() {
        return text.iterator();
    }

    @Override
    public String getType() {
        return text.getType();
    }

    @Override
    public String getText() {
        return text.getText();
    }

    @Override
    public List<String> getLines(boolean skipEmpty) {
        return text.getLines(skipEmpty);
    }

    @Override
    public long writeTo(Writer writer) throws IOException {
        return text.writeTo(writer);
    }

}
