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

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * Entry point to the Android API.
 * You must instantiate a <code>Client</code> object with your application ID and API key to start using Algolia Search
 * API.
 * <p>
 * WARNING: For performance reasons, arguments to asynchronous methods are not cloned. Therefore, you should not
 * modify mutable arguments after they have been passed (unless explicitly noted).
 * </p>
 */
public class Client {
    private final static String version = "3.4.2";

    /**
     * The user agents as a raw string. This is what is passed in request headers.
     * WARNING: It is stored for efficiency purposes. It should not be modified directly.
     */
    private String userAgentRaw;

    /***
     * A version of a software library.
     * Used to construct the <code>User-Agent</code> header.
     */
    public static class LibraryVersion {
        public final @NonNull String name;
        public final @NonNull String version;

        public LibraryVersion(@NonNull String name, @NonNull String version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof LibraryVersion))
                return false;
            LibraryVersion other = (LibraryVersion)object;
            return this.name.equals(other.name) && this.version.equals(other.version);
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ version.hashCode();
        }
    }

    /** The user agents, as a structured list of library versions. */
    private List<LibraryVersion> userAgents = new ArrayList<>();

    /** Connect timeout (ms). */
    private int connectTimeout = 2000;

    /** Default read (receive) timeout (ms). */
    private int readTimeout = 30000;

    /** Read timeout for search requests (ms). */
    private int searchTimeout = 5000;

    private final String applicationID;
    private final String apiKey;
    private List<String> readHosts;
    private List<String> writeHosts;

    /**
     * HTTP headers that will be sent with every request.
     */
    private HashMap<String, String> headers = new HashMap<String, String>();

    /** Handler used to execute operations on the main thread. */
    protected Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Thread pool used to run asynchronous requests. */
    protected ExecutorService searchExecutorService = Executors.newFixedThreadPool(4);

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
        this.applicationID = applicationID;
        this.apiKey = apiKey;
        this.addUserAgent(new LibraryVersion("Algolia for Android", version));
        this.addUserAgent(new LibraryVersion("Android", Build.VERSION.RELEASE));
        if (hosts != null) {
            setReadHosts(hosts);
            setWriteHosts(hosts);
        } else {
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
            readHosts = new ArrayList<>(fallbackHosts.size() + 1);
            readHosts.add(applicationID + "-dsn.algolia.net");
            readHosts.addAll(fallbackHosts);
            writeHosts = new ArrayList<>(fallbackHosts.size() + 1);
            writeHosts.add(applicationID + ".algolia.net");
            writeHosts.addAll(fallbackHosts);
        }
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public String getApplicationID() {
        return applicationID;
    }

    /**
     * Set an HTTP header that will be sent with every request.
     *
     * @param name  Header name.
     * @param value Value for the header. If null, the header will be removed.
     */
    public void setHeader(@NonNull String name, @Nullable String value) {
        if (value == null) {
            headers.remove(name);
        } else {
            headers.put(name, value);
        }
    }

    /**
     * Get an HTTP header.
     *
     * @param name Header name.
     */
    public String getHeader(@NonNull String name) {
        return headers.get(name);
    }

    public String[] getReadHosts() {
        return readHosts.toArray(new String[readHosts.size()]);
    }

    public void setReadHosts(@NonNull String... hosts) {
        if (hosts.length == 0) {
            throw new IllegalArgumentException("Hosts array cannot be empty");
        }
        readHosts = Arrays.asList(hosts);
    }

    public String[] getWriteHosts() {
        return writeHosts.toArray(new String[writeHosts.size()]);
    }

    public void setWriteHosts(@NonNull String... hosts) {
        if (hosts.length == 0) {
            throw new IllegalArgumentException("Hosts array cannot be empty");
        }
        writeHosts = Arrays.asList(hosts);
    }

    /**
     * Set read and write hosts to the same value (convenience method).
     *
     * @param hosts New hosts. Must not be empty.
     */
    public void setHosts(@NonNull String... hosts) {
        setReadHosts(hosts);
        setWriteHosts(hosts);
    }

    /**
     * Get the connection timeout.
     *
     * @return The connection timeout (ms).
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Set the connection timeout.
     *
     * @param connectTimeout The new connection timeout (ms).
     */
    public void setConnectTimeout(int connectTimeout) {
        if (connectTimeout <= 0)
            throw new IllegalArgumentException();
        this.connectTimeout = connectTimeout;
    }

    /**
     * Get the default read timeout.
     *
     * @return The default read timeout (ms).
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Set the default read timeout.
     *
     * @param readTimeout The default read timeout (ms).
     */
    public void setReadTimeout(int readTimeout) {
        if (readTimeout <= 0)
            throw new IllegalArgumentException();
        this.readTimeout = readTimeout;
    }

    /**
     * Get the read timeout for search requests.
     *
     * @return The read timeout for search requests (ms).
     */
    public int getSearchTimeout() {
        return searchTimeout;
    }

    /**
     * Set the read timeout for search requests.
     *
     * @param searchTimeout The read timeout for search requests (ms).
     */
    public void setSearchTimeout(int searchTimeout) {
        if (searchTimeout <= 0)
            throw  new IllegalArgumentException();
        this.searchTimeout = searchTimeout;
    }

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

    /**
     * Add a software library to the list of user agents.
     *
     * @param userAgent The library to add.
     */
    public void addUserAgent(@NonNull LibraryVersion userAgent) {
        userAgents.add(userAgent);
        updateUserAgents();
    }

    /**
     * Remove a software library from the list of user agents.
     *
     * @param userAgent The library to remove.
     */
    public void removeUserAgent(@NonNull LibraryVersion userAgent) {
        userAgents.remove(userAgent);
        updateUserAgents();
    }

    /**
     * Retrieve the list of declared user agents.
     *
     * @return The declared user agents.
     */
    public @NonNull LibraryVersion[] getUserAgents() {
        return userAgents.toArray(new LibraryVersion[userAgents.size()]);
    }

    /**
     * Test whether a user agent is declared.
     *
     * @param userAgent The user agent to look for.
     * @return true if it is declared on this client, false otherwise.
     */
    public boolean hasUserAgent(@NonNull LibraryVersion userAgent) {
        return userAgents.contains(userAgent);
    }

    private void updateUserAgents() {
        StringBuilder s = new StringBuilder();
        for (LibraryVersion userAgent : userAgents) {
            if (s.length() != 0) {
                s.append("; ");
            }
            s.append(userAgent.name);
            s.append(" (");
            s.append(userAgent.version);
            s.append(")");
        }
        userAgentRaw = s.toString();
    }

    // ----------------------------------------------------------------------
    // Public operations
    // ----------------------------------------------------------------------

    /**
     * List existing indexes.
     *
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request listIndexesAsync(@NonNull CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
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
    public Request deleteIndexAsync(final @NonNull String indexName, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
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
    public Request moveIndexAsync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
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
    public Request copyIndexAsync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return copyIndex(srcIndexName, dstIndexName);
            }
        }.start();
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
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request multipleQueriesAsync(final @NonNull List<IndexQuery> queries, final MultipleQueriesStrategy strategy, @NonNull CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
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
    public Request batchAsync(final @NonNull JSONArray operations, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return batch(operations);
            }
        }.start();
    }

    // ----------------------------------------------------------------------
    // Internal operations
    // ----------------------------------------------------------------------

    /**
     * List all existing indexes
     *
     * @return a JSON Object in the form:
     * { "items": [ {"name": "contacts", "createdAt": "2013-01-18T15:33:13.556Z"},
     *              {"name": "notes", "createdAt": "2013-01-18T15:33:13.556Z"}]}
     */
    protected JSONObject listIndexes() throws AlgoliaException {
        return getRequest("/1/indexes/", false);
    }

    /**
     * Delete an index
     *
     * @param indexName the name of index to delete
     * @return an object containing a "deletedAt" attribute
     */
    protected JSONObject deleteIndex(String indexName) throws AlgoliaException {
        try {
            return deleteRequest("/1/indexes/" + URLEncoder.encode(indexName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Move an existing index.
     *
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     */
    protected JSONObject moveIndex(String srcIndexName, String dstIndexName) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("operation", "move");
            content.put("destination", dstIndexName);
            return postRequest("/1/indexes/" + URLEncoder.encode(srcIndexName, "UTF-8") + "/operation", content.toString(), false);
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
     */
    protected JSONObject copyIndex(String srcIndexName, String dstIndexName) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("operation", "copy");
            content.put("destination", dstIndexName);
            return postRequest("/1/indexes/" + URLEncoder.encode(srcIndexName, "UTF-8") + "/operation", content.toString(), false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    protected JSONObject multipleQueries(List<IndexQuery> queries, String strategy) throws AlgoliaException {
        try {
            JSONArray requests = new JSONArray();
            for (IndexQuery indexQuery : queries) {
                requests.put(new JSONObject()
                        .put("indexName", indexQuery.getIndexName())
                        .put("params", indexQuery.getQuery().build())
                );
            }
            JSONObject body = new JSONObject().put("requests", requests);
            String path = "/1/indexes/*/queries";
            if (strategy != null) {
                body.put("strategy", strategy);
            }
            return postRequest(path, body.toString(), true);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Custom batch
     *
     * @param actions the array of actions
     * @throws AlgoliaException if the response is not valid json
     */
    protected JSONObject batch(JSONArray actions) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("requests", actions);
            return postRequest("/1/indexes/*/batch", content.toString(), false);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    // ----------------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------------

    private enum Method {
        GET, POST, PUT, DELETE
    }

    protected byte[] getRequestRaw(String url, boolean search) throws AlgoliaException {
        return _requestRaw(Method.GET, url, null, readHosts, connectTimeout, search ? searchTimeout : readTimeout);
    }

    protected JSONObject getRequest(String url, boolean search) throws AlgoliaException {
        return _request(Method.GET, url, null, readHosts, connectTimeout, search ? searchTimeout : readTimeout);
    }

    protected JSONObject deleteRequest(String url) throws AlgoliaException {
        return _request(Method.DELETE, url, null, writeHosts, connectTimeout, readTimeout);
    }

    protected JSONObject postRequest(String url, String obj, boolean readOperation) throws AlgoliaException {
        return _request(Method.POST, url, obj, (readOperation ? readHosts : writeHosts), connectTimeout, (readOperation ? searchTimeout : readTimeout));
    }

    protected byte[] postRequestRaw(String url, String obj, boolean readOperation) throws AlgoliaException {
        return _requestRaw(Method.POST, url, obj, (readOperation ? readHosts : writeHosts), connectTimeout, (readOperation ? searchTimeout : readTimeout));
    }

    protected JSONObject putRequest(String url, String obj) throws AlgoliaException {
        return _request(Method.PUT, url, obj, writeHosts, connectTimeout, readTimeout);
    }

    /**
     * Reads the InputStream as UTF-8
     *
     * @param stream the InputStream to read
     * @return the stream's content as a String
     * @throws IOException if the stream can't be read, decoded as UTF-8 or closed
     */
    private static String _toCharArray(InputStream stream) throws IOException {
        InputStreamReader is = new InputStreamReader(stream, "UTF-8");
        StringBuilder builder = new StringBuilder();
        char[] buf = new char[1000];
        int l = 0;
        while (l >= 0) {
            builder.append(buf, 0, l);
            l = is.read(buf);
        }
        is.close();
        return builder.toString();
    }

    /**
     * Reads the InputStream into a byte array
     * @param stream the InputStream to read
     * @return the stream's content as a byte[]
     * @throws AlgoliaException if the stream can't be read or flushed
     */
    private static byte[] _toByteArray(InputStream stream) throws AlgoliaException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        byte[] buffer = new byte[1024];

        try {
            while ((read = stream.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, read);
            }

            out.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new AlgoliaException("Error while reading stream: " + e.getMessage());
        }
    }


    protected static JSONObject _getJSONObject(String input) throws JSONException {
        return new JSONObject(new JSONTokener(input));
    }

    protected static JSONObject _getJSONObject(byte[] array) throws JSONException, UnsupportedEncodingException {
        return new JSONObject(new String(array, "UTF-8"));
    }

    private static JSONObject _getAnswerJSONObject(InputStream istream) throws IOException, JSONException {
        return _getJSONObject(_toCharArray(istream));
    }

    /**
     * Send the query according to parameters and returns its result as a JSONObject
     *
     * @param m              HTTP Method to use
     * @param url            endpoint URL
     * @param json           optional JSON Object to send
     * @param hostsArray     array of hosts to try successively
     * @param connectTimeout maximum wait time to open connection
     * @param readTimeout    maximum time to read data on socket
     * @return a JSONObject containing the resulting data or error
     * @throws AlgoliaException if the request data is not valid json
     */
    private JSONObject _request(Method m, String url, String json, List<String> hostsArray, int connectTimeout, int readTimeout) throws AlgoliaException {
        try {
            return _getJSONObject(_requestRaw(m, url, json, hostsArray, connectTimeout, readTimeout));
        } catch (JSONException e) {
            throw new AlgoliaException("JSON decode error:" + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new AlgoliaException("UTF-8 decode error:" + e.getMessage());
        }
    }

    /**
     * Send the query according to parameters and returns its result as a JSONObject
     *
     * @param m              HTTP Method to use
     * @param url            endpoint URL
     * @param json           optional JSON Object to send
     * @param hostsArray     array of hosts to try successively
     * @param connectTimeout maximum wait time to open connection
     * @param readTimeout    maximum time to read data on socket
     * @return a JSONObject containing the resulting data or error
     * @throws AlgoliaException in case of connection or data handling error
     */
    private byte[] _requestRaw(Method m, String url, String json, List<String> hostsArray, int connectTimeout, int readTimeout) throws AlgoliaException {
        String requestMethod;
        List<Exception> errors = new ArrayList<>(hostsArray.size());
        // for each host
        for (String host : hostsArray) {
            switch (m) {
                case DELETE:
                    requestMethod = "DELETE";
                    break;
                case GET:
                    requestMethod = "GET";
                    break;
                case POST:
                    requestMethod = "POST";
                    break;
                case PUT:
                    requestMethod = "PUT";
                    break;
                default:
                    throw new IllegalArgumentException("Method " + m + " is not supported");
            }

            InputStream stream = null;
            HttpURLConnection hostConnection = null;
            // set URL
            try {
                URL hostURL = new URL("https://" + host + url);
                hostConnection = (HttpURLConnection) hostURL.openConnection();

                //set timeouts
                hostConnection.setRequestMethod(requestMethod);
                hostConnection.setConnectTimeout(connectTimeout);
                hostConnection.setReadTimeout(readTimeout);

                // set auth headers
                hostConnection.setRequestProperty("X-Algolia-Application-Id", this.applicationID);
                hostConnection.setRequestProperty("X-Algolia-API-Key", this.apiKey);
                for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                    hostConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }

                // set user agent
                hostConnection.setRequestProperty("User-Agent", userAgentRaw);

                // write JSON entity
                if (json != null) {
                    if (!(requestMethod.equals("PUT") || requestMethod.equals("POST"))) {
                        throw new IllegalArgumentException("Method " + m + " cannot enclose entity");
                    }
                    hostConnection.setRequestProperty("Content-type", "application/json; charset=UTF-8");
                    hostConnection.setDoOutput(true);
                    OutputStreamWriter writer = new OutputStreamWriter(hostConnection.getOutputStream(), "UTF-8");
                    writer.write(json);
                    writer.close();
                }

                // read response
                int code = hostConnection.getResponseCode();
                final boolean codeIsError = code / 100 != 2;
                stream = codeIsError ? hostConnection.getErrorStream() : hostConnection.getInputStream();
                // As per the official Java docs (not the Android docs):
                // - `getErrorStream()` may return null => we have to handle this case.
                //   See <https://docs.oracle.com/javase/7/docs/api/java/net/HttpURLConnection.html#getErrorStream()>.
                // - `getInputStream()` should never return null... but let's err on the side of caution.
                //   See <https://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#getInputStream()>.
                if (stream == null) {
                    throw new IOException(String.format("Null stream when reading connection (status %d)", code));
                }

                final byte[] rawResponse;
                String encoding = hostConnection.getContentEncoding();
                if (encoding != null && encoding.equals("gzip")) {
                    rawResponse = _toByteArray(new GZIPInputStream(stream));
                } else {
                    rawResponse = _toByteArray(stream);
                }

                // handle http errors
                if (codeIsError) {
                    if (code / 100 == 4) {
                        consumeQuietly(hostConnection);
                        throw new AlgoliaException(_getJSONObject(rawResponse).getString("message"), code);
                    } else {
                        consumeQuietly(hostConnection);
                        errors.add(new AlgoliaException(_toCharArray(stream), code));
                        continue;
                    }
                }
                return rawResponse;

            }
            catch (JSONException e) { // fatal
                consumeQuietly(hostConnection);
                throw new AlgoliaException("Invalid JSON returned by server", e);
            }
            catch (UnsupportedEncodingException e) { // fatal
                consumeQuietly(hostConnection);
                throw new AlgoliaException("Invalid encoding returned by server", e);
            }
            catch (IOException e) { // host error, continue on the next host
                consumeQuietly(hostConnection);
                errors.add(e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        String errorMessage = "All hosts failed: " + Arrays.toString(errors.toArray());
        // When several errors occurred, use the last one as the cause for the returned exception.
        Throwable lastError = errors.get(errors.size() - 1);
        throw new AlgoliaException(errorMessage, lastError);
    }

    /**
     * Ensures that the entity content is fully consumed and the content stream, if exists,
     * is closed.
     */
    private static void consumeQuietly(final HttpURLConnection connection) {
        try {
            int read = 0;
            while (read != -1) {
                read = connection.getInputStream().read();
            }
            connection.getInputStream().close();
            read = 0;
            while (read != -1) {
                read = connection.getErrorStream().read();
            }
            connection.getErrorStream().close();
            connection.disconnect();
        } catch (IOException e) {
            // no inputStream to close
        }
    }

    // ----------------------------------------------------------------------
    // Utils
    // ----------------------------------------------------------------------

    /**
     * Abstract {@link Request} implementation using an `AsyncTask`.
     * Derived classes just have to implement the {@link #run()} method.
     */
    abstract class AsyncTaskRequest implements Request {
        /** The completion handler notified of the result. May be null if the caller omitted it. */
        private CompletionHandler completionHandler;

        /** The executor used to execute the request. */
        private ExecutorService executorService;

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
         * Construct a new request with the specified completion handler, executing on the client's default executor.
         *
         * @param completionHandler The completion handler to be notified of results. May be null if the caller omitted it.
         */
        AsyncTaskRequest(@Nullable CompletionHandler completionHandler) {
            this(completionHandler, searchExecutorService);
        }

        /**
         * Construct a new request with the specified completion handler, executing on the specified executor.
         *
         * @param completionHandler The completion handler to be notified of results. May be null if the caller omitted it.
         * @param executorService Executor service on which to execute the request.
         */
        AsyncTaskRequest(@Nullable CompletionHandler completionHandler, @NonNull ExecutorService executorService) {
            this.completionHandler = completionHandler;
            this.executorService = executorService;
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
        AsyncTaskRequest start() {
            // WARNING: Starting with Honeycomb (3.0), `AsyncTask` execution is serial, so we must force parallel
            // execution. See <http://developer.android.com/reference/android/os/AsyncTask.html>.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                task.executeOnExecutor(executorService);
            } else {
                task.execute();
            }
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
            return finished;
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

}
