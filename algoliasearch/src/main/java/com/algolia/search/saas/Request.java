/*
 * Copyright (c) 2012-2016 Algolia
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

/**
 * An API request.
 *
 * Instances of this class encapsulates a sequence of normally one (nominal case), potentially many (in case of retry)
 * network calls into a high-level operation. This operation can be cancelled by the user.
 */
public interface Request
{
    // ==============================================================================================================
    // DISCUSSION: Why don't we use `AsyncTask` directly?
    // --------------------------------------------------
    // `AsyncTask` does not fit our purpose for many reasons:
    //
    // - `AsyncTask` has serial execution, whereas we need parallel execution. We don't want network requests to be
    //   processed serially, especially when searching "as you type"!
    //
    // - Having a custom interface leads to better encapsulation. We can refactor the underlying implementation without
    //   any breaking change.
    //
    // - The public API of `AsyncTask` is very verbose, and does not fit our purpose:
    //     - `cancel(.)` has an extraneous parameter `mayInterruptIfRunning`;
    //     - no `isFinished()` method.
    //
    // - `AsyncTask` does not allow calling the completion handler on another thread than the UI thread.
    //
    // - In theory, `AsyncTask` must be created on the UI thread, making it potentially dangerous to call API methods
    //   from another thread.
    // ==============================================================================================================

    /**
     * Cancel this request.
     * The listener will not be called after a request has been cancelled.
     * <p>
     * WARNING: Cancelling a request may or may not cancel the underlying network call, depending how late the
     * cancellation happens. In other words, a cancelled request may have already been executed by the server. In any
     * case, cancelling never carries "undo" semantics.
     * </p>
     */
    public void cancel();

    /**
     * Test if this request is still running.
     *
     * @return true if completed or cancelled, false if still running.
     */
    public boolean isFinished();

    /**
     * Test if this request has been cancelled.
     *
     * @return true if cancelled, false otherwise.
     */
    public boolean isCancelled();
}
