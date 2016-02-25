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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Abstract Entry point in the Java API.
 */
abstract class BaseAPIClient {
    private int httpSocketTimeoutMS = 30000;
    private int httpConnectTimeoutMS = 2000;
    private int httpSearchTimeoutMS = 5000;

    private final static String version = "2.6.2";

    private final String applicationID;
    private final String apiKey;
    private final List<String> readHostsArray;
    private final List<String> writeHostsArray;
    private final DefaultHttpClient httpClient;
    private String tagFilters;
    private String userToken;
    private HashMap<String, String> headers;

    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param hostsArray the list of hosts that you have received for the service
     * @param enableDsn set to true if your account has the Distributed Search Option
     * @param dsnHost override the automatic computation of dsn hostname
     */
    protected BaseAPIClient(String applicationID, String apiKey, List<String> hostsArray, boolean enableDsn, String dsnHost) {
        if (applicationID == null || applicationID.length() == 0) {
            throw new RuntimeException("AlgoliaSearch requires an applicationID.");
        }
        this.applicationID = applicationID;
        if (apiKey == null || apiKey.length() == 0) {
            throw new RuntimeException("AlgoliaSearch requires an apiKey.");
        }
        this.apiKey = apiKey;
        if (hostsArray == null || hostsArray.size() == 0) {
            readHostsArray = Arrays.asList(applicationID + "-dsn.algolia.net",
                    applicationID + "-1.algolianet.com",
                    applicationID + "-2.algolianet.com",
                    applicationID + "-3.algolianet.com");
            writeHostsArray = Arrays.asList(applicationID + ".algolia.net",
                    applicationID + "-1.algolianet.com",
                    applicationID + "-2.algolianet.com",
                    applicationID + "-3.algolianet.com");
        } else {
            readHostsArray = writeHostsArray = hostsArray;
        }
        httpClient = new DefaultHttpClient();
        headers = new HashMap<String, String>();
    }

    /**
     * Allow to set custom headers
     */
    public void setExtraHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * Allow to set timeout
     * @param connectTimeout connection timeout in MS
     * @param readTimeout socket timeout in MS
     */
    public void setTimeout(int connectTimeout, int readTimeout) {
        httpSocketTimeoutMS = readTimeout;
        httpConnectTimeoutMS = httpSearchTimeoutMS = connectTimeout;
    }

    /**
     * Allow to set timeout
     * @param connectTimeout connection timeout in MS
     * @param readTimeout socket timeout in MS
     * @param searchTimeout socket timeout in MS
     */
    public void setTimeout(int connectTimeout, int readTimeout, int searchTimeout) {
        httpSocketTimeoutMS = readTimeout;
        httpConnectTimeoutMS = connectTimeout;
        httpSearchTimeoutMS = searchTimeout;
    }

