package pw.phylame.jem.epm.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.vam.Archive;
import pw.phylame.ycl.vam.ArchiveWriter;
import pw.phylame.ycl.vam.Item;

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

    public static <T extends Item> void write(ArchiveWriter<T> zipout, String name, String str, String encoding)
            throws IOException {
        val item = zipout.mkitem(name);
        zipout.begin(item);
        zipout.write(item, encoding == null ? str.getBytes() : str.getBytes(encoding));
        zipout.end(item);
    }

    public static <T extends Item> void write(ArchiveWriter<T> zipout, String name, Flob flob) throws IOException {
        val item = zipout.mkitem(name);
        flob.writeTo(zipout.begin(item));
        zipout.end(item);
    }

    /**
     * Writes text content in TextObject to PMAB archive.
     *
     * @param zipout
     *            PMAB archive stream
     * @param name
     *            name of entry to store text content
     * @param text
     *            the TextObject
     * @param encoding
     *            encoding to encode text, if <tt>null</tt> use platform encoding
     * @throws NullPointerException
     *             if arguments contain <tt>null</tt>
     * @throws IOException
     *             if occurs IO errors when writing text
     */
    public static <T extends Item> void write(ArchiveWriter<T> zipout, String name, Text text, String encoding)
            throws IOException {
        val item = zipout.mkitem(name);
        val in = zipout.begin(item);

        Writer writer = encoding != null ? new OutputStreamWriter(in, encoding) : new OutputStreamWriter(in);
        text.writeTo(writer);
        writer.flush();
        zipout.end(item);
    }
}
