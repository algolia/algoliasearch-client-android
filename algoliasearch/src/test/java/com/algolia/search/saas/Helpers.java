/*
 * Copyright (c) 2015 Algolia
 * http://www.algolia.com/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.algolia.search.saas;

import android.support.annotation.NonNull;
import java.util.Arrays;

public class Helpers {
    public static String app_id = "%ALGOLIA_APPLICATION_ID%";
    public static String api_key = "%ALGOLIA_API_KEY%";
    public static String PLACES_APP_ID = "%PLACES_APPLICATION_ID%";
    public static String PLACES_API_KEY = "%PLACES_API_KEY%";
    public static String job_number = "%JOB_NUMBER%";

    public static int wait = 30;

    static String getLongApiKey() {
        int n = 65000;
        char[] chars = new char[n];
        Arrays.fill(chars, 'c');
        return new String(chars);
    }

    static String safeIndexName(String name) {
        if (job_number.matches("\\d+\\.\\d+")) {
            name = String.format("%s_travis-%s", name, job_number);
        }
        return name;
    }

    /**
     * Get the method name of the caller.
     *
     * @return The caller's method name.
     */
    static @NonNull
    String getMethodName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int index = 0;
        while (index < stack.length) {
            StackTraceElement frame = stack[index];
            if (frame.getClassName().equals(Helpers.class.getName())) {
                ++index;
                break;
            }
            ++index;
        }
        assert(index < stack.length);
        return stack[index].getMethodName();
    }
}
