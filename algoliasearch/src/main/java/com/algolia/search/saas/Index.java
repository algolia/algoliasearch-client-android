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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A proxy to an Algolia index.
 * <p>
 * You cannot construct this class directly. Please use {@link APIClient#initIndex(String)} to obtain an instance.
 * </p>
 * <p>
 * WARNING: For performance reasons, arguments to asynchronous methods are not cloned. Therefore, you should not
 * modify mutable arguments after they have been passed (unless explicitly noted).
 * </p>
 */
public class Index extends BaseIndex {
    /**
     * Index initialization (You should not call this initialized yourself)
     */
    protected Index(APIClient client, String indexName) {
        super(client, indexName);
    }

    /**
     * Search inside this index (asynchronously).
     *
     * @param query Search parameters. May be null to use an empty query.
     * @param completionHandler The listener that will be notified of the request's outcome.
     */
    public Request searchASync(@NonNull Query query, CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return search(queryCopy);
            }
        }.start();
    }

    /**
     * Search inside this index (synchronously).
     *
     * @return Search results.
     */
    public JSONObject searchSync(@NonNull Query query) throws AlgoliaException {
        return search(query);
    }

    /**
     * Search inside this index synchronously.
     *
     * @return Search results.
     */
    protected byte[] searchSyncRaw(@NonNull Query query) throws AlgoliaException {
        return searchRaw(query);
    }

    /**
     * Perform a search with disjunctive facets, generating as many queries as number of disjunctive facets (helper).
     *
     * @param query             The query.
     * @param disjunctiveFacets List of disjunctive facets.
     * @param refinements       The current refinements, mapping facet names to a list of values.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchDisjunctiveFacetingAsync(@NonNull Query query, @NonNull List<String> disjunctiveFacets, @NonNull Map<String, List<String>> refinements, CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        final List<String> disjunctiveFacetsCopy = new ArrayList<>(disjunctiveFacets);
        final Map<String, List<String>> refinementsCopy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : refinements.entrySet()) {
            refinementsCopy.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return searchDisjunctiveFaceting(queryCopy, disjunctiveFacetsCopy, refinementsCopy);
            }
        }.start();
    }

    /**
     * Add an object to this index (asynchronously).
     * <p>
     * WARNING: For performance reasons, the arguments are not cloned. Since the method is executed in the background,
     * you should not modify the object after it has been passed.
     * </p>
     *
     * @param object The object to add.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectASync(final @NonNull JSONObject object, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return addObject(object);
            }
        }.start();
    }

    /**
     * Add an object to this index, assigning it the specified object ID (asynchronously).
     * If an object already exists with the same object ID, the existing object will be overwritten.
     * <p>
     * WARNING: For performance reasons, the arguments are not cloned. Since the method is executed in the background,
     * you should not modify the object after it has been passed.
     * </p>
     *
     * @param object The object to add.
     * @param objectID Identifier that you want to assign this object.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectASync(final @NonNull JSONObject object, final @NonNull String objectID, CompletionHandler completionHandler)  {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return addObject(object, objectID);
            }
        }.start();
    }

    /**
     * Add several objects to this index (asynchronously).
     *
     * @param objects Objects to add.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectsASync(final @NonNull JSONArray objects, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return addObjects(objects);
            }
        }.start();
    }

    /**
     * Update an object (asynchronously).
     *
     * @param object New version of the object to update.
     * @param objectID Identifier of the object to update.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request saveObjectASync(final @NonNull JSONObject object, final @NonNull String objectID, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return saveObject(object, objectID);
            }
        }.start();
    }

    /**
     * Update several objects (asynchronously).
     *
     * @param objects Objects to update. Each object must contain an <code>objectID</code> attribute.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request saveObjectsASync(final @NonNull JSONArray objects, @NonNull CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return saveObjects(objects);
            }
        }.start();
    }

    /**
     * Partially update an object (asynchronously).
     *
     * @param partialObject New value/operations for the object.
     * @param objectID Identifier of object to be updated.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectASync(final @NonNull JSONObject partialObject, final @NonNull String objectID, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return partialUpdateObject(partialObject, objectID);
            }
        }.start();
    }

    /**
     * Partially update several objects (asynchronously).
     *
     * @param partialObjects New values/operations for the objects. Each object must contain an <code>objectID</code>
     *                       attribute.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectsASync(final @NonNull JSONArray partialObjects, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return partialUpdateObjects(partialObjects);
            }
        }.start();
    }

    /**
     * Get an object from this index (asynchronously).
     *
     * @param objectID Identifier of the object to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectASync(final @NonNull String objectID, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return getObject(objectID);
            }
        }.start();
    }

    /**
     * Get an object from this index, optionally restricting the retrieved content (asynchronously).
     *
     * @param objectID Identifier of the object to retrieve.
     * @param attributesToRetrieve List of attributes to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectASync(final @NonNull String objectID, final List<String> attributesToRetrieve, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return getObject(objectID, attributesToRetrieve);
            }
        }.start();
    }

    /**
     * Get several objects from this index (asynchronously).
     *
     * @param objectIDs Identifiers of objects to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectsASync(final @NonNull List<String> objectIDs, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return getObjects(objectIDs);
            }
        }.start();
    }

    /**
     * Wait until the publication of a task on the server (helper).
     * All server tasks are asynchronous. This method helps you check that a task is published.
     *
     * @param taskID Identifier of the task (as returned by the server).
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request waitTaskASync(final @NonNull String taskID, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return waitTask(taskID);
            }
        }.start();
    }

    /**
     * Delete an object from this index (asynchronously).
     *
     * @param objectID Identifier of the object to delete.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteObjectASync(final @NonNull String objectID, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return deleteObject(objectID);
            }
        }.start();
    }

    /**
     * Delete several objects from this index (asynchronously).
     *
     * @param objectIDs Identifiers of objects to delete.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteObjectsASync(final @NonNull List<String> objectIDs, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return deleteObjects(objectIDs);
            }
        }.start();
    }

    /**
     * Delete all objects matching a query (helper).
     *
     * @param query The query that objects to delete must match.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteByQueryASync(@NonNull Query query, CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                deleteByQuery(queryCopy);
                return new JSONObject();
            }
        }.start();
    }

    /**
     * Get this index's settings (asynchronously).
     *
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getSettingsASync(CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return getSettings();
            }
        }.start();
    }

    /**
     * Set this index's settings (asynchronously).
     *
     * Please refer to our <a href="https://www.algolia.com/doc/android#index-settings">API documentation</a> for the
     * list of supported settings.
     *
     * @param settings New settings.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request setSettingsASync(final @NonNull JSONObject settings, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return setSettings(settings);
            }
        }.start();
    }

    /**
     * Browse all index content (initial call).
     * This method should be called once to initiate a browse. It will return the first page of results and a cursor,
     * unless the end of the index has been reached. To retrieve subsequent pages, call `browseFromASync` with that
     * cursor.
     *
     * @param query The query parameters for the browse.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request browseASync(@NonNull Query query, CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return browse(queryCopy);
            }
        }.start();
    }

    /**
     * Browse the index from a cursor.
     * This method should be called after an initial call to `browseASync()`. It returns a cursor, unless the end of
     * the index has been reached.
     *
     * @param cursor The cursor of the next page to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request browseFromASync(final @NonNull String cursor, CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return browseFrom(cursor);
            }
        }.start();
    }

    /**
     * Clear this index.
     *
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request clearIndexASync(CompletionHandler completionHandler) {
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return clearIndex();
            }
        }.start();
    }
}