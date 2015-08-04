package com.algolia.search.saas;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;

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

/**
 * Entry point in the Java API.
 * You should instantiate a Client object with your ApplicationID, ApiKey and Hosts 
 * to start using Algolia Search API
 */
public class APIClient extends BaseAPIClient {
    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     */
    public APIClient(String applicationID, String apiKey) {
        this(applicationID, apiKey, null);
    }

    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param hostsArray the list of hosts that you have received for the service
     */
    public APIClient(String applicationID, String apiKey, List<String> hostsArray) {
        this(applicationID, apiKey, hostsArray, false, null);
    }

    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param enableDsn set to true if your account has the Distributed Search Option
     */
    public APIClient(String applicationID, String apiKey, boolean enableDsn) {
        this(applicationID, apiKey, null, enableDsn, null);
    }

    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param hostsArray the list of hosts that you have received for the service
     * @param enableDsn set to true if your account has the Distributed Search Option
     * @param dsnHost override the automatic computation of dsn hostname
     */
    public APIClient(String applicationID, String apiKey, List<String> hostsArray, boolean enableDsn, String dsnHost) {
        super(applicationID, apiKey, hostsArray, enableDsn, dsnHost);
    }

    /**
     * Get the index object initialized (no server call needed for initialization)
     *
     * @param indexName the name of index
     */
    public Index initIndex(String indexName) {
        return new Index(this, indexName);
    }

    private enum ASyncAPIClientTaskKind
    {
        ListIndexes,
        DeleteIndex,
        CopyIndex,
        MoveIndex,
        MultipleQueries,
        Batch,
        GetLogs,
        ListUserKeys,
        GetUserKey,
        AddUserKey,
        UpdateUserKey,
        DeleteUserKey
    };

    private static class ASyncAPIClientTaskParams
    {
        public APIClientListener listener;
        public ASyncAPIClientTaskKind kind;
        public String strParam;
        public String strParam2;
        public int intParam;
        public int intParam2;
        public LogType logType;
        public JSONObject jsonParam;
        public JSONArray jsonArrayParam;
        public List<IndexQuery> queries;

        public ASyncAPIClientTaskParams(APIClientListener listener, ASyncAPIClientTaskKind kind) {
            this.listener = listener;
            this.kind = kind;
        }

        public ASyncAPIClientTaskParams(APIClientListener listener, ASyncAPIClientTaskKind kind, String strParam) {
            this.listener = listener;
            this.kind = kind;
            this.strParam = strParam;
        }

        public ASyncAPIClientTaskParams(APIClientListener listener, ASyncAPIClientTaskKind kind, String strParam, String strParam2) {
            this.listener = listener;
            this.kind = kind;
            this.strParam = strParam;
            this.strParam2 = strParam2;
        }

        public ASyncAPIClientTaskParams(APIClientListener listener, ASyncAPIClientTaskKind kind, int intParam, int intParam2, LogType logType) {
            this.listener = listener;
            this.kind = kind;
            this.intParam = intParam;
            this.intParam2 = intParam2;
            this.logType = logType;
        }

        public ASyncAPIClientTaskParams(APIClientListener listener, ASyncAPIClientTaskKind kind, JSONObject jsonParam) {
            this.listener = listener;
            this.kind = kind;
            this.jsonParam = jsonParam;
        }

        public ASyncAPIClientTaskParams(APIClientListener listener, ASyncAPIClientTaskKind kind, String strParam, JSONObject jsonParam) {
            this.listener = listener;
            this.kind = kind;
            this.strParam = strParam;
            this.jsonParam = jsonParam;
        }

        public ASyncAPIClientTaskParams(APIClientListener listener, ASyncAPIClientTaskKind kind, List<IndexQuery> queries, String strParam) {
            this.listener = listener;
            this.kind = kind;
            this.queries = queries;
            this.strParam = strParam;
        }

        public ASyncAPIClientTaskParams(APIClientListener listener, ASyncAPIClientTaskKind kind, JSONArray jsonArrayParam) {
            this.listener = listener;
            this.kind = kind;
            this.jsonArrayParam = jsonArrayParam;
        }
    }

