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

package com.algolia.search.saas;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for the `OfflineIndex` class.
 */
public class OfflineClientTest extends OfflineTestBase  {
    @Test
    public void testListIndexes() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        client.listIndexesOfflineAsync(new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                // Check that response is valid.
                assertNotNull(content);
                JSONArray items = content.optJSONArray("items");
                assertNotNull(items);
                for (int i = 0; i < items.length(); ++i) {
                    JSONObject indexDetails = items.optJSONObject(i);
                    assertNotNull(indexDetails);
                    String name = indexDetails.optString("name", null);
                    assertNotNull(name);
                    // Check that the index does not exist yet.
                    assertNotEquals(index.getName(), name);
                }
                index.addObjectAsync(objects.get("snoopy"), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        client.listIndexesOfflineAsync(new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                // Check that response is valid.
                                assertNotNull(content);
                                JSONArray items = content.optJSONArray("items");
                                assertNotNull(items);
                                boolean found = false;
                                for (int i = 0; i < items.length(); ++i) {
                                    JSONObject indexDetails = items.optJSONObject(i);
                                    assertNotNull(indexDetails);
                                    String name = indexDetails.optString("name", null);
                                    assertNotNull(name);
                                    // Check that the index *does* exist.
                                    if (index.getName().equals(name)) {
                                        found = true;
                                    }
                                }
                                assertTrue(found);
                                signal.countDown();
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testDeleteIndex() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = client.getOfflineIndex(Helpers.getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                assertTrue(client.hasOfflineData(index.getName()));
                client.deleteIndexOfflineAsync(index.getName(), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertNotNull(content.optString("deletedAt", null));
                        assertFalse(client.hasOfflineData(index.getName()));
                        signal.countDown();
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testMoveIndex() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex srcIndex = client.getOfflineIndex(Helpers.getMethodName());
        final OfflineIndex dstIndex = client.getOfflineIndex(Helpers.getMethodName() + "_new");
        srcIndex.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                assertTrue(client.hasOfflineData(srcIndex.getName()));
                assertFalse(client.hasOfflineData(dstIndex.getName()));
                client.moveIndexOfflineAsync(srcIndex.getName(), dstIndex.getName(), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertNotNull(content.optString("updatedAt", null));
                        assertFalse(client.hasOfflineData(srcIndex.getName()));
                        assertTrue(client.hasOfflineData(dstIndex.getName()));
                        dstIndex.searchAsync(new Query("woodstock"), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertEquals(1, content.optInt("nbHits"));
                                signal.countDown();
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }
}