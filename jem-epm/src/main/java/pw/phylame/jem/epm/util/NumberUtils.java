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

package pw.phylame.jem.epm.util;

import pw.phylame.ycl.util.Provider;
import pw.phylame.ycl.value.Lazy;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Random;

/**
 * Utility class for number operations.
 */
public final class NumberUtils {

    private NumberUtils() {
    }

    private static final Lazy<Random> random = new Lazy<>(new Provider<Random>() {
        @Override
        public Random provide() throws Exception {
            return new Random(System.currentTimeMillis());
        }
    });

    /**
     * Generates a random integer number between {@code bottom} and {@code top}.
     *
     * @param bottom the lower limit
     * @param top    the upper limit
     * @return a random number
     */
    public static int randInteger(int bottom, int top) {
        return random.get().nextInt(top - bottom) + bottom;
    }

    /**
     * Generates a random long number between {@code bottom} and {@code top}.
     *
     * @param bottom the lower limit
     * @param top    the upper limit
     * @return a random number
     */
    public static Long randLong(long bottom, long top) {
        return random.get().nextInt((int) (top - bottom)) + bottom;
    }

    public static int parseInt(String str) throws ParserException {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ParserException(JEMessages.tr("err.number.invalidInteger", str), e);
        }
    }

    public static double parseDouble(String str) throws ParserException {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            throw new ParserException(JEMessages.tr("err.number.invalidDouble", str), e);
        }
    }

    public static Number parseNumber(String str) throws ParserException {
        try {
            return NumberFormat.getInstance().parse(str);
        } catch (ParseException e) {
            throw new ParserException(JEMessages.tr("err.number.invalidNumber", str), e);
        }
    }
}