    private class ASyncAPIClientTask extends AsyncTask<ASyncAPIClientTaskParams, Void, Void> {

        private void _sendResult(ASyncAPIClientTaskParams p, JSONObject res)
        {
            final ASyncAPIClientTaskParams fp = p;
            final JSONObject fres = res;
            if (p.listener instanceof Activity) {
                ((Activity)p.listener).runOnUiThread(new Runnable() {
                    public void run() {
                        _sendResultImpl(fp, fres);
                    }
                });
            } else {
                _sendResultImpl(p, res);
            }
        }

        private void _sendResultImpl(ASyncAPIClientTaskParams p, JSONObject res)
        {
            switch (p.kind) {
                case ListIndexes:
                    p.listener.listIndexesResult(APIClient.this, res);
                    break;
                case DeleteIndex:
                    p.listener.deleteIndexResult(APIClient.this, p.strParam, res);
                    break;
                case MoveIndex:
                    p.listener.moveIndexResult(APIClient.this, p.strParam, p.strParam2, res);
                    break;
                case CopyIndex:
                    p.listener.copyIndexResult(APIClient.this, p.strParam, p.strParam2, res);
                    break;
                case GetLogs:
                    p.listener.getLogsResult(APIClient.this, p.intParam, p.intParam2, p.logType, res);
                    break;
                case GetUserKey:
                    p.listener.getUserKeyResult(APIClient.this, p.strParam, res);
                    break;
                case ListUserKeys:
                    p.listener.listUserKeysResult(APIClient.this, res);
                    break;
                case DeleteUserKey:
                    p.listener.deleteUserKeyResult(APIClient.this, p.strParam, res);
                    break;
                case AddUserKey:
                    p.listener.addUserKeyResult(APIClient.this, p.jsonParam, res);
                    break;
                case UpdateUserKey:
                    p.listener.updateUserKeyResult(APIClient.this, p.strParam, p.jsonParam, res);
                    break;
                case MultipleQueries:
                    p.listener.multipleQueriesResult(APIClient.this, p.queries, p.strParam, res);
                    break;
                case Batch:
                    p.listener.batchResult(APIClient.this, p.jsonArrayParam, res);
                    break;
            }
        }

