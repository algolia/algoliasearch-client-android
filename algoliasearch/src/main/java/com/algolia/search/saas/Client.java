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
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point to the Android API.
 * You must instantiate a <code>Client</code> object with your application ID and API key to start using Algolia Search
 * API.
 * <p>
 * WARNING: For performance reasons, arguments to asynchronous methods are not cloned. Therefore, you should not
 * modify mutable arguments after they have been passed (unless explicitly noted).
 * </p>
 */
public class Client extends AbstractClient {
    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    /**
     * Cache of already created indices. The values are weakly referenced to avoid memory leaks.
     */
    protected Map<String, WeakReference<Object>> indices = new HashMap<>();

    // ----------------------------------------------------------------------
    // Initialization
    // ----------------------------------------------------------------------

    /**
     * Create a new Algolia Search client targeting the default hosts.
     *
     * NOTE: This is the recommended way to initialize a client is most use cases.
     *
     * @param applicationID The application ID (available in your Algolia Dashboard).
     * @param apiKey A valid API key for the service.
     */
    public Client(@NonNull String applicationID, @NonNull String apiKey) {
        this(applicationID, apiKey, null);
    }

    /**
     * Create a new Algolia Search client with explicit hosts to target.
     *
     * NOTE: In most use cases, you should the default hosts. See {@link Client#Client(String, String)}.
     *
     * @param applicationID The application ID (available in your Algolia Dashboard).
     * @param apiKey A valid API key for the service.
     * @param hosts An explicit list of hosts to target, or null to use the default hosts.
     */
    public Client(@NonNull String applicationID, @NonNull String apiKey, @Nullable String[] hosts) {
        super(applicationID, apiKey, hosts, hosts);
        if (hosts == null) {
            // Initialize hosts to their default values.
            //
            // NOTE: The host list comes in two parts:
            //
            // 1. The fault-tolerant, load-balanced DNS host.
            // 2. The non-fault-tolerant hosts. Those hosts must be randomized to ensure proper load balancing in case
            //    of the first host's failure.
            //
            List<String> fallbackHosts = Arrays.asList(
                    applicationID + "-1.algolianet.com",
                    applicationID + "-2.algolianet.com",
                    applicationID + "-3.algolianet.com"
            );
            Collections.shuffle(fallbackHosts);

            List<String> readHosts = new ArrayList<>(fallbackHosts.size() + 1);
            readHosts.add(applicationID + "-dsn.algolia.net");
            readHosts.addAll(fallbackHosts);
            setReadHosts(readHosts.toArray(new String[readHosts.size()]));

            List<String> writeHosts = new ArrayList<>(fallbackHosts.size() + 1);
            writeHosts.add(applicationID + ".algolia.net");
            writeHosts.addAll(fallbackHosts);
            setWriteHosts(writeHosts.toArray(new String[writeHosts.size()]));
        }
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public @NonNull String getApplicationID() {
        return super.getApplicationID();
    }

    // ----------------------------------------------------------------------
    // Index management
    // ----------------------------------------------------------------------

    /**
     * Create a proxy to an Algolia index (no server call required by this method).
     *
     * @param indexName The name of the index.
     * @return A new proxy to the specified index.
     *
     * @deprecated You should now use {@link #getIndex(String)}, which re-uses instances with the same name.
     */
    public Index initIndex(@NonNull String indexName) {
        return new Index(this, indexName);
    }

    /**
     * Obtain a proxy to an Algolia index (no server call required by this method).
     *
     * @param indexName The name of the index.
     * @return A proxy to the specified index.
     */
    public @NonNull Index getIndex(@NonNull String indexName) {
        Index index = null;
        WeakReference<Object> existingIndex = indices.get(indexName);
        if (existingIndex != null) {
            index = (Index)existingIndex.get();
        }
        if (index == null) {
            index = new Index(this, indexName);
            indices.put(indexName, new WeakReference<Object>(index));
        }
        return index;
    }

    // ----------------------------------------------------------------------
    // Public operations
    // ----------------------------------------------------------------------

    /**
     * List existing indexes.
     *
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request listIndexesAsync(@Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return listIndexes(requestOptions);
            }
        }.start();
    }

    /**
     * List existing indexes.
     *
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request listIndexesAsync(@NonNull CompletionHandler completionHandler) {
        return listIndexesAsync(/* requestOptions: */ null, completionHandler);
    }

    /**
     * Delete an index.
     *
     * @param indexName Name of index to delete.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteIndexAsync(final @NonNull String indexName, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return deleteIndex(indexName, requestOptions);
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
    public Request deleteIndexAsync(final @NonNull String indexName, CompletionHandler completionHandler) {
        return deleteIndexAsync(indexName, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Move an existing index.
     * If the destination index already exists, its specific API keys will be preserved and the source index specific
     * API keys will be added.
     *
     * @param srcIndexName Name of index to move.
     * @param dstIndexName The new index name.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request moveIndexAsync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return moveIndex(srcIndexName, dstIndexName, requestOptions);
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
    public Request moveIndexAsync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, CompletionHandler completionHandler) {
        return moveIndexAsync(srcIndexName, dstIndexName, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Copy an existing index.
     * If the destination index already exists, its specific API keys will be preserved and the source index specific
     * API keys will be added.
     *
     * @param srcIndexName Name of index to copy.
     * @param dstIndexName The new index name.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request copyIndexAsync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return copyIndex(srcIndexName, dstIndexName, requestOptions);
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
    public Request copyIndexAsync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, CompletionHandler completionHandler) {
        return copyIndexAsync(srcIndexName, dstIndexName, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Strategy when running multiple queries. See {@link Client#multipleQueriesAsync}.
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
     * Run multiple queries, potentially targeting multiple indexes, with one API call.
     *
     * @param queries The queries to run.
     * @param strategy The strategy to use.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request multipleQueriesAsync(final @NonNull List<IndexQuery> queries, final MultipleQueriesStrategy strategy, @Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return multipleQueries(queries, strategy == null ? null : strategy.toString(), requestOptions);
            }
        }.start();
    }

    /**
     * Run multiple queries, potentially targeting multiple indexes, with one API call.
     *
     * @param queries The queries to run.
     * @param strategy The strategy to use.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request multipleQueriesAsync(final @NonNull List<IndexQuery> queries, final MultipleQueriesStrategy strategy, @NonNull CompletionHandler completionHandler) {
        return multipleQueriesAsync(queries, strategy, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Batch operations.
     *
     * @param operations List of operations.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request batchAsync(final @NonNull JSONArray operations, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return batch(operations, requestOptions);
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
    public Request batchAsync(final @NonNull JSONArray operations, CompletionHandler completionHandler) {
        return batchAsync(operations, /* requestOptions: */ null, completionHandler);
    }

    // ----------------------------------------------------------------------
    // Internal operations
    // ----------------------------------------------------------------------

    /**
     * List all existing indexes
     *
     * @param requestOptions Request-specific options.
     * @return a JSON Object in the form:
     * { "items": [ {"name": "contacts", "createdAt": "2013-01-18T15:33:13.556Z"},
     *              {"name": "notes", "createdAt": "2013-01-18T15:33:13.556Z"}]}
     */
    protected JSONObject listIndexes(@Nullable RequestOptions requestOptions) throws AlgoliaException {
        return getRequest("/1/indexes/", /* urlParameters: */ null, false, requestOptions);
    }

    /**
     * Delete an index
     *
     * @param requestOptions Request-specific options.
     * @param indexName the name of index to delete
     * @return an object containing a "deletedAt" attribute
     */
    protected JSONObject deleteIndex(String indexName, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            return deleteRequest("/1/indexes/" + URLEncoder.encode(indexName, "UTF-8"), /* urlParameters: */ null, requestOptions);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Move an existing index.
     *
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     * @param requestOptions Request-specific options.
     */
    protected JSONObject moveIndex(String srcIndexName, String dstIndexName, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("operation", "move");
            content.put("destination", dstIndexName);
            return postRequest("/1/indexes/" + URLEncoder.encode(srcIndexName, "UTF-8") + "/operation", /* urlParameters: */ null, content.toString(), false, requestOptions);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Copy an existing index.
     *
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     * @param requestOptions Request-specific options.
     */
    protected JSONObject copyIndex(String srcIndexName, String dstIndexName, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("operation", "copy");
            content.put("destination", dstIndexName);
            return postRequest("/1/indexes/" + URLEncoder.encode(srcIndexName, "UTF-8") + "/operation", /* urlParameters: */ null, content.toString(), false, requestOptions);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    protected JSONObject multipleQueries(List<IndexQuery> queries, String strategy, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            JSONArray requests = new JSONArray();
            for (IndexQuery indexQuery : queries) {
                requests.put(new JSONObject()
                        .put("indexName", indexQuery.getIndexName())
                        .put("params", indexQuery.getQuery().build())
                );
            }
            JSONObject body = new JSONObject().put("requests", requests);
            if (strategy != null) {
                body.put("strategy", strategy);
            }
            return postRequest("/1/indexes/*/queries", /* urlParameters: */ null, body.toString(), true, requestOptions);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Custom batch
     *
     * @param actions the array of actions
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException if the response is not valid json
     */
    protected JSONObject batch(JSONArray actions, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("requests", actions);
            return postRequest("/1/indexes/*/batch", /* urlParameters: */ null, content.toString(), false, requestOptions);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }
}
