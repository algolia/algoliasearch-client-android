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

import com.algolia.search.saas.listeners.DeleteObjectsListener;
import com.algolia.search.saas.listeners.GetObjectsListener;
import com.algolia.search.saas.listeners.IndexingListener;
import com.algolia.search.saas.listeners.SearchDisjunctiveFacetingListener;
import com.algolia.search.saas.listeners.SearchListener;
import com.algolia.search.saas.listeners.SettingsListener;
import com.algolia.search.saas.listeners.WaitTaskListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Contains all the functions related to one index
 * You should use APIClient.initIndex(indexName) to retrieve this object
 */
public class Index extends BaseIndex {
    /**
     * Index initialization (You should not call this initialized yourself)
     */
    protected Index(APIClient client, String indexName) {
        super(client, indexName);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// SEARCH TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class ASyncSearchTask extends AsyncTask<TaskParams.Search, Void, TaskParams.Search> {
        @Override
        protected TaskParams.Search doInBackground(TaskParams.Search... params) {
            TaskParams.Search p = params[0];
            try {
                p.content = search(p.query);
            } catch (AlgoliaException e) {
                p.error = e;
            }
            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.Search p) {
            p.sendResult(Index.this);
        }
    }

    /**
     * Search inside the index asynchronously
     * @param listener the listener that will receive the result or error.
     */
    public void searchASync(Query query, SearchListener listener) {
        TaskParams.Search params = new TaskParams.Search(listener, query);
        new ASyncSearchTask().execute(params);
    }

    /**
     * Search inside the index synchronously
     * @return a JSONObject containing search results
     */
    public JSONObject searchSync(Query query) throws AlgoliaException {
        return search(query);
    }

    /**
     * Search inside the index synchronously
     * @return a byte array containing search results
     */
    protected byte[] searchSyncRaw(Query query) throws AlgoliaException {
        return searchRaw(query);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// SEARCH DISJUNCTIVE FACETING TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private class AsyncSearchDisjunctiveFacetingTask extends AsyncTask<TaskParams.SearchDisjunctiveFaceting, Void, TaskParams.SearchDisjunctiveFaceting> {
        @Override
        protected TaskParams.SearchDisjunctiveFaceting doInBackground(TaskParams.SearchDisjunctiveFaceting... params) {
            TaskParams.SearchDisjunctiveFaceting p = params[0];
            try {
                p.content = searchDisjunctiveFaceting(p.query, p.disjunctiveFacets, p.refinements);
            } catch (AlgoliaException e) {
                p.error = e;
            }
            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.SearchDisjunctiveFaceting p) {
            p.sendResult(Index.this);
        }
    }

    /**
     * Perform a search with disjunctive facets generating as many queries as number of disjunctive facets
     *
     * @param query             the query
     * @param disjunctiveFacets the array of disjunctive facets
     * @param refinements       Map representing the current refinements
     * @param listener the listener that will receive the result or error.
     */
    public void searchDisjunctiveFacetingAsync(Query query, List<String> disjunctiveFacets, Map<String, List<String>> refinements, SearchDisjunctiveFacetingListener listener) {
    TaskParams.SearchDisjunctiveFaceting params = new TaskParams.SearchDisjunctiveFaceting(listener, query, disjunctiveFacets, refinements);
        new AsyncSearchDisjunctiveFacetingTask().execute(params);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// INDEXING TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class AsyncIndexingTask extends AsyncTask<TaskParams.Indexing, Void, TaskParams.Indexing> {
        @Override
        protected TaskParams.Indexing doInBackground(TaskParams.Indexing... params) {
            TaskParams.Indexing p = params[0];
            try {
                switch (p.method) {
                    case AddObject:
                        p.content = addObject(p.object);
                        break;
                    case AddObjectWithObjectID:
                        p.content = addObject(p.object, p.objectID);
                        break;
                    case AddObjects:
                        p.content = addObjects(p.objects);
                        break;
                    case SaveObject:
                        p.content = saveObject(p.object, p.objectID);
                        break;
                    case SaveObjects:
                        p.content = saveObjects(p.objects);
                        break;
                    case PartialUpdateObject:
                        p.content = partialUpdateObject(p.object, p.objectID);
                        break;
                    case PartialUpdateObjects:
                        p.content = partialUpdateObjects(p.objects);
                        break;
                }
            } catch (AlgoliaException e) {
                p.error = e;
            }

            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.Indexing p) {
            p.sendResult(Index.this);
        }
    }

