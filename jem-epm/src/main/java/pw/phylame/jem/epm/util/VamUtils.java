package pw.phylame.jem.epm.util;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.vam.Archive;
import pw.phylame.ycl.vam.ArchiveWriter;
import pw.phylame.ycl.vam.Item;

import java.io.IOException;
import java.io.InputStream;

public final class VamUtils {
    private VamUtils() {
    }

    public static <T extends Item> InputStream streamOf(@NonNull Archive<T> archive, String name) throws IOException {
        val item = archive.itemFor(name);
        if (item == null) {
            throw new IOException(M.tr("err.vam.noEntry", name, archive.getName()));
        }
        return archive.inputStreamOf(item);
    }

    public static <T extends Item> String textOf(Archive<T> archive, String name, String encoding) throws IOException {
        try (val in = streamOf(archive, name)) {
            return IOUtils.toString(in, encoding);
        }
    }

    public static <T extends Item> void write(ArchiveWriter<T> aw, String name, String str, String encoding) throws IOException {
        val item = aw.mkitem(name);
        aw.begin(item);
        aw.write(item, encoding == null ? str.getBytes() : str.getBytes(encoding));
        aw.end(item);
    }

    public static <T extends Item> void write(ArchiveWriter<T> aw, String name, Flob flob) throws IOException {
        val item = aw.mkitem(name);
        flob.writeTo(aw.begin(item));
        aw.end(item);
    }

    public static <T extends Item> void write(ArchiveWriter<T> aw, String name, Text text, String encoding) throws IOException {
        val item = aw.mkitem(name);
        val writer = IOUtils.writerFor(aw.begin(item), encoding);
        text.writeTo(writer);
        writer.flush();
        aw.end(item);
    }
}
