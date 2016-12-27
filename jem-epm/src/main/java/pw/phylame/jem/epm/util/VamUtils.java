package pw.phylame.jem.epm.util;

import java.io.IOException;
import java.io.InputStream;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.vam.VamItem;
import pw.phylame.ycl.vam.VamReader;
import pw.phylame.ycl.vam.VamWriter;

public final class VamUtils {
    private VamUtils() {
    }

    public static <T extends VamItem> InputStream streamOf(@NonNull VamReader<T> vr, String name) throws IOException {
        val item = vr.itemFor(name);
        if (item == null) {
            throw new IOException(M.tr("err.vam.noEntry", name, vr.getName()));
        }
        return vr.streamOf(item);
    }

    public static <T extends VamItem> String textOf(VamReader<T> vr, String name, String encoding) throws IOException {
        try (val in = streamOf(vr, name)) {
            return IOUtils.toString(in, encoding);
        }
    }

    public static <T extends VamItem> void write(VamWriter<T> vw, String name, String str, String encoding) throws IOException {
        val item = vw.mkitem(name);
        vw.begin(item);
        vw.write(item, encoding == null ? str.getBytes() : str.getBytes(encoding));
        vw.end(item);
    }

    public static <T extends VamItem> void write(VamWriter<T> vw, String name, Flob flob) throws IOException {
        val item = vw.mkitem(name);
        flob.writeTo(vw.begin(item));
        vw.end(item);
    }

    public static <T extends VamItem> void write(VamWriter<T> vw, String name, Text text, String encoding) throws IOException {
        val item = vw.mkitem(name);
        val writer = IOUtils.writerFor(vw.begin(item), encoding);
        text.writeTo(writer);
        writer.flush();
        vw.end(item);
    }
}
