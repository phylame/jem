/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.epm.util.xml;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.xmlpull.v1.XmlSerializer;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.ycl.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.LinkedList;

/**
 * Renders XML document.
 */
public class XmlRender {
    private final XmlSerializer serializer;
    private final XmlConfig config;

    private final boolean useIndent;
    private int indentCount;

    private final LinkedList<TagEntry> tagStack = new LinkedList<>();

    public XmlRender(@NonNull XmlConfig config, boolean awareness) throws MakerException {
        this.config = config;
        serializer = XmlUtils.newSerializer(awareness);
        useIndent = StringUtils.isNotEmpty(config.indentString);
    }

    public XmlRender setOutput(@NonNull Writer writer) throws IOException {
        serializer.setOutput(writer);
        return this;
    }

    public XmlRender setOutput(@NonNull OutputStream outputStream) throws IOException {
        serializer.setOutput(outputStream, config.encoding);
        return this;
    }

    public XmlRender setOutput(@NonNull OutputStream outputStream, String encoding) throws IOException {
        serializer.setOutput(outputStream, encoding);
        return this;
    }

    public XmlRender flush() throws IOException {
        serializer.flush();
        return this;
    }

    public XmlRender beginXml() throws IOException {
        serializer.startDocument(config.encoding, config.standalone);
        reset();
        return this;
    }

    public void endXml() throws IOException {
        serializer.endDocument();
        flush();
    }

    public XmlRender reset() {
        tagStack.clear();
        indentCount = 0;
        return this;
    }

    public XmlRender docdecl(String root, String id, String url) throws IOException {
        docdecl(root + " PUBLIC \"" + id + "\" \"" + url + "\"");
        return this;
    }

    public XmlRender docdecl(String text) throws IOException {
        serializer.text(config.lineSeparator);
        serializer.docdecl(" " + text);
        return this;
    }

    private void indent(int count) throws IOException {
        if (count <= 0) {
            return;
        }
        serializer.text(StringUtils.multiplyOf(config.indentString, count));
    }

    private void newNode() throws IOException {
        serializer.text(config.lineSeparator);   // node in new line
        if (useIndent) {
            indent(indentCount);
        }
        if (!tagStack.isEmpty()) {
            tagStack.getFirst().hasSubTag = true;
        }
    }

    public XmlRender comment(String text) throws IOException {
        newNode();
        serializer.comment(text);
        return this;
    }

    public XmlRender beginTag(String name) throws IOException {
        return beginTag(null, name);
    }

    public XmlRender beginTag(String namespace, String name) throws IOException {
        newNode();
        ++indentCount;
        serializer.startTag(namespace, name);
        tagStack.push(new TagEntry(namespace, name));
        return this;
    }

    public XmlRender attribute(String name, String value) throws IOException {
        serializer.attribute(null, name, value);
        return this;
    }

    public XmlRender attribute(String namespace, String name, String value) throws IOException {
        serializer.attribute(namespace, name, value);
        return this;
    }

    public XmlRender text(String text) throws IOException {
        serializer.text(text);
        return this;
    }

    public XmlRender endTag() throws IOException {
        if (tagStack.isEmpty()) {
            throw new AssertionError("startTag should be called firstly");
        }
        val tagEntry = tagStack.pop();
        if (tagEntry.hasSubTag) {
            serializer.text(config.lineSeparator);
            if (useIndent) {
                indent(indentCount - 1);
            }
        }
        --indentCount;
        serializer.endTag(tagEntry.namespace, tagEntry.name);
        return this;
    }

    @RequiredArgsConstructor
    private class TagEntry {
        private final String namespace;
        private final String name;

        // for endTag, if hasSubTag add line separator and indent
        private boolean hasSubTag = false;
    }
}