    public void setSecurityTags(String tagFilters) {
        this.tagFilters = tagFilters;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    /**
     * List all existing indexes
     * return an JSON Object in the form:
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
     * return an object containing a "deletedAt" attribute
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

    /**
     * Return last logs entries.
     * @param offset Specify the first entry to retrieve (0-based, 0 is the most recent log entry).
     * @param length Specify the maximum number of entries to retrieve starting at offset. Maximum allowed value: 1000.
     * @param logType Specify the type of log to retrieve
     */
    protected JSONObject getLogs(int offset, int length, LogType logType) throws AlgoliaException {
        String type = null;
        switch (logType) {
            case LOG_BUILD:
                type = "build";
                break;
            case LOG_QUERY:
                type = "query";
                break;
            case LOG_ERROR:
                type = "error";
                break;
            case LOG_ALL:
                type = "all";
                break;
        }
        return getRequest("/1/logs?offset=" + offset + "&length=" + length + "&type=" + type, false);
    }

    /**
     * List all existing user keys with their associated ACLs
     */
    protected JSONObject listUserKeys() throws AlgoliaException {
        return getRequest("/1/keys", false);
    }

    /**
     * Delete an existing user key
     */
    protected JSONObject deleteUserKey(String key) throws AlgoliaException {
        return deleteRequest("/1/keys/" + key);
    }

    /**
     * Create a new user key
     *
     * @param params the list of parameters for this key. Defined by a JSONObject that
     * can contains the following values:
     *   - acl: array of string
     *   - indices: array of string
     *   - validity: int
     *   - referers: array of string
     *   - description: string
     *   - maxHitsPerQuery: integer
     *   - queryParameters: string
     *   - maxQueriesPerIPPerHour: integer
     *
     */
    protected JSONObject addUserKey(JSONObject params) throws AlgoliaException {
        return postRequest("/1/keys", params.toString(), false);
    }

    /**
     * Update a user key
     *
     * @param params the list of parameters for this key. Defined by a JSONObject that
     * can contains the following values:
     *   - acl: array of string
     *   - indices: array of string
     *   - validity: int
     *   - referers: array of string
     *   - description: string
     *   - maxHitsPerQuery: integer
     *   - queryParameters: string
     *   - maxQueriesPerIPPerHour: integer
     */
    protected JSONObject updateUserKey(String key, JSONObject params) throws AlgoliaException {
        return putRequest("/1/keys/" + key, params.toString());
    }

    protected JSONObject multipleQueries(List<IndexQuery> queries, String strategy) throws AlgoliaException {
        try {
            JSONArray requests = new JSONArray();
            for (IndexQuery indexQuery : queries) {
                String paramsString = indexQuery.build();
                requests.put(new JSONObject().put("indexName", indexQuery.getIndex()).put("params", paramsString));
            }
            JSONObject body = new JSONObject().put("requests", requests);
            return postRequest("/1/indexes/*/queries?strategy=" + strategy, body.toString(), true);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Custom batch
     *
     * @param actions the array of actions
     * @throws AlgoliaException
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

    private enum Method {
        GET, POST, PUT, DELETE
    }

    protected JSONObject getRequest(String url, boolean search) throws AlgoliaException {
        return _request(Method.GET, url, null, readHostsArray, httpConnectTimeoutMS, search ? httpSearchTimeoutMS : httpSocketTimeoutMS);
    }

    protected JSONObject deleteRequest(String url) throws AlgoliaException {
        return _request(Method.DELETE, url, null, writeHostsArray, httpConnectTimeoutMS, httpSocketTimeoutMS);
    }

    protected JSONObject postRequest(String url, String obj, boolean readOperation) throws AlgoliaException {
        return _request(Method.POST, url, obj, (readOperation ? readHostsArray : writeHostsArray), httpConnectTimeoutMS, (readOperation ? httpSearchTimeoutMS : httpSocketTimeoutMS));
    }

    protected JSONObject putRequest(String url, String obj) throws AlgoliaException {
        return _request(Method.PUT, url, obj, writeHostsArray, httpConnectTimeoutMS, httpSocketTimeoutMS);
    }

    private JSONObject _getAnswerObject(InputStream istream) throws IOException, JSONException {
        InputStreamReader is = new InputStreamReader(istream, "UTF-8");
        StringBuilder builder= new StringBuilder();
        char[] buf = new char[1000];
        int l = 0;
        while (l >= 0) {
            builder.append(buf, 0, l);
            l = is.read(buf);
        }
        JSONTokener tokener = new JSONTokener(builder.toString());
        JSONObject res = new JSONObject(tokener);
        is.close();
        return res;
    }

    private synchronized JSONObject _request(Method m, String url, String json, List<String> hostsArray, int connectTimeout, int readTimeout) throws AlgoliaException {
        HttpRequestBase req;
        HashMap<String, String> errors = new HashMap<String, String>();
        // for each host
        for (String host : hostsArray) {
            switch (m) {
                case DELETE:
                    req = new HttpDelete();
                    break;
                case GET:
                    req = new HttpGet();
                    break;
                case POST:
                    req = new HttpPost();
                    break;
                case PUT:
                    req = new HttpPut();
                    break;
                default:
                    throw new IllegalArgumentException("Method " + m + " is not supported");
            }

            // set URL
            try {
                req.setURI(new URI("https://" + host + url));
            } catch (URISyntaxException e) {
                // never reached
                throw new IllegalStateException(e);
            }

            // set auth headers
            req.setHeader("X-Algolia-Application-Id", this.applicationID);
            req.setHeader("X-Algolia-API-Key", this.apiKey);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                req.setHeader(entry.getKey(), entry.getValue());
            }

            // set user agent
            req.setHeader("User-Agent", "Algolia for Android " + version);

            // set optional headers
            if (this.userToken != null) {
                req.setHeader("X-Algolia-UserToken", this.userToken);
            }
            if (this.tagFilters != null) {
                req.setHeader("X-Algolia-TagFilters", this.tagFilters);
            }
            req.addHeader("Accept-Encoding","gzip");

            // set JSON entity
            if (json != null) {
                if (!(req instanceof HttpEntityEnclosingRequestBase)) {
                    throw new IllegalArgumentException("Method " + m + " cannot enclose entity");
                }
                req.setHeader("Content-type", "application/json");
                try {
                    StringEntity se = new StringEntity(json, "UTF-8");
                    se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    ((HttpEntityEnclosingRequestBase) req).setEntity(se);
                } catch (UnsupportedEncodingException e) {
                    throw new AlgoliaException("Invalid JSON Object: " + json);
                }
            }

            httpClient.getParams().setParameter("http.socket.timeout", readTimeout);
            httpClient.getParams().setParameter("http.connection.timeout", connectTimeout);

            HttpResponse response;
            try {
                response = httpClient.execute(req);
            } catch (IOException e) {
                // on error continue on the next host
                errors.put(host, String.format("%s=%s", e.getClass().getName(), e.getMessage()));
                continue;
            }
            int code = response.getStatusLine().getStatusCode();
            if (code / 100 == 2) {
                // OK
            } else if (code / 100 == 4) {
                String message = "Error detected in backend";
                try {
                    message = _getAnswerObject(response.getEntity().getContent()).getString("message");
                } catch (IOException e) {
                    continue;
                } catch (JSONException e) {
                    throw new AlgoliaException("JSON decode error:" + e.getMessage());
                }
                consumeQuietly(response.getEntity());
                throw new AlgoliaException(message);
            } else {
                try {
                    errors.put(host, EntityUtils.toString(response.getEntity()));
                } catch (IOException e) {
                    errors.put(host, String.valueOf(code));
                }
                consumeQuietly(response.getEntity());
                // KO, continue
                continue;
            }
            try {
                String encoding = response.getEntity().getContentEncoding() != null ? response.getEntity().getContentEncoding().getValue() : null;
                if (encoding != null && encoding.contains("gzip"))
                    return _getAnswerObject(new GZIPInputStream(response.getEntity().getContent()));
                else
                    return _getAnswerObject(response.getEntity().getContent());
            } catch (IOException e) {
                continue;
            } catch (JSONException e) {
                throw new AlgoliaException("JSON decode error:" + e.getMessage());
            }
        }
        StringBuilder builder = new StringBuilder("Hosts unreachable: ");
        Boolean first = true;
        for (Map.Entry<String, String> entry : errors.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.toString());
            first = false;
        }
        throw new AlgoliaException(builder.toString());
    }

    /**
     * Ensures that the entity content is fully consumed and the content stream, if exists,
     * is closed.
     */
    private void consumeQuietly(final HttpEntity entity) {
        if (entity == null) {
            return;
        }
        try {
            if (entity.isStreaming()) {
                InputStream instream = entity.getContent();
                if (instream != null) {
                    instream.close();
                }
            }
        } catch (IOException e) {
            // not fatal
        }
    }
}
