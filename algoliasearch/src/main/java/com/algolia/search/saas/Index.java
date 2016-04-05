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
 * An index from Algolia's API.
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
     * Search inside the index asynchronously
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
     * Search inside the index synchronously
     * @return a JSONObject containing search results
     */
    public JSONObject searchSync(@NonNull Query query) throws AlgoliaException {
        return search(query);
    }

    /**
     * Search inside the index synchronously
     * @return a byte array containing search results
     */
    protected byte[] searchSyncRaw(@NonNull Query query) throws AlgoliaException {
        return searchRaw(query);
    }

    /**
     * Perform a search with disjunctive facets generating as many queries as number of disjunctive facets
     *
     * @param query             the query
     * @param disjunctiveFacets the array of disjunctive facets
     * @param refinements       Map representing the current refinements
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
     * Add an object in this index asynchronously
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
     * Add an object in this index asynchronously
     * <p>
     * WARNING: For performance reasons, the arguments are not cloned. Since the method is executed in the background,
     * you should not modify the object after it has been passed.
     * </p>
     *
     * @param object the object to add.
     *  The object is represented by an associative array
     * @param objectID an objectID you want to attribute to this object
     * (if the attribute already exist the old object will be overwrite)
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
     * Add several objects asynchronously
     *
     * @param objects contains an array of objects to add. If the object contains an objectID
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
     * Override the content of object asynchronously
     *
     * @param object the object to save
     * @param objectID the objectID
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
     * Override the content of several objects asynchronously
     *
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
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
     * Update partially an object asynchronously.
     *
     * @param partialObject the object attributes to override.
     * @param objectID the objectID
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
     * Override the content of several objects asynchronously
     *
     * @param partialObjects contains an array of objects to update (each object must contains an objectID attribute)
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
     * Get an object from this index asynchronously
     *
     * @param objectID the unique identifier of the object to retrieve
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
     * Get an object from this index asynchronously
     *
     * @param objectID the unique identifier of the object to retrieve
     * @param attributesToRetrieve, contains the list of attributes to retrieve as a string separated by ","
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
     * Get several objects from this index asynchronously
     *
     * @param objectIDs the array of unique identifier of objects to retrieve
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
     * Wait the publication of a task on the server asynchronously.
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
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
     * Delete an object from the index asynchronously
     *
     * @param objectID the unique identifier of object to delete
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
     * Delete several objects asynchronously
     *
     * @param objectIDs the array of objectIDs to delete
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
     * Delete all objects matching a query asynchronously
     *
     * @param query the query string
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
     * Get settings of this index asynchronously
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
     * Set settings for this index asynchronously
     *
     * @param settings the settings
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
}