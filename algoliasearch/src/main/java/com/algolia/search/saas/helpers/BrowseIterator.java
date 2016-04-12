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

package com.algolia.search.saas.helpers;

import android.support.annotation.NonNull;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.algolia.search.saas.Request;

import org.json.JSONObject;

/**
 * Iterator to browse all index content.
 *
 * This helper takes care of chaining API requests and calling back the handler block with the results, until:
 * - the end of the index has been reached;
 * - an error has been encountered;
 * - or the user cancelled the iteration.
 */
public class BrowseIterator {

    /**
     * Listener for {@link com.algolia.search.saas.helpers.BrowseIterator}.
     */
    public interface BrowseIteratorHandler {
        /**
         * Called at each batch of results.
         *
         * @param iterator The iterator where the results originate from.
         * @param result The results (in case of success).
         * @param error The error (in case of error).
         */
        public void handleBatch(@NonNull BrowseIterator iterator, JSONObject result, AlgoliaException error);
    }

    /** The index being browsed. */
    private Index index;

    /** The query used to filter the results. */
    private Query query;

    /** Listener. */
    private BrowseIteratorHandler handler;

    /** Cursor to use for the next call, if any. */
    private String cursor;

    /** Whether the iteration has already started. */
    private transient boolean started = false;

    /** Whether the iteration has been cancelled by the user. */
    private transient boolean cancelled = false;

    /** The currently ongoing request, if any. */
    private Request request;

    /**
     * Construct a new browse iterator.
     * NOTE: The iteration does not start automatically. You have to call `start()` explicitly.
     *
     * @param index The index to be browsed.
     * @param query The query used to filter the results.
     * @param handler Handler called for each batch of results.
     */
    public BrowseIterator(@NonNull Index index, @NonNull Query query, @NonNull BrowseIteratorHandler handler) {
        this.index = index;
        this.query = query;
        this.handler = handler;
    }

    /**
     * Start the iteration.
     */
    public void start() {
        if (started) {
            throw new IllegalStateException();
        }
        started = true;
        request = index.browseAsync(query, completionHandler);
    }

    /**
     * Cancel the iteration.
     * This cancels any currently ongoing request, and cancels the iteration.
     * The listener will not be called after the iteration has been cancelled.
     */
    public void cancel() {
        if (cancelled)
            return;
        request.cancel();
        request = null;
        cancelled = true;
    }

    /**
     * Determine if there is more content to be browsed.
     * WARNING: Can only be called from the handler, once the iteration has started.
     */
    public boolean hasNext() {
        if (!started) {
            throw new IllegalStateException();
        }
        return cursor != null;
    }

    private void next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }
        request = index.browseFromAsync(cursor, completionHandler);
    }

    private CompletionHandler completionHandler = new CompletionHandler() {
        @Override
        public void requestCompleted(JSONObject content, AlgoliaException error) {
            if (!cancelled) {
                handler.handleBatch(BrowseIterator.this, content, error);
                if (error == null) {
                    cursor = content.optString("cursor", null);
                    if (!cancelled && hasNext()) {
                        next();
                    }
                }
            }
        }
    };
}
