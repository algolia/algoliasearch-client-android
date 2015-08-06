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

import android.app.Activity;
import android.os.AsyncTask;

import com.algolia.search.saas.Listener.Index.GetListener;
import com.algolia.search.saas.Listener.Index.IndexingListener;
import com.algolia.search.saas.Listener.Index.SearchListener;
import com.algolia.search.saas.Listener.IndexListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    private enum ASyncIndexTaskKind
    {
        PartialSaveObject,
        PartialSaveObjects,
        PartialSaveObjects2,
        DeleteObject,
        DeleteObjects,
        DeleteByQuery,
        WaitTask,
        GetSettings,
        SetSettings,
    }

    private static class ASyncIndexTaskParams
    {
        public IndexListener listener;
        public Query query;
        public ASyncIndexTaskKind kind;
        public String objectID;
        public List list;
        public JSONObject objectContent;
        public JSONArray objects2;
        public List<String> attributesToRetrieve;

        public ASyncIndexTaskParams(IndexListener listener, ASyncIndexTaskKind kind, String objectID, JSONObject content)
        {
            this.listener = listener;
            this.kind = kind;
            this.objectID = objectID;
            this.objectContent = content;
        }

        public ASyncIndexTaskParams(IndexListener listener, ASyncIndexTaskKind kind, List<?> objects)
        {
            this.listener = listener;
            this.kind = kind;
            this.list = objects;
        }

        public ASyncIndexTaskParams(IndexListener listener, ASyncIndexTaskKind kind, Query query)
        {
            this.listener = listener;
            this.kind = kind;
            this.query = query;
        }

        public ASyncIndexTaskParams(IndexListener listener, ASyncIndexTaskKind kind, JSONArray objects)
        {
            this.listener = listener;
            this.kind = kind;
            this.objects2 = objects;
        }

        public ASyncIndexTaskParams(IndexListener listener, ASyncIndexTaskKind kind, String objectID, List<String> attributesToRetrieve)
        {
            this.listener = listener;
            this.kind = kind;
            this.objectID = objectID;
            this.attributesToRetrieve = attributesToRetrieve;
        }
    }

    private class ASyncIndexTask extends AsyncTask<ASyncIndexTaskParams, Void, Void> {

        private void _sendResult(ASyncIndexTaskParams p, JSONObject res)
        {
            final ASyncIndexTaskParams fp = p;
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

        private void _sendResultImpl(ASyncIndexTaskParams p, JSONObject res)
        {
            switch (p.kind) {
                case WaitTask:
                    p.listener.waitTaskResult(Index.this, p.objectID);
                    break;
                case DeleteObject:
                    p.listener.deleteObjectResult(Index.this, p.objectID, res);
                    break;
                case PartialSaveObject:
                    p.listener.partialUpdateResult(Index.this, p.objectContent, p.objectID, res);
                    break;
                case PartialSaveObjects:
                    p.listener.partialUpdateObjectsResult(Index.this, p.list, res);
                    break;
                case PartialSaveObjects2:
                    p.listener.partialUpdateObjectsResult(Index.this, p.objects2, res);
                    break;
                case DeleteObjects:
                    p.listener.deleteObjectsResult(Index.this, p.objects2, res);
                    break;
                case DeleteByQuery:
                    p.listener.deleteByQueryResult(Index.this);
                    break;
                case GetSettings:
                    p.listener.getSettingsResult(Index.this, res);
                    break;
                case SetSettings:
                    p.listener.setSettingsResult(Index.this, p.objectContent, res);
                    break;
            }
        }

        @Override
        protected Void doInBackground(ASyncIndexTaskParams... params) {
            ASyncIndexTaskParams p = params[0];
            JSONObject res = null;
            switch (p.kind) {
                case WaitTask:
                    try {
                        waitTask(p.objectID);
                    } catch (AlgoliaException e) {
                        p.listener.waitTaskError(Index.this, p.objectID, e);
                        return null;
                    }
                    break;
                case DeleteObject:
                    try {
                        res = deleteObject(p.objectID);
                    } catch (AlgoliaException e) {
                        p.listener.deleteObjectError(Index.this, p.objectID, e);
                        return null;
                    }
                    break;
                case DeleteByQuery:
                    try {
                        deleteByQuery(p.query);
                    } catch (AlgoliaException e) {
                        p.listener.deleteByQueryError(Index.this, p.query, e);
                        return null;
                    }
                    break;
                case PartialSaveObject:
                    try {
                        res = partialUpdateObject(p.objectContent, p.objectID);
                    } catch (AlgoliaException e) {
                        p.listener.partialUpdateError(Index.this, p.objectContent, p.objectID, e);
                        return null;
                    }
                    break;
                case PartialSaveObjects:
                    try {
                        res = partialUpdateObjects(p.list);
                    } catch (AlgoliaException e) {
                        p.listener.partialUpdateObjectsError(Index.this, p.list, e);
                        return null;
                    }
                    break;
                case PartialSaveObjects2:
                    try {
                        res = partialUpdateObjects(p.objects2);
                    } catch (AlgoliaException e) {
                        p.listener.partialUpdateObjectsError(Index.this, p.objects2, e);
                        return null;
                    }
                    break;
                case DeleteObjects:
                    try {
                        res = deleteObjects2(p.list);
                    } catch (AlgoliaException e) {
                        p.listener.deleteObjectsError(Index.this, p.list, e);
                        return null;
                    }
                    break;
                case GetSettings:
                    try {
                        res = getSettings();
                    } catch (AlgoliaException e) {
                        p.listener.getSettingsError(Index.this, e);
                        return null;
                    }
                    break;
                case SetSettings:
                    try {
                        res = setSettings(p.objectContent);
                    } catch (AlgoliaException e) {
                        p.listener.setSettingsError(Index.this, p.objectContent, e);
                        return null;
                    }
                    break;
            }
            _sendResult(p, res);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
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
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void searchASync(Query query, SearchListener listener) {
        TaskParams.Search params = new TaskParams.Search(listener, query);
        new ASyncSearchTask().execute(params);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// INDEXING TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class AsyncIndexingTask extends AsyncTask<TaskParams.Indexing, Void, TaskParams.Indexing> {
        @Override
        protected TaskParams.Indexing doInBackground(TaskParams.Indexing... params) {
            TaskParams.Indexing p = params[0];
            try {
                switch (p.kind) {
                    case AddObject:
                        p.content = addObject(p.object);
                        break;
                    case AddObjectWithObjectID:
                        p.content = addObject(p.object, p.objectID);
                    case AddObjects:
                        p.content = addObjects(p.objects);
                        break;
                    case SaveObject:
                        p.content = saveObject(p.object, p.objectID);
                        break;
                    case SaveObjects:
                        p.content = saveObjects(p.objects);
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
     * @param obj the object to add. 
     *  The object is represented by an associative array
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void addObjectASync(JSONObject obj, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, TaskKind.AddObject, obj);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Add an object in this index asynchronously
     *
     * @param obj the object to add. 
     *  The object is represented by an associative array
     * @param objectID an objectID you want to attribute to this object 
     * (if the attribute already exist the old object will be overwrite)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void addObjectASync(JSONObject obj, String objectID, IndexingListener listener)  {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, TaskKind.AddObjectWithObjectID, obj, objectID);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Add several objects asynchronously
     *
     * @param objects contains an array of objects to add. If the object contains an objectID
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void addObjectsASync(JSONArray objects, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, TaskKind.AddObjects, objects);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Override the content of object asynchronously
     *
     * @param obj the object to save
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void saveObjectASync(JSONObject obj, String objectID, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, TaskKind.SaveObject, obj, objectID);
        new AsyncIndexingTask().execute(params);
    }

    /**
     * Override the content of several objects asynchronously
     *
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void saveObjectsASync(JSONArray objects, IndexingListener listener) {
        TaskParams.Indexing params = new TaskParams.Indexing(listener, TaskKind.SaveObjects, objects);
        new AsyncIndexingTask().execute(params);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    /// GET TASK
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private class AsyncGetTask extends AsyncTask<TaskParams.Get, Void, TaskParams.Get> {
        @Override
        protected TaskParams.Get doInBackground(TaskParams.Get... params) {
            TaskParams.Get p = params[0];
            try {
                switch (p.kind) {
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
        protected void onPostExecute(TaskParams.Get p) {
            p.sendResult(Index.this);
        }
    }

    /**
     * Get an object from this index asynchronously
     *
     * @param objectID the unique identifier of the object to retrieve
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void getObjectASync(String objectID, GetListener listener) {
        TaskParams.Get params = new TaskParams.Get(listener, TaskKind.GetObject, objectID);
        new AsyncGetTask().execute(params);
    }

    /**
     * Get an object from this index asynchronously
     *
     * @param objectID the unique identifier of the object to retrieve
     * @param attributesToRetrieve, contains the list of attributes to retrieve as a string separated by ","
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void getObjectASync(String objectID, List<String> attributesToRetrieve, GetListener listener) {
        TaskParams.Get params = new TaskParams.Get(listener, TaskKind.GetObjectWithAttributesToRetrieve, objectID, attributesToRetrieve);
        new AsyncGetTask().execute(params);
    }

    /**
     * Get several objects from this index asynchronously
     *
     * @param objectIDs the array of unique identifier of objects to retrieve
     * @throws AlgoliaException
     */
    public void getObjectsASync(List<String> objectIDs, GetListener listener) throws AlgoliaException {
        TaskParams.Get params = new TaskParams.Get(listener, TaskKind.GetObjects, objectIDs);
        new AsyncGetTask().execute(params);
    }










    /**
     * Update partially an object asynchronously (only update attributes passed in argument)
     *
     * @param partialObject the object attributes to override, the 
     *  object must contains an objectID attribute
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void partialUpdateObjectASync(JSONObject partialObject, String objectID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.PartialSaveObject, objectID, partialObject);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Override the content of several objects asynchronously
     *
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void partialUpdateObjectsASync(JSONArray objects, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.PartialSaveObjects2, objects);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Partially Override the content of several objects asynchronously
     *
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void partialUpdateObjectsASync(List<JSONObject> objects, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.PartialSaveObjects, objects);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Delete an object from the index asynchronously
     *
     * @param objectID the unique identifier of object to delete
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void deleteObjectASync(String objectID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.DeleteObject, objectID, (List)null);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Delete several objects asynchronously
     *
     * @param ids the array of objectIDs to delete
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void deleteObjectsASync(List<String> ids, IndexListener listener) throws AlgoliaException {
        List<JSONObject> objects = new ArrayList<JSONObject>();
        for (String id : ids) {
            try {
                objects.add(new JSONObject().put("objectID", id));
            } catch (JSONException e) {
                throw new AlgoliaException(e.getMessage());
            }
        }
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.DeleteObjects, objects);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Delete all objects matching a query asynchronously
     *
     * @param query the query string
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void deleteByQueryASync(Query query, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.DeleteByQuery, query);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Wait the publication of a task on the server asynchronously. 
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void waitTaskASync(String taskID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.WaitTask, taskID, (List)null);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Get settings of this index asynchronously
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void getSettingsASync(IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.GetSettings, null, (List)null);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Set settings for this index asynchronously
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIThread
     */
    public void setSettingsASync(JSONObject settings, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.SetSettings, null, settings);
        new ASyncIndexTask().execute(params);
    }
}