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
import android.util.Log;

import org.json.JSONObject;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

/**
 * Abstract {@link Request} implementation, using a {@link java.util.concurrent.Future Future} internally.
 * Derived classes just have to implement the {@link #run()} method.
 */
abstract class FutureRequest implements Request {
    /** The completion handler notified of the result. May be null if the caller omitted it. */
    private final @Nullable CompletionHandler completionHandler;

    /** The executor used to execute the request. */
    private final @NonNull Executor requestExecutor;

    /** The executor used to execute the completion handler. */
    private final @NonNull Executor completionExecutor;

    /** The callable running the request. */
    private Callable<APIResult> callable = new Callable<APIResult>() {
        @Override
        public APIResult call() throws Exception {
            try {
                return new APIResult(run());
            } catch (AlgoliaException e) {
                return new APIResult(e);
            }
        }
    };

    /**
     * The future used to manage this request.
     * Compared to the raw `Callable`, the future gives us cancellation (built-in) and completion (the overridden
     * `done()` method).
     */
    private FutureTask<APIResult> task = new FutureTask<APIResult>(callable) {
        @Override
        protected void done() {
            if (completionHandler == null) {
                return;
            }
            try {
                final APIResult result = get();
                completionExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        // NOTE: Cancellation might have intervened after the request execution, but before the
                        // completion handler has been called.
                        if (isCancelled()) {
                            return;
                        }
                        completionHandler.requestCompleted(result.content, result.error);
                    }
                });
            }
            // If the task was cancelled, the call to `get()` will throw a `CancellationException`.
            catch (CancellationException e) {
                // Ignore.
            }
            catch (InterruptedException | ExecutionException e) {
                // Should never happen => log the error, but do not crash.
                Log.e(this.getClass().getName(), "When processing in background", e);
            }
        }
    };

    /**
     * Construct a new request with the specified completion handler, executing on the specified executor, and
     * calling the completion handler on another executor.
     *
     * @param completionHandler The completion handler to be notified of results. May be null if the caller omitted it.
     * @param requestExecutor Executor on which to execute the request.
     * @param completionExecutor Executor on which to call the completion handler.
     */
    FutureRequest(@Nullable CompletionHandler completionHandler, @NonNull Executor requestExecutor, @NonNull Executor completionExecutor) {
        this.completionHandler = completionHandler;
        this.requestExecutor = requestExecutor;
        this.completionExecutor = completionExecutor;
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
    abstract protected JSONObject run() throws AlgoliaException;

    /**
     * Run this request asynchronously.
     *
     * @return This instance.
     */
    public FutureRequest start() {
        requestExecutor.execute(task);
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
    @Override
    public void cancel() {
        // NOTE: We interrupt the task's thread to better cope with timeouts.
        task.cancel(true /* mayInterruptIfRunning */);
    }

    /**
     * Test if this request is still running.
     *
     * @return true if completed or cancelled, false if still running.
     */
    @Override
    public boolean isFinished() {
        return task.isDone();
    }

    /**
     * Test if this request has been cancelled.
     *
     * @return true if cancelled, false otherwise.
     */
    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }
}