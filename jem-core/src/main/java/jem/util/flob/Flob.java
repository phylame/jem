/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.util.flob;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A file-like object providing large and reusable binary data.
 */
public interface Flob {
    /**
     * Returns the name of this object.
     *
     * @return the name of this object
     */
    String getName();

    /**
     * Returns the mime type of this object.
     *
     * @return the mime type of this object
     */
    String getMime();

    /**
     * Opens an {@code InputStream} for reading data of this object.
     * <p>
     * This method can be called multiple times.
     *
     * @return an input stream for reading this object
     * @throws IOException if an I/O error occurs
     */
    InputStream openStream() throws IOException;

    /**
     * Writes data of this object to specified output stream.
     *
     * @param output the destination output stream
     * @return number of written bytes
     * @throws NullPointerException if specified output is null
     * @throws IOException          if an I/O error occurs
     */
    long writeTo(OutputStream output) throws IOException;
}
