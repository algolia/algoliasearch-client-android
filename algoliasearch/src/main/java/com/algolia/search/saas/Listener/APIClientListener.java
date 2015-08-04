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

package com.algolia.search.saas.Listener;

import com.algolia.search.saas.APIClient;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.IndexQuery;
import com.algolia.search.saas.LogType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Asynchronously receive result of Index asynchronous methods
 */
public interface APIClientListener {

    /**
     * Asynchronously receive result of APIClient.listIndexesASync methods.
     */
    void listIndexesResult(APIClient client, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.listIndexesASync methods.
     */
    void listIndexesError(APIClient client, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.deleteIndexASync methods.
     */
    void deleteIndexResult(APIClient client, String indexName, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.deleteIndexASync methods.
     */
    void deleteIndexError(APIClient client, String indexName, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.copyIndexASync methods.
     */
    void copyIndexResult(APIClient client, String srcIndex, String dstIndex, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.copyIndexASync methods.
     */
    void copyIndexError(APIClient client, String srcIndex, String dstIndex, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.moveIndexASync methods.
     */
    void moveIndexResult(APIClient client, String srcIndex, String dstIndex, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.moveIndexASync methods.
     */
    void moveIndexError(APIClient client, String srcIndex, String dstIndex, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.getLogsASync methods.
     */
    void getLogsResult(APIClient client, int offset, int length, LogType logType, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.getLogsASync methods.
     */
    void getLogsError(APIClient client, int offset, int length, LogType logType, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.listUserKeysASync methods.
     */
    void listUserKeysResult(APIClient client, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.listUserKeysASync methods.
     */
    void listUserKeysError(APIClient client, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.getUserKeyASync methods.
     */
    void getUserKeyResult(APIClient client, String key, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.getUserKeyASync methods.
     */
    void getUserKeyError(APIClient client, String key, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.deleteUserKeyASync methods.
     */
    void deleteUserKeyResult(APIClient client, String key, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.deleteUserKeyASync methods.
     */
    void deleteUserKeyError(APIClient client, String key, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.addUserKeyASync methods.
     */
    void addUserKeyResult(APIClient client, JSONObject param, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.addUserKeyASync methods.
     */
    void addUserKeyError(APIClient client, JSONObject param, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.updateUserKeyASync methods.
     */
    void updateUserKeyResult(APIClient client, String key, JSONObject param, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.updateUserKeyASync methods.
     */
    void updateUserKeyError(APIClient client, String key, JSONObject param, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.updateUserKeyASync methods.
     */
    void multipleQueriesResult(APIClient client, List<IndexQuery> queries, String strategy, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.updateUserKeyASync methods.
     */
    void multipleQueriesError(APIClient client, List<IndexQuery> queries, String strategy, AlgoliaException e);

    /**
     * Asynchronously receive result of APIClient.updateUserKeyASync methods.
     */
    void batchResult(APIClient client, JSONArray actions, JSONObject result);

    /**
     * Asynchronously receive error of APIClient.updateUserKeyASync methods.
     */
    void batchError(APIClient client, JSONArray actions, AlgoliaException e);
}