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

import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.concurrent.RoboExecutorService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for the `OfflineIndex` class.
 */
public class OfflineIndexTest extends RobolectricTestCase  {
    /** Offline client. */
    private OfflineClient client;

    /** Maximum time to wait for each test case. */
    private static long waitTimeout = 5;

    /** Useful object constants. */
    private static Map<String, JSONObject> objects = new HashMap<>();
    static {
        try {
            objects.put("snoopy", new JSONObject()
                .put("objectID", "1")
                .put("name", "Snoopy")
                .put("kind", "dog")
                .put("born", 1967)
                .put("series", new JSONArray().put("Peanuts"))
            );
            objects.put("woodstock", new JSONObject()
                .put("objectID", "2")
                .put("name", "Woodstock")
                .put("kind", "bird")
                .put("born", 1970)
                .put("series", new JSONArray().put("Peanuts"))
            );
        }
        catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        client = new OfflineClient(RuntimeEnvironment.application, Helpers.app_id, Helpers.api_key);
        // NOTE: We don't really control the package name with Robolectric's supplied application.
        // The license below is generated for package "com.algolia.search.saas.android".
        Log.d(this.getClass().getName(), "Robolectric package name: " + RuntimeEnvironment.application.getPackageName());
        client.enableOfflineMode(" AkwBAQH/3YXDBf+GxMAFZBxDbJYBbWVudCBMZSBQcm92b3N0IChBbGdvbGlhKR9jb20uYWxnb2xpYS5zZWFyY2guc2Fhcy5hbmRyb2lkMC0CFAP8/jWtJskE4iRYYWAvHYbOOsf8AhUAsS5RNputtb8FEMkqn0r3MOgPmes=");

        // WARNING: Robolectric cannot work with custom executors in `AsyncTask`, so we substitute the client's
        // executor with a Robolectric-compliant one.
        Whitebox.setInternalState(client, "searchExecutorService", new RoboExecutorService());
    }

    @Test
    public void testAddGetDeleteObject() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        index.addObjectAsync(objects.get("snoopy"), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.getObjectAsync("1", new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals("Snoopy", content.optString("name", null));
                        index.deleteObjectAsync("1", new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                index.getObjectAsync("1", new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(error);
                                        assertEquals(404, error.getStatusCode());
                                        signal.countDown();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testAddWithIDGetDeleteObject() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        index.addObjectAsync(new JSONObject().put("name", "unknown"), "xxx", new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.getObjectAsync("xxx", new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals("unknown", content.optString("name", null));
                        index.deleteObjectAsync("xxx", new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                index.getObjectAsync("xxx", new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(error);
                                        assertEquals(404, error.getStatusCode());
                                        signal.countDown();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testAddGetDeleteObjects() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.getObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        JSONArray results = content.optJSONArray("results");
                        assertNotNull(results);
                        assertEquals(2, results.length());
                        assertEquals("Snoopy", results.optJSONObject(0).optString("name", null));
                        assertEquals("Woodstock", results.optJSONObject(1).optString("name", null));
                        index.deleteObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNull(error);
                                index.getObjectsAsync(Arrays.asList("1", "2"), new CompletionHandler() {
                                    @Override
                                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                                        assertNotNull(error);
                                        assertEquals(404, error.getStatusCode());
                                        signal.countDown();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testSearch() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                final Query query = new Query("snoopy");
                index.searchAsync(query, new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals(1, content.optInt("nbHits"));
                        JSONArray hits = content.optJSONArray("hits");
                        assertNotNull(hits);
                        assertEquals(1, hits.length());
                        assertEquals("Snoopy", hits.optJSONObject(0).optString("name"));
                        signal.countDown();
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testGetSetSettings() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        final JSONObject settings = new JSONObject().put("attributesToIndex", new JSONArray().put("foo").put("bar"));
        index.setSettingsAsync(settings, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.getSettingsAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        assertEquals(settings.optJSONArray("attributesToIndex"), content.optJSONArray("attributesToIndex"));
                        assertNull(settings.opt("attributesToRetrieve"));
                        signal.countDown();
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    @Test
    public void testClear() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.clearIndexAsync(new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.browseAsync(new Query(), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertEquals(0, content.optInt("nbHits", 666));
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
    public void testBrowse() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.browseAsync(new Query().setHitsPerPage(1), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        String cursor = content.optString("cursor", null);
                        assertNotNull(cursor);
                        index.browseFromAsync(cursor, new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertNull(content.optString("cursor", null));
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
    public void testDeleteByQuery() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.deleteByQueryAsync(new Query().setNumericFilters(new JSONArray().put("born < 1970")), new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNull(error);
                        index.browseAsync(new Query(), new CompletionHandler() {
                            @Override
                            public void requestCompleted(JSONObject content, AlgoliaException error) {
                                assertNotNull(content);
                                assertEquals(1, content.optInt("nbHits"));
                                assertNull(content.opt("cursor"));
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
    public void testMultipleQueries() throws Exception {
        final CountDownLatch signal = new CountDownLatch(1);
        final OfflineIndex index = new OfflineIndex(client, getMethodName());
        index.addObjectsAsync(objects.values(), new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                assertNull(error);
                index.multipleQueriesAsync(Arrays.asList(new Query("snoopy"), new Query("woodstock")), null, new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        assertNotNull(content);
                        JSONArray results = content.optJSONArray("results");
                        assertNotNull(results);
                        assertEquals(2, results.length());
                        for (int i = 0; i < results.length(); ++i) {
                            JSONObject result = results.optJSONObject(i);
                            assertNotNull(result);
                            assertEquals(1, result.optInt("nbHits"));
                        }
                        signal.countDown();
                    }
                });
            }
        });
        assertTrue("No callback was called", signal.await(waitTimeout, TimeUnit.SECONDS));
    }

    // ----------------------------------------------------------------------
    // Utils
    // ----------------------------------------------------------------------

    /**
     * Get the method name of the caller.
     *
     * @return The caller's method name.
     */
    private static @NonNull String getMethodName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int index = 0;
        while (index < stack.length) {
            StackTraceElement frame = stack[index];
            if (frame.getClassName().equals(OfflineIndexTest.class.getName())) {
                ++index;
                break;
            }
            ++index;
        }
        assert(index < stack.length);
        return stack[index].getMethodName();
    }
}