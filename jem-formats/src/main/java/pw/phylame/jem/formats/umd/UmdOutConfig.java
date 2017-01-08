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

package pw.phylame.jem.formats.umd;


import pw.phylame.jem.epm.util.config.AbstractConfig;
import pw.phylame.jem.epm.util.config.Configured;
import pw.phylame.jem.epm.util.text.TextConfig;
import pw.phylame.jem.util.flob.Flob;

import java.util.List;

/**
 * Config for making UMD book.
 */
public class UmdOutConfig extends AbstractConfig {
    public static final String SELF = "config";
    public static final String TEXT_CONFIG = "textConfig";
    public static final String UMD_TYPE = "type";
    public static final String CARTOON_IMAGES = "cartoonImages";
    public static final String IMAGE_FORMAT = "imageFormat";

    /**
     * Config for rendering book text.
     *
     * @see TextConfig
     */
    @Configured(TEXT_CONFIG)
    public TextConfig textConfig = new TextConfig();

    /**
     * Output UMD type, may be {@link UMD#TEXT}, {@link UMD#CARTOON}, {@link UMD#COMIC}
     */
    @Configured(UMD_TYPE)
    public int umdType = UMD.TEXT;

    /**
     * List of <tt>Flob</tt> for making {@link UMD#CARTOON} book.
     * The <tt>Flob</tt> contain image data.
     * <p>If not specify the value, the maker will use covers of each chapter.
     * <p><strong>NOTE:</strong> this value will be available when <tt>umdType</tt>
     * is {@link UMD#CARTOON}.
     */
    @Configured(CARTOON_IMAGES)
    public List<Flob> cartoonImages = null;

    /**
     * Format of image in <tt>cartoonImages</tt>, ex: jpg, png, bmp..
     */
    @Configured(IMAGE_FORMAT)
    public String imageFormat = "jpg";

    @Override
    public void adjust() {
        textConfig.writeTitle = false;
    }
}
