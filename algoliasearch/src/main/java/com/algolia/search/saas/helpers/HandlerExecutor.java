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

package com.algolia.search.saas.helpers;


import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Adapts an Android {@link android.os.Handler Handler} into a JRE {@link java.util.concurrent.Executor Executor}.
 * Runnables will be posted asynchronously.
 */
public class HandlerExecutor implements Executor {
    /** Handler wrapped by this executor. */
    private Handler handler;

    /**
     * Construct a new executor wrapping the specified handler.
     *
     * @param handler Handler to wrap.
     */
    public HandlerExecutor(@NonNull Handler handler) {
        this.handler = handler;
    }

    /**
     * Execute a command, by posting it to the underlying handler.
     *
     * @param command Command to execute.
     */
    public void execute(Runnable command) {
        handler.post(command);
    }
}
