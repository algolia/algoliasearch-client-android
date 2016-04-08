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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Entry point in the Java API.
 * You should instantiate a Client object with your ApplicationID, ApiKey and Hosts
 * to start using Algolia Search API
 * <p>
 * WARNING: For performance reasons, arguments to asynchronous methods are not cloned. Therefore, you should not
 * modify mutable arguments after they have been passed (unless explicitly noted).
 * </p>
 */
public class APIClient extends BaseAPIClient {
    /**
     * Create a new Algolia Search client targetting the default hosts.
     *
     * NOTE: This is the recommended way to initialize a client is most use cases.
     *
     * @param applicationID The application ID (available in your Algolia Dashboard).
     * @param apiKey A valid API key for the service.
     */
    public APIClient(@NonNull String applicationID, @NonNull String apiKey) {
        this(applicationID, apiKey, null);
    }

    /**
     * Create a new Algolia Search client with explicit hosts to target.
     *
     * NOTE: In most use cases, you should the default hosts. See {@link APIClient#APIClient(String, String)}.
     *
     * @param applicationID The application ID (available in your Algolia Dashboard).
     * @param apiKey A valid API key for the service.
     * @param hosts An explicit list of hosts to target, or null to use the default hosts.
     */
    public APIClient(@NonNull String applicationID, @NonNull String apiKey, String[] hosts) {
        super(applicationID, apiKey, hosts);
    }

    /**
     * Create a proxy to an Algolia index (no server call required by this method).
     *
     * @param indexName The name of the index.
     * @return A new proxy to the specified index.
     */
    public Index initIndex(@NonNull String indexName) {
        return new Index(this, indexName);
    }

    /**
     * List existing indexes.
     *
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request listIndexesASync(CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return listIndexes();
            }
        }.start();
    }

    /**
     * Delete an index.
     *
     * @param indexName Name of index to delete.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteIndexASync(final @NonNull String indexName, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return deleteIndex(indexName);
            }
        }.start();
    }

    /**
     * Move an existing index.
     * If the destination index already exists, its specific API keys will be preserved and the source index specific
     * API keys will be added.
     *
     * @param srcIndexName Name of index to move.
     * @param dstIndexName The new index name.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request moveIndexASync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return moveIndex(srcIndexName, dstIndexName);
            }
        }.start();
    }

    /**
     * Copy an existing index.
     * If the destination index already exists, its specific API keys will be preserved and the source index specific
     * API keys will be added.
     *
     * @param srcIndexName Name of index to copy.
     * @param dstIndexName The new index name.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request copyIndexASync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return copyIndex(srcIndexName, dstIndexName);
            }
        }.start();
    }

    /**
     * Strategy when running multiple queries. See {@link APIClient#multipleQueriesASync}.
     */
    public enum MultipleQueriesStrategy {
        /** Execute the sequence of queries until the end. */
        NONE("none"),
        /** Execute the sequence of queries until the number of hits is reached by the sum of hits. */
        STOP_IF_ENOUGH_MATCHES("stopIfEnoughMatches");

        private String rawValue;

        MultipleQueriesStrategy(String rawValue) {
            this.rawValue = rawValue;
        }

        public String toString() {
            return rawValue;
        }
    }

    /**
     * Run multiple queries, potentially targetting multiple indexes, with one API call.
     *
     * @param queries The queries to run.
     * @param strategy The strategy to use.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request multipleQueriesASync(final @NonNull List<IndexQuery> queries, final MultipleQueriesStrategy strategy, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return multipleQueries(queries, strategy == null ? null : strategy.toString());
            }
        }.start();
    }

    /**
     * Batch operations.
     *
     * @param operations List of operations.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request batchASync(final @NonNull JSONArray operations, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return batch(operations);
            }
        }.start();
    }
}