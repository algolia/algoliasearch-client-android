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

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.algolia.search.saas.helpers.HandlerExecutor;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * An abstract API client.
 */
public abstract class AbstractClient {
    // ----------------------------------------------------------------------
    // Types
    // ----------------------------------------------------------------------

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

    private static class HostStatus {
        boolean isUp = true;
        long lastTryTimestamp;

        HostStatus(boolean isUp) {
            this.isUp = isUp;
            lastTryTimestamp = new Date().getTime();
        }
    }

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    /** This library's version. */
    private final static String version = "3.12.1";

    /** Maximum size for an API key to be sent in the HTTP headers. Bigger keys will go inside the body. */
    private final static int MAX_API_KEY_LENGTH = 500;

    // ----------------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------------

    /**
     * The user agents as a raw string. This is what is passed in request headers.
     * WARNING: It is stored for efficiency purposes. It should not be modified directly.
     */
    private String userAgentRaw;

    /** The user agents, as a structured list of library versions. */
    private List<LibraryVersion> userAgents = new ArrayList<>();

    /** Connect timeout (ms). */
    private int connectTimeout = 2000;

    /** Default read (receive) timeout (ms). */
    private int readTimeout = 30000;

    /** Read timeout for search requests (ms). */
    private int searchTimeout = 5000;

    /** Delay to wait when a host is down before retrying it (ms). */
    private int hostDownDelay = 5000;

    private final String applicationID;
    private final String apiKey;
    private List<String> readHosts;
    private List<String> writeHosts;
    private HashMap<String, HostStatus> hostStatuses = new HashMap<>();

    /**
     * HTTP headers that will be sent with every request.
     */
    private HashMap<String, String> headers = new HashMap<String, String>();

    /** Thread pool used to run asynchronous requests. */
    protected ExecutorService searchExecutorService = Executors.newFixedThreadPool(4);

    /** Executor used to run completion handlers. By default, runs on the main thread. */
    protected @NonNull Executor completionExecutor = new HandlerExecutor(new Handler(Looper.getMainLooper()));

    protected Map<String, WeakReference<Object>> indices = new HashMap<>();

    // ----------------------------------------------------------------------
    // Initialization
    // ----------------------------------------------------------------------

    /**
     * Create a new client.
     *
     * @param applicationID [optional] The application ID.
     * @param apiKey [optional] A valid API key for the service.
     * @param readHosts List of hosts for read operations.
     * @param writeHosts List of hosts for write operations.
     */
    protected AbstractClient(@Nullable String applicationID, @Nullable String apiKey, @Nullable String[] readHosts, @Nullable String[] writeHosts) {
        this.applicationID = applicationID;
        this.apiKey = apiKey;
        this.addUserAgent(new LibraryVersion("Algolia for Android", version));
        this.addUserAgent(new LibraryVersion("Android", Build.VERSION.RELEASE));
        if (readHosts != null)
            setReadHosts(readHosts);
        if (writeHosts != null)
            setWriteHosts(writeHosts);
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
        checkTimeout(connectTimeout);
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
        checkTimeout(readTimeout);
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
        checkTimeout(searchTimeout);
        this.searchTimeout = searchTimeout;
    }

    /**
     * Get the timeout for retrying connection to a down host.
     *
     * @return The delay before connecting again to a down host (ms).
     */
    public int getHostDownDelay() {
        return hostDownDelay;
    }

