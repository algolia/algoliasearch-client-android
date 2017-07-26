/*
 * Copyright (c) 2012-2017 Algolia
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
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-request options.
 * This class allows specifying options at the request level, overriding default options at the {@link Client} level.
 *
 * NOTE: These are reserved for advanced use cases. In most situations, they shouldn't be needed.
 */
public class RequestOptions {
    // Low-level storage
    // -----------------

    /** HTTP headers, as untyped values. */
    @NonNull
    Map<String, String> headers = new HashMap<>();

    /**
     * Set a HTTP header (untyped version).
     * Whenever possible, you should use a typed accessor.
     *
     * @param name Name of the header.
     * @param value Value of the header, or `null` to remove the header.
     */
    public RequestOptions setHeader(@NonNull String name, @Nullable String value) {
        if (value == null) {
            headers.remove(name);
        } else {
            headers.put(name, value);
        }
        return this;
    }

    /**
     * Get the value of a HTTP header.
     *
     * @param name Name of the header.
     * @return Value of the header, or `null` if it does not exist.
     */
    public String getHeader(@NonNull String name) {
        return headers.get(name);
    }

    // Debug
    // -----

    @Override
    public @NonNull String toString() {
        return String.format("%s{headers: %s}", this.getClass().getSimpleName(), this.headers);
    }

    // Comparison
    // ----------

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RequestOptions)) return false;
        RequestOptions another = (RequestOptions)obj;
        return this.headers.equals(another.headers);
    }

    // Construction
    // ------------

    /**
     * Construct empty request options.
     */
    public RequestOptions() {
    }
}
