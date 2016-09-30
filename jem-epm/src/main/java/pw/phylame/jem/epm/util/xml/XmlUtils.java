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

package pw.phylame.jem.epm.util.xml;

import lombok.val;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.M;
import pw.phylame.jem.epm.util.ParserException;

/**
 * XML utilities.
 */
public final class XmlUtils {
    private XmlUtils() {
    }

    public static XmlPullParser newPullParser(boolean awareness) throws ParserException {
        try {
            val factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(awareness);
            return factory.newPullParser();
        } catch (XmlPullParserException e) {
            throw new ParserException(M.tr("err.xml.noPullParser"), e);
        }
    }

    public static XmlSerializer newSerializer(boolean awareness) throws MakerException {
        try {
            val factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(awareness);
            return factory.newSerializer();
        } catch (XmlPullParserException e) {
            throw new MakerException(M.tr("err.xml.noSerializer"), e);
        }
    }

    public static String attributeOf(XmlPullParser xpp, String name) throws ParserException {
        String value = xpp.getAttributeValue(null, name);
        if (value == null) {
            throw new ParserException(M.tr("err.xml.noAttribute", name, xpp.getName()));
        }
        return value;
    }
}