    /**
     * Set the timeout for retrying connection to a down host.
     *
     * @param hostDownDelay The delay before connecting again to a down host (ms).
     */
    public void setHostDownDelay(int hostDownDelay) {
        checkTimeout(hostDownDelay);
        this.hostDownDelay = hostDownDelay;
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

    private List<String> getReadHostsThatAreUp() {
        return hostsThatAreUp(readHosts);
    }

    private List<String> getWriteHostsThatAreUp() {
        return hostsThatAreUp(writeHosts);
    }

    /**
     * Change the executor on which completion handlers are executed.
     * By default, completion handlers are executed on the main thread.
     *
     * @param completionExecutor The new completion executor to use.
     */
    public void setCompletionExecutor(@NonNull Executor completionExecutor) {
        this.completionExecutor = completionExecutor;
    }

    // ----------------------------------------------------------------------
    // Utilities
    // ----------------------------------------------------------------------

    /**
     * HTTP method.
     */
    private enum Method {
        GET, POST, PUT, DELETE
    }

    protected byte[] getRequestRaw(String url, boolean search) throws AlgoliaException {
        return _requestRaw(Method.GET, url, null, getReadHostsThatAreUp(), connectTimeout, search ? searchTimeout : readTimeout);
    }

    protected JSONObject getRequest(String url, boolean search) throws AlgoliaException {
        return _request(Method.GET, url, null, getReadHostsThatAreUp(), connectTimeout, search ? searchTimeout : readTimeout);
    }

    protected JSONObject deleteRequest(String url) throws AlgoliaException {
        return _request(Method.DELETE, url, null, getWriteHostsThatAreUp(), connectTimeout, readTimeout);
    }

    protected JSONObject postRequest(String url, String obj, boolean readOperation) throws AlgoliaException {
        return _request(Method.POST, url, obj, (readOperation ? getReadHostsThatAreUp() : getWriteHostsThatAreUp()), connectTimeout, (readOperation ? searchTimeout : readTimeout));
    }

    protected byte[] postRequestRaw(String url, String obj, boolean readOperation) throws AlgoliaException {
        return _requestRaw(Method.POST, url, obj, (readOperation ? getReadHostsThatAreUp() : getWriteHostsThatAreUp()), connectTimeout, (readOperation ? searchTimeout : readTimeout));
    }

    protected JSONObject putRequest(String url, String obj) throws AlgoliaException {
        return _request(Method.PUT, url, obj, getWriteHostsThatAreUp(), connectTimeout, readTimeout);
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
                // If API key is too big, send it in the request's body (if applicable).
                if (this.apiKey != null && this.apiKey.length() > MAX_API_KEY_LENGTH && json != null) {
                    try {
                        final JSONObject body = new JSONObject(json);
                        body.put("apiKey", this.apiKey);
                        json = body.toString();
                    } catch (JSONException e) {
                        throw new AlgoliaException("Failed to patch JSON body");
                    }
                } else {
                    hostConnection.setRequestProperty("X-Algolia-API-Key", this.apiKey);
                }
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
                hostStatuses.put(host, new HostStatus(true));

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
            } catch (IOException e) { // host error, continue on the next host
                hostStatuses.put(host, new HostStatus(false));
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

    private void checkTimeout(int connectTimeout) {
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get the hosts that are not considered down in a given list.
     * @param hosts a list of hosts whose {@link HostStatus} will be checked.
     * @return the hosts considered up, or all hosts if none is known to be reachable.
     */
    private List<String> hostsThatAreUp(List<String> hosts) {
        List<String> upHosts = new ArrayList<>();
        for (String host : hosts) {
            if (isUpOrCouldBeRetried(host)) {
                upHosts.add(host);
            }
        }
        return upHosts.isEmpty() ? hosts : upHosts;
    }

    boolean isUpOrCouldBeRetried(String host) {
        HostStatus status = hostStatuses.get(host);
        return status == null || status.isUp || new Date().getTime() - status.lastTryTimestamp >= hostDownDelay;
    }

    // ----------------------------------------------------------------------
    // Utils
    // ----------------------------------------------------------------------

    /**
     * Abstract convenience implementation of {@link FutureRequest} using the client's default executors.
     */
    abstract protected class AsyncTaskRequest extends FutureRequest {
        /**
         * Construct a new request with the specified completion handler, executing on the client's search executor,
         * and calling the completion handler on the client's completion executor.
         *
         * @param completionHandler  The completion handler to be notified of results. May be null if the caller omitted it.
         */
        protected AsyncTaskRequest(@Nullable CompletionHandler completionHandler) {
            this(completionHandler, searchExecutorService);
        }

        /**
         * Construct a new request with the specified completion handler, executing on the specified executor, and
         * calling the completion handler on the client's completion executor.
         *
         * @param completionHandler  The completion handler to be notified of results. May be null if the caller omitted it.
         * @param requestExecutor    Executor on which to execute the request.
         */
        protected AsyncTaskRequest(@Nullable CompletionHandler completionHandler, @NonNull Executor requestExecutor) {
            super(completionHandler, requestExecutor, completionExecutor);
        }
    }
}
