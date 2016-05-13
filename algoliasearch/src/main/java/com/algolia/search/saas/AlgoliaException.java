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

import java.io.IOException;

/**
 * Any error that was encountered during the processing of a request.
 * Could be server-side, network failure, or client-side.
 */
public class AlgoliaException extends Exception {
    /** HTTP status code. Only valid when the error originates from the server. */
    private int statusCode;

    public AlgoliaException(String message) {
        super(message);
    }

    public AlgoliaException(String message, Throwable throwable)
    {
        super(message, throwable);
    }

    public AlgoliaException(String message, int statusCode)
    {
        super(message);
        this.statusCode = statusCode;
    }

    private static final long serialVersionUID = 1L;

    public int getStatusCode()
    {
        return statusCode;
    }

    /**
     * Test whether this error is transient.
     *
     * @return true if transient, false if fatal.
     */
    public boolean isTransient() {
        Throwable cause = getCause();
        if (cause == null) {
            return isServerError(statusCode);
        } else if (cause instanceof AlgoliaException) {
            return ((AlgoliaException)cause).isTransient();
        } else if (cause instanceof IOException) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isServerError(int statusCode) {
        return statusCode / 100 == 5;
    }
}
