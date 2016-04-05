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

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.json.JSONObject;

/**
 * An API request.
 *
 * This class encapsulates a sequence of normally one (nominal case), potentially many (in case of retry) network
 * calls into a high-level operation. This operation can be cancelled by the user.
 */
public abstract class Request
{
    /** The completion handler notified of the result. May be null if the caller omitted it. */
    private CompletionHandler completionHandler;

    private boolean finished = false;

    /**
     * The underlying asynchronous task.
     */
    private AsyncTask<Void, Void, APIResult> task = new AsyncTask<Void, Void, APIResult>() {
        @Override
        protected APIResult doInBackground(Void... params) {
            try {
                return new APIResult(run());
            } catch (AlgoliaException e) {
                return new APIResult(e);
            }
        }

        @Override
        protected void onPostExecute(APIResult result) {
            finished = true;
            if (completionHandler != null) {
                completionHandler.requestCompleted(result.content, result.error);
            }
        }

        @Override
        protected void onCancelled(APIResult apiResult) {
            finished = true;
        }
    };

    /**
     * Construct a new request with the specified completion handler.
     *
     * @param completionHandler The completion handler to be notified of results. May be null if the caller omitted it.
     */
    Request(CompletionHandler completionHandler) {
        this.completionHandler = completionHandler;
    }

    /**
     * Run this request synchronously. To be implemented by derived classes.
     * <p>
     * <strong>Do not call this method directly.</strong> Will be run in a background thread when calling
     * {@link #start()}.
     * </p>
     *
     * @return The request's result.
     * @throws AlgoliaException If an error was encountered.
     */
    @NonNull
    abstract JSONObject run() throws AlgoliaException;

    /**
     * Run this request asynchronously.
     *
     * @return This instance.
     */
    Request start() {
        task.execute();
        return this;
    }

    /**
     * Cancel this request.
     * The listener will not be called after a request has been cancelled.
     * <p>
     * WARNING: Cancelling a request may or may not cancel the underlying network call, depending how late the
     * cancellation happens. In other words, a cancelled request may have already been executed by the server. In any
     * case, cancelling never carries "undo" semantics.
     * </p>
     */
    public void cancel() {
        // NOTE: We interrupt the task's thread to better cope with timeouts.
        task.cancel(true /* mayInterruptIfRunning */);
    }

    /**
     * Test if this request is still running.
     *
     * @return true if completed or cancelled, false if still running.
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Test if this request has been cancelled.
     *
     * @return true if cancelled, false otherwise.
     */
    public boolean isCancelled() {
        return task.isCancelled();
    }
}
