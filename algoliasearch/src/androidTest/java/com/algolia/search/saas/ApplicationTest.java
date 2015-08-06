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

import android.app.Application;
import android.test.ApplicationTestCase;
import android.test.UiThreadTest;

import com.algolia.search.saas.Listener.Index.SearchListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */

public class ApplicationTest extends ApplicationTestCase<Application> {
    APIClient client;
    Index index;
    String indexName;

    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        client = new APIClient(Helpers.app_id, Helpers.api_key);
        indexName = Helpers.safeIndexName("àlgol?à-android");
        index = client.initIndex(indexName);
        JSONObject task = index.addObject(new JSONObject("{'name': 'Thibault'}"));
        index.waitTask(task.getString("taskID"));
    }

    @Override
    protected void tearDown() throws Exception {
        client.deleteIndex(indexName);
        super.tearDown();
    }

    @UiThreadTest
    public void testSearchAsync() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);

        class Listener implements SearchListener {
            @Override
            public void searchResult(Index index, Query query, JSONObject results) {
                try {
                    assertTrue(results.getInt("nbHits") > 0);
                } catch (JSONException e) {
                    fail(String.format("Error during parsing JSON: %s", e.getMessage()));
                }

                signal.countDown();
            }

            @Override
            public void searchError(Index index, Query query, AlgoliaException e) {
                fail(String.format("Error during search: %s", e.getMessage()));
                signal.countDown();
            }
        }

        final Listener searchListener = new Listener();

        index.searchASync(new Query(), searchListener);
        assertTrue(signal.await(30, TimeUnit.SECONDS));
    }
}