        @Override
        protected Void doInBackground(ASyncAPIClientTaskParams... params) {
            ASyncAPIClientTaskParams p = params[0];
            JSONObject res = null;
            switch (p.kind) {
                case ListIndexes:
                    try {
                        res = listIndexes();
                    } catch (AlgoliaException e) {
                        p.listener.listIndexesError(APIClient.this, e);
                        return null;
                    }
                    break;
                case DeleteIndex:
                    try {
                        res = deleteIndex(p.strParam);
                    } catch (AlgoliaException e) {
                        p.listener.deleteIndexError(APIClient.this, p.strParam, e);
                    }
                    break;
                case MoveIndex:
                    try {
                        res = moveIndex(p.strParam, p.strParam2);
                    } catch (AlgoliaException e) {
                        p.listener.moveIndexError(APIClient.this, p.strParam, p.strParam2, e);
                    }
                    break;
                case CopyIndex:
                    try {
                        res = copyIndex(p.strParam, p.strParam2);
                    } catch (AlgoliaException e) {
                        p.listener.copyIndexError(APIClient.this, p.strParam, p.strParam2, e);
                    }
                    break;
                case GetLogs:
                    try {
                        res = getLogs(p.intParam, p.intParam2, p.logType);
                    } catch (AlgoliaException e) {
                        p.listener.getLogsError(APIClient.this, p.intParam, p.intParam2, p.logType, e);
                    }
                    break;
                case GetUserKey:
                    try {
                        res = getUserKeyACL(p.strParam);
                    } catch (AlgoliaException e) {
                        p.listener.getUserKeyError(APIClient.this, p.strParam, e);
                    }
                    break;
                case ListUserKeys:
                    try {
                        res = listUserKeys();
                    } catch (AlgoliaException e) {
                        p.listener.listUserKeysError(APIClient.this, e);
                    }
                    break;
                case DeleteUserKey:
                    try {
                        res = deleteUserKey(p.strParam);
                    } catch (AlgoliaException e) {
                        p.listener.deleteUserKeyError(APIClient.this, p.strParam, e);
                    }
                    break;
                case AddUserKey:
                    try {
                        res = addUserKey(p.jsonParam);
                    } catch (AlgoliaException e) {
                        p.listener.addUserKeyError(APIClient.this, p.jsonParam, e);
                    }
                    break;
                case UpdateUserKey:
                    try {
                        res = updateUserKey(p.strParam, p.jsonParam);
                    } catch (AlgoliaException e) {
                        p.listener.updateUserKeyError(APIClient.this, p.strParam, p.jsonParam, e);
                    }
                    break;
                case MultipleQueries:
                    try {
                        res = multipleQueries(p.queries, p.strParam);
                    } catch (AlgoliaException e) {
                        p.listener.multipleQueriesError(APIClient.this, p.queries, p.strParam, e);
                    }
                    break;
                case Batch:
                    try {
                        res = batch(p.jsonArrayParam);
                    } catch (AlgoliaException e) {
                        p.listener.batchError(APIClient.this, p.jsonArrayParam, e);
                    }
                    break;
            }
            _sendResult(p, res);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    /**
     * List all existing user keys with their associated ACLs
     *
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void listIndexesASync(APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.ListIndexes);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Delete an index
     *
     * @param indexName the name of index to delete
     * return an object containing a "deletedAt" attribute
     */
    public void deleteIndexASync(String indexName, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.DeleteIndex, indexName);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Move an existing index.
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     */
    public void moveIndexASync(String srcIndexName, String dstIndexName, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.MoveIndex, srcIndexName, dstIndexName);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Copy an existing index.
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     */
    public void copyIndexASync(String srcIndexName, String dstIndexName, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.CopyIndex, srcIndexName, dstIndexName);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Return last logs entries.
     * @param offset Specify the first entry to retrieve (0-based, 0 is the most recent log entry).
     * @param length Specify the maximum number of entries to retrieve starting at offset. Maximum allowed value: 1000.
     * @param logType Specify the type of log to retrieve
     */
    public void getLogsASync(int offset, int length, LogType logType, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.GetLogs, offset, length, logType);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * List all existing user keys with their associated ACLs
     */
    public void listUserKeysASync(APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.ListUserKeys);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Get ACL of a user key
     */
    public void getUserKeyACLASync(String key, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.GetUserKey, key);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Delete an existing user key
     */
    public void deleteUserKeyASync(String key, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.DeleteUserKey, key);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Create a new user key
     */
    public void addUserKeyASync(JSONObject parameters, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.AddUserKey, parameters);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Update a user key asynchronously
     *
     * @param parameters the list of parameters for this key. Defined by a JSONObject that
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
    public void updateUserKeyASync(String key, JSONObject parameters, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.UpdateUserKey, key, parameters);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * This method allows to query multiple indexes with one API call asynchronously
     */
    public void multipleQueriesASync(List<IndexQuery> queries, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.MultipleQueries, queries, "none");
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * This method allows to query multiple indexes with one API call asynchronously
     */
    public void multipleQueriesASync(List<IndexQuery> queries, String strategy, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.MultipleQueries, queries, strategy);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Custom batch asynchronous
     *
     * @param actions the array of actions
     */
    public void batchASync(JSONArray actions, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.Batch, actions);
        new ASyncAPIClientTask().execute(params);
    }

    /**
     * Custom batch asynchronous
     *
     * @param actions the array of actions
     */
    public void batchASync(List<JSONObject> actions, APIClientListener listener) {
        ASyncAPIClientTaskParams params = new ASyncAPIClientTaskParams(listener, ASyncAPIClientTaskKind.Batch, new JSONArray(actions));
        new ASyncAPIClientTask().execute(params);
    }
}