    /**
     * Add an object in this index asynchronously
     *
     * @param object the object to add.
     *  The object is represented by an associative array
     * @param listener the listener that will receive the result or error.
     */
    public void addObjectASync(JSONObject object, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, IndexMethod.AddObject, object);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Add an object in this index asynchronously
     *
     * @param object the object to add.
     *  The object is represented by an associative array
     * @param objectID an objectID you want to attribute to this object
     * (if the attribute already exist the old object will be overwrite)
     * @param listener the listener that will receive the result or error.
     */
    public void addObjectASync(JSONObject object, String objectID, IndexingListener listener)  {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, IndexMethod.AddObjectWithObjectID, object, objectID);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Add several objects asynchronously
     *
     * @param objects contains an array of objects to add. If the object contains an objectID
     * @param listener the listener that will receive the result or error.
     */
    public void addObjectsASync(JSONArray objects, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, IndexMethod.AddObjects, objects);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Override the content of object asynchronously
     *
     * @param object the object to save
     * @param objectID the objectID
     * @param listener the listener that will receive the result or error.
     */
    public void saveObjectASync(JSONObject object, String objectID, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, IndexMethod.SaveObject, object, objectID);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Override the content of several objects asynchronously
     *
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     * @param listener the listener that will receive the result or error.
     */
    public void saveObjectsASync(JSONArray objects, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, IndexMethod.SaveObjects, objects);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Update partially an object asynchronously.
     *
     * @param partialObject the object attributes to override.
     * @param objectID the objectID
     * @param listener the listener that will receive the result or error.
     */
    public void partialUpdateObjectASync(JSONObject partialObject, String objectID, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, IndexMethod.PartialUpdateObject, partialObject, objectID);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Override the content of several objects asynchronously
     *
     * @param partialObjects contains an array of objects to update (each object must contains an objectID attribute)
     * @param listener the listener that will receive the result or error.
     */
    public void partialUpdateObjectsASync(JSONArray partialObjects, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, IndexMethod.PartialUpdateObjects, partialObjects);
        new AsyncIndexingTask().execute(params);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// GET TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class AsyncGetTask extends AsyncTask<TaskParams.GetObjects, Void, TaskParams.GetObjects> {
        @Override
        protected TaskParams.GetObjects doInBackground(TaskParams.GetObjects... params) {
            TaskParams.GetObjects p = params[0];
            try {
                switch (p.method) {
                    case GetObject:
                        p.content = getObject(p.objectID);
                        break;
                    case GetObjectWithAttributesToRetrieve:
                        p.content = getObject(p.objectID, p.attributesToRetrieve);
                        break;
                    case GetObjects:
                        p.content = getObjects(p.objectIDs);
                        break;
                }
            } catch (AlgoliaException e) {
                p.error = e;
            }

            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.GetObjects p) {
            p.sendResult(Index.this);
        }
    }

    /**
     * Get an object from this index asynchronously
     *
     * @param objectID the unique identifier of the object to retrieve
     * @param listener the listener that will receive the result or error.
     */
    public void getObjectASync(String objectID, GetObjectsListener listener) {
        TaskParams.GetObjects params = new TaskParams.GetObjects(listener, IndexMethod.GetObject, objectID);
        new AsyncGetTask().execute(params);
    }

    /**
     * Get an object from this index asynchronously
     *
     * @param objectID the unique identifier of the object to retrieve
     * @param attributesToRetrieve, contains the list of attributes to retrieve as a string separated by ","
     * @param listener the listener that will receive the result or error.
     */
    public void getObjectASync(String objectID, List<String> attributesToRetrieve, GetObjectsListener listener) {
        TaskParams.GetObjects params = new TaskParams.GetObjects(listener, IndexMethod.GetObjectWithAttributesToRetrieve, objectID, attributesToRetrieve);
        new AsyncGetTask().execute(params);
    }

    /**
     * Get several objects from this index asynchronously
     *
     * @param objectIDs the array of unique identifier of objects to retrieve
     * @throws AlgoliaException
     */
    public void getObjectsASync(List<String> objectIDs, GetObjectsListener listener) throws AlgoliaException {
        TaskParams.GetObjects params = new TaskParams.GetObjects(listener, IndexMethod.GetObjects, objectIDs);
        new AsyncGetTask().execute(params);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// WAIT TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class ASyncWaitTask extends AsyncTask<TaskParams.WaitTask, Void, TaskParams.WaitTask> {
        @Override
        protected TaskParams.WaitTask doInBackground(TaskParams.WaitTask... params) {
            TaskParams.WaitTask p = params[0];
            try {
                waitTask(p.taskID);
            } catch (AlgoliaException e) {
                p.error = e;
            }
            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.WaitTask p) {
            p.sendResult(Index.this);
        }
    }

    /**
     * Wait the publication of a task on the server asynchronously.
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @param listener the listener that will receive the result or error.
     */
    public void waitTaskASync(String taskID, WaitTaskListener listener) {
        TaskParams.WaitTask params = new TaskParams.WaitTask(listener, taskID);
        new ASyncWaitTask().execute(params);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// DELETE TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class AsyncDeleteTask extends AsyncTask<TaskParams.DeleteObjects, Void, TaskParams.DeleteObjects> {
        @Override
        protected TaskParams.DeleteObjects doInBackground(TaskParams.DeleteObjects... params) {
            TaskParams.DeleteObjects p = params[0];
            try {
                switch (p.method) {
                    case DeleteObject:
                        p.content = deleteObject(p.objectID);
                        break;
                    case DeleteObjects:
                        p.content = deleteObjects(p.objectIDs);
                        break;
                    case DeleteByQuery:
                        deleteByQuery(p.query);
                        p.content = new JSONObject();
                        break;
                }
            } catch (AlgoliaException e) {
                p.error = e;
            }

            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.DeleteObjects p) {
            p.sendResult(Index.this);
        }
    }

    /**
     * Delete an object from the index asynchronously
     *
     * @param objectID the unique identifier of object to delete
     * @param listener the listener that will receive the result or error.
     */
    public void deleteObjectASync(String objectID, DeleteObjectsListener listener) {
        TaskParams.DeleteObjects params = new TaskParams.DeleteObjects(listener, IndexMethod.DeleteObject, objectID);
        new AsyncDeleteTask().execute(params);
    }

    /**
     * Delete several objects asynchronously
     *
     * @param objectIDs the array of objectIDs to delete
     * @param listener the listener that will receive the result or error.
     */
    public void deleteObjectsASync(List<String> objectIDs, DeleteObjectsListener listener) {
        TaskParams.DeleteObjects params = new TaskParams.DeleteObjects(listener, IndexMethod.DeleteObjects, objectIDs);
        new AsyncDeleteTask().execute(params);
    }

    /**
     * Delete all objects matching a query asynchronously
     *
     * @param query the query string
     * @param listener the listener that will receive the result or error.
     */
    public void deleteByQueryASync(Query query, DeleteObjectsListener listener) {
        query.enableDistinct(false);
        TaskParams.DeleteObjects params = new TaskParams.DeleteObjects(listener, IndexMethod.DeleteByQuery, query);
        new AsyncDeleteTask().execute(params);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// SETTINGS TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class AsyncSettingsTask extends AsyncTask<TaskParams.Settings, Void, TaskParams.Settings> {
        @Override
        protected TaskParams.Settings doInBackground(TaskParams.Settings... params) {
            TaskParams.Settings p = params[0];
            try {
                switch (p.method) {
                    case GetSettings:
                        p.content = getSettings();
                        break;
                    case SetSettings:
                        p.content = setSettings(p.settings);
                        break;
                }
            } catch (AlgoliaException e) {
                p.error = e;
            }

            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.Settings p) {
            p.sendResult(Index.this);
        }
    }

    /**
     * Get settings of this index asynchronously
     *
     * @param listener the listener that will receive the result or error.
     */
    public void getSettingsASync(SettingsListener listener) {
        TaskParams.Settings params = new TaskParams.Settings(listener, IndexMethod.GetSettings);
        new AsyncSettingsTask().execute(params);
    }

    /**
     * Set settings for this index asynchronously
     *
     * @param settings the settings
     * @param listener the listener that will receive the result or error.
     */
    public void setSettingsASync(JSONObject settings, SettingsListener listener) {
        TaskParams.Settings params = new TaskParams.Settings(listener, IndexMethod.SetSettings, settings);
        new AsyncSettingsTask().execute(params);
    }
}