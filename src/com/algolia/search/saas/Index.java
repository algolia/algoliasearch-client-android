package com.algolia.search.saas;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;

/*
 * Copyright (c) 2013 Algolia
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
 * Contains all the functions related to one index
 * You should use APIClient.initIndex(indexName) to retrieve this object
 */
public class Index {
    private APIClient client;
    private String indexName;
    private String originalIndexName;
    
    /**
     * Index initialization (You should not call this initialized yourself)
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param hostsArray the list of hosts that you have received for the service
     */
    protected Index(APIClient client, String indexName) {
        try {
            this.client = client;
            this.indexName = URLEncoder.encode(indexName, "UTF-8");
            this.originalIndexName = indexName;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public String getIndexName() {
        return originalIndexName;
    }

    /**
     * Add an object in this index
     * 
     * @param content contains the object to add inside the index. 
     *  The object is represented by an associative array
    */
    public JSONObject addObject(String obj) throws AlgoliaException {
        return client._postRequest("/1/indexes/" + indexName, obj);
    }
   
    /**
     * Add an object in this index
     * 
     * @param content contains the object to add inside the index. 
     *  The object is represented by an associative array
     * @param objectID an objectID you want to attribute to this object 
     * (if the attribute already exist the old object will be overwrite)
     */
    public JSONObject addObject(String obj, String objectID) throws AlgoliaException {
        try {
            return client._putRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8"), obj);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Add an object in this index asynchronously
     * 
     * @param content contains the object to add inside the index. 
     *  The object is represented by an associative array
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void addObjectASync(String obj, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.AddObject, null, obj);
        new ASyncIndexTask().execute(params);
    }
   
    /**
     * Add an object in this index asynchronously
     * 
     * @param content contains the object to add inside the index. 
     *  The object is represented by an associative array
     * @param objectID an objectID you want to attribute to this object 
     * (if the attribute already exist the old object will be overwrite)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void addObjectASync(String obj, String objectID, IndexListener listener)  {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.AddObject, objectID, obj);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Add several objects
     * 
     * @param objects contains an array of objects to add. If the object contains an objectID
     */
    public JSONObject addObjects(List<JSONObject> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (JSONObject obj : objects) {
                JSONObject action = new JSONObject();
                action.put("action", "addObject");
                action.put("body",obj);
                array.put(action);
            }
            JSONObject content = new JSONObject();
            content.put("requests", array);
            return client._postRequest("/1/indexes/" + indexName + "/batch", content.toString());
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Add several objects asynchronously
     * 
     * @param objects contains an array of objects to add. If the object contains an objectID
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void addObjectsASync(List<JSONObject> objects, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.AddObjects, objects);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Get an object from this index
     * 
     * @param objectID the unique identifier of the object to retrieve
     */
    public JSONObject getObject(String objectID) throws AlgoliaException {
        try {
            return client._getRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get an object from this index
     * 
     * @param objectID the unique identifier of the object to retrieve
     * @param attributesToRetrieve, contains the list of attributes to retrieve as a string separated by ","
     */
    public JSONObject getObject(String objectID,  List<String> attributesToRetrieve) throws AlgoliaException {
        try {
            StringBuilder params = new StringBuilder();
            params.append("?attributes=");
            for (int i = 0; i < attributesToRetrieve.size(); ++i) {
                if (i > 0)
                    params.append(",");
                params.append(URLEncoder.encode(attributesToRetrieve.get(i), "UTF-8"));
            }
            return client._getRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8") + params.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Get an object from this index asynchronously
     * 
     * @param objectID the unique identifier of the object to retrieve
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void getObjectASync(String objectID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.GetObject, objectID, null);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Get an object from this index asynchronously
     * 
     * @param objectID the unique identifier of the object to retrieve
     * @param attributesToRetrieve, contains the list of attributes to retrieve as a string separated by ","
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void getObjectASync(String objectID, List<String> attributesToRetrieve, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, objectID, attributesToRetrieve);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Update partially an object (only update attributes passed in argument)
     * 
     * @param partialObject contains the object attributes to override, the 
     *  object must contains an objectID attribute
     */
    public JSONObject partialUpdateObject(String partialObject, String objectID) throws AlgoliaException {
        try {
            return client._postRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8") + "/partial", partialObject);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Update partially an object asynchronously (only update attributes passed in argument)
     * 
     * @param partialObject contains the object attributes to override, the 
     *  object must contains an objectID attribute
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void partialUpdateObjectASync(String partialObject, String objectID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.PartialSaveObject, objectID, partialObject);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Override the content of object
     * 
     * @param object contains the object to save
     */
    public JSONObject saveObject(String object, String objectID) throws AlgoliaException {
        try {
            return client._putRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8"), object);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override the content of object asynchronously
     * 
     * @param object contains the object to save
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void saveObjectASync(String object, String objectID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.SaveObject, objectID, object);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Override the content of several objects
     * 
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     */
    public JSONObject saveObjects(List<JSONObject> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (JSONObject obj : objects) {
                JSONObject action = new JSONObject();
                action.put("action", "updateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body",obj);
                array.put(action);
            }
            JSONObject content = new JSONObject();
            content.put("requests", array);
            return client._postRequest("/1/indexes/" + indexName + "/batch", content.toString());
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }
    
    /**
     * Override the content of several objects asynchronously
     * 
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void saveObjectsASync(List<JSONObject> objects, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.SaveObjects, objects);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Delete an object from the index 
     * 
     * @param objectID the unique identifier of object to delete
     */
    public JSONObject deleteObject(String objectID) throws AlgoliaException {
        try {
            return client._deleteRequest("/1/indexes/" + indexName + "/" + URLEncoder.encode(objectID, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Delete an object from the index asynchronously
     * 
     * @param objectID the unique identifier of object to delete
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void deleteObjectASync(String objectID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.DeleteObject, objectID, null);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Search inside the index
     */
    public JSONObject search(Query query) throws AlgoliaException {
        String paramsString = query.getQueryString();
        if (paramsString.length() > 0)
            return client._getRequest("/1/indexes/" + indexName + "?" + paramsString);
        else
            return client._getRequest("/1/indexes/" + indexName);
    }

    /**
     * Search inside the index asynchronously
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void searchASync(Query query, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, query);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Wait the publication of a task on the server. 
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @param timeBeforeRetry the time in milliseconds before retry (default = 100ms)
     */
    public void waitTask(String taskID) throws AlgoliaException {
        try {
            while (true) {
                JSONObject obj = client._getRequest("/1/indexes/" + indexName + "/task/" + URLEncoder.encode(taskID, "UTF-8"));
                if (obj.getString("status").equals("published"))
                    return;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait the publication of a task on the server asynchronously. 
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @param timeBeforeRetry the time in milliseconds before retry (default = 100ms)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void waitTaskASync(String taskID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.WaitTask, taskID, null);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Get settings of this index
     */
    public JSONObject getSettings() throws AlgoliaException {
        return client._getRequest("/1/indexes/" + indexName + "/settings");
    }

    /**
     * Get settings of this index asynchronously
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void getSettingsASync(IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.GetSettings, null, null);
        new ASyncIndexTask().execute(params);
    }
    
    /**
     * Set settings for this index
     * 
     * @param settigns the settings object that can contains :
     *  - minWordSizeForApprox1 (integer) the minimum number of characters to accept one typo (default = 3)
     *  - minWordSizeForApprox2: (integer) the minimum number of characters to accept two typos (default = 7)
     *  - hitsPerPage: (integer) the number of hits per page (default = 10)
     *  - attributesToRetrieve: (array of strings) default list of attributes to retrieve for objects
     *  - attributesToHighlight: (array of strings) default list of attributes to highlight
     *  - attributesToIndex: (array of strings) the list of fields you want to index. 
     *    By default all textual attributes of your objects are indexed, but you should update it to get optimal 
     *    results. This parameter has two important uses:
     *       - Limit the attributes to index. 
     *         For example if you store a binary image in base64, you want to store it in the index but you 
     *         don't want to use the base64 string for search.
     *       - Control part of the ranking (see the ranking parameter for full explanation). 
     *         Matches in attributes at the beginning of the list will be considered more important than matches 
     *         in attributes further down the list.
     *  - ranking: (array of strings) controls the way results are sorted. 
     *     We have four available criteria: 
     *       - typo (sort according to number of typos), 
     *       - geo: (sort according to decreassing distance when performing a geo-location based search),
     *       - position (sort according to the matching attribute), 
     *       - custom which is user defined
     *     (the standard order is ["typo", "geo", position", "custom"])
     *  - customRanking: (array of strings) lets you specify part of the ranking. 
     *    The syntax of this condition is an array of strings containing attributes prefixed 
     *    by asc (ascending order) or desc (descending order) operator.
     */
    public JSONObject setSettings(String settings) throws AlgoliaException {
        return client._putRequest("/1/indexes/" + indexName + "/settings", settings);
    }
    
    /**
     * Set settings for this index asynchronously
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void setSettingsASync(String settings, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.SetSettings, null, settings);
        new ASyncIndexTask().execute(params);
    }
    
    private enum ASyncIndexTaskKind
    {
        GetObject,
        AddObject,
        AddObjects,
        SaveObject,
        SaveObjects,
        PartialSaveObject,
        DeleteObject,
        WaitTask,
        Query,
        GetSettings,
        SetSettings
    };
    
    private static class ASyncIndexTaskParams
    {
        public IndexListener listener;
        public Query query;
        public ASyncIndexTaskKind kind;
        public String objectID;
        public String objectContent;
        public List<JSONObject> objects;
        public List<String> attributesToRetrieve;
        
        public ASyncIndexTaskParams(IndexListener listener, Query query) {
            this.listener = listener;
            this.query = query;
            this.kind = ASyncIndexTaskKind.Query;
        }
        
        public ASyncIndexTaskParams(IndexListener listener, ASyncIndexTaskKind kind, String objectID, String content)
        {
            this.listener = listener;
            this.kind = kind;
            this.objectID = objectID;
            this.objectContent = content;
        }
        
        public ASyncIndexTaskParams(IndexListener listener, ASyncIndexTaskKind kind, List<JSONObject> objects)
        {
            this.listener = listener;
            this.kind = kind;
            this.objects = objects;
        }
        public ASyncIndexTaskParams(IndexListener listener, String objectID, List<String> attributesToRetrieve)
        {
            this.listener = listener;
            this.kind = ASyncIndexTaskKind.GetObject;
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
            case AddObject:
                p.listener.addObjectResult(Index.this, p.objectContent, res);
                break;
            case AddObjects:
                p.listener.addObjectsResult(Index.this, p.objects, res);
                break;
            case WaitTask:
                p.listener.waitTaskResult(Index.this, p.objectID);
                break;
            case SaveObject:
                p.listener.saveObjectResult(Index.this, p.objectContent, p.objectID, res);
                break;
            case SaveObjects:
                p.listener.saveObjectsResult(Index.this, p.objects, res);
                break;
            case DeleteObject:
                p.listener.deleteObjectResult(Index.this, p.objectID, res);
                break;
            case PartialSaveObject:
                p.listener.partialUpdateResult(Index.this, p.objectContent, p.objectID, res);
                break;
            case GetObject:
                p.listener.getObjectResult(Index.this, p.objectID, res);
                break;
            case Query:
                p.listener.searchResult(Index.this, p.query, res);
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
            case AddObject:
                try {
                    res = (p.objectID == null) ? addObject(p.objectContent) : addObject(p.objectContent, p.objectID);
                } catch (AlgoliaException e) {
                    p.listener.addObjectError(Index.this, p.objectContent, e);
                    return null;
                }
                break;
            case AddObjects:
                try {
                    res = addObjects(p.objects);
                } catch (AlgoliaException e) {
                    p.listener.addObjectsError(Index.this, p.objects, e);
                    return null;
                }
                break;
            case WaitTask:
                try {
                    waitTask(p.objectID);
                } catch (AlgoliaException e) {
                    p.listener.waitTaskError(Index.this, p.objectID, e);
                    return null;
                }
                break;
            case SaveObject:
                try {
                    res = saveObject(p.objectContent, p.objectID);
                } catch (AlgoliaException e) {
                    p.listener.saveObjectError(Index.this, p.objectContent, p.objectID, e);
                    return null;
                }
                break;
            case SaveObjects:
                try {
                    res = saveObjects(p.objects);
                } catch (AlgoliaException e) {
                    p.listener.saveObjectsError(Index.this, p.objects, e);
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
            case PartialSaveObject:
                try {
                    res = partialUpdateObject(p.objectContent, p.objectID);
                } catch (AlgoliaException e) {
                    p.listener.partialUpdateError(Index.this, p.objectContent, p.objectID, e);
                    return null;
                }
                break;
            case GetObject:
                try {
                    if (p.attributesToRetrieve == null) {
                        res = getObject(p.objectID);
                    } else {
                        res = getObject(p.objectID, p.attributesToRetrieve);
                    }
                } catch (AlgoliaException e) {
                    p.listener.getObjectError(Index.this, p.objectID, e);
                    return null;
                }
                break;
            case Query:
                try {
                    res = search(p.query);
                } catch (AlgoliaException e) {
                    p.listener.searchError(Index.this, p.query, e);
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

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
  }   
}
