package pw.phylame.jem.epm.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.vam.FileVamReader;
import pw.phylame.ycl.vam.VamReader;
import pw.phylame.ycl.vam.VamWriter;
import pw.phylame.ycl.vam.ZipVamReader;

public final class VamUtils {
    private VamUtils() {
    }

    public static VamReader openReader(File file, String type) throws IOException {
        switch (type) {
        case "dir":
            return new FileVamReader(file);
        case "zip":
            return new ZipVamReader(file);
        default:
            return null;
        }
    }

    public static VamReader openReader(File file) throws IOException {
        return file.isDirectory() ? new FileVamReader(file) : new ZipVamReader(file);
    }

    public static InputStream streamOf(@NonNull VamReader vr, String name) throws IOException {
        val item = vr.itemFor(name);
        if (item == null) {
            throw new IOException(M.tr("err.vam.noEntry", name, vr.getName()));
        }
        return vr.streamOf(item);
    }

    public static String textOf(VamReader vr, String name, String encoding) throws IOException {
        try (val in = streamOf(vr, name)) {
            return IOUtils.toString(in, encoding);
        }
    }

    public static void write(VamWriter vw, String name, String str, String encoding) throws IOException {
        val item = vw.mkitem(name);
        vw.begin(item);
        vw.write(item, encoding == null ? str.getBytes() : str.getBytes(encoding));
        vw.end(item);
    }

    public static void write(VamWriter vw, String name, Flob flob) throws IOException {
        val item = vw.mkitem(name);
        flob.writeTo(vw.begin(item));
        vw.end(item);
    }

    public static void write(VamWriter vw, String name, Text text, String encoding) throws IOException {
        val item = vw.mkitem(name);
        val writer = IOUtils.writerFor(vw.begin(item), encoding);
        text.writeTo(writer);
        writer.flush();
        vw.end(item);
    }
}
