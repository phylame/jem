/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pw.phylame.jem.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A file-like object providing reused and read-only input source.
 */
public interface Flob {
    /**
     * Returns name of the object.
     *
     * @return the name, never be <code>null</code>
     */
    String getName();

    /**
     * Returns the MIME type of the text.
     *
     * @return the MIME string, never be <code>null</code>
     */
    String getMime();

    /**
     * Opens an <code>InputStream</code> for reading the text.
     *
     * @return the stream, never be <code>null</code>
     * @throws IOException if occurs I/O errors
     */
    InputStream openStream() throws IOException;

    /**
     * Reads all bytes from the object.
     *
     * @return the byte array, may be an empty array
     * @throws IOException if occurs I/O errors
     */
    byte[] readAll() throws IOException;

    /**
     * Writes the object to specified output stream.
     *
     * @param out the destination output stream
     * @return number of written bytes
     * @throws IOException if occurs I/O errors
     */
    int writeTo(OutputStream out) throws IOException;
}
