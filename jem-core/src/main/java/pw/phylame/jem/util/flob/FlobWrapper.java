package pw.phylame.jem.util.flob;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FlobWrapper implements Flob {
    @NonNull
    private final Flob flob;

    public final Flob getTarget() {
        return flob;
    }

    @Override
    public String getName() {
        return flob.getMime();
    }

    @Override
    public String getMime() {
        return flob.getMime();
    }

    @Override
    public InputStream openStream() throws IOException {
        return flob.openStream();
    }

    @Override
    public byte[] readAll() throws IOException {
        return flob.readAll();
    }

    @Override
    public long writeTo(OutputStream out) throws IOException {
        return flob.writeTo(out);
    }

}
