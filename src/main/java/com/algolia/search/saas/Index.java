package com.algolia.search.saas;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.algolia.search.saas.APIClient.IndexQuery;

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
 * Contains all the functions related to one index
 * You should use APIClient.initIndex(indexName) to retrieve this object
 */
public class Index {
    private APIClient client;
    private String encodedIndexName;
    private String indexName;

    /**
     * Index initialization (You should not call this initialized yourself)
     */
    protected Index(APIClient client, String indexName) {
        try {
            this.client = client;
            this.encodedIndexName = URLEncoder.encode(indexName, "UTF-8");
            this.indexName = indexName;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getIndexName() {
        return indexName;
    }

    /**
     * Add an object in this index
     * 
     * @param obj the object to add. 
    */
    public JSONObject addObject(JSONObject obj) throws AlgoliaException {
        return client.postRequest("/1/indexes/" + encodedIndexName, obj.toString(), false);
    }

    /**
     * Add an object in this index
     * 
     * @param obj the object to add. 
     * @param objectID an objectID you want to attribute to this object 
     * (if the attribute already exist the old object will be overwrite)
     */
    public JSONObject addObject(JSONObject obj, String objectID) throws AlgoliaException {
        try {
            return client.putRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), obj.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add an object in this index asynchronously
     * 
     * @param obj the object to add. 
     *  The object is represented by an associative array
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void addObjectASync(JSONObject obj, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.AddObject, null, obj);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Add an object in this index asynchronously
     * 
     * @param obj the object to add. 
     *  The object is represented by an associative array
     * @param objectID an objectID you want to attribute to this object 
     * (if the attribute already exist the old object will be overwrite)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void addObjectASync(JSONObject obj, String objectID, IndexListener listener)  {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.AddObject, objectID, obj);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Custom batch
     * 
     * @param actions the array of actions
     * @throws AlgoliaException 
     */
    public JSONObject batch(JSONArray actions) throws AlgoliaException {
	    try {
	    	JSONObject content = new JSONObject();
	    	content.put("requests", actions);
	    	return client.postRequest("/1/indexes/" + encodedIndexName + "/batch", content.toString(), false);
	    } catch (JSONException e) {
	        throw new AlgoliaException(e.getMessage());
	    }
    }

    /**
     * Custom batch
     * 
     * @param actions the array of actions
     * @throws AlgoliaException 
     */
    public JSONObject batch(List<JSONObject> actions) throws AlgoliaException {
	    try {
	    	JSONObject content = new JSONObject();
	    	content.put("requests", actions);
	    	return client.postRequest("/1/indexes/" + encodedIndexName + "/batch", content.toString(), false);
	    } catch (JSONException e) {
	        throw new AlgoliaException(e.getMessage());
	    }
    }

    /**
     * Add several objects
     * 
     * @param objects contains an array of objects to add.
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
            return batch(array);
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
     * Add several objects
     * 
     * @param objects contains an array of objects to add.
     */
    public JSONObject addObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for(int n = 0; n < inputArray.length(); n++)
            {
                JSONObject action = new JSONObject();
                action.put("action", "addObject");
                action.put("body", inputArray.getJSONObject(n));
                array.put(action);
            }
            return batch(array);
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
    public void addObjectsASync(JSONArray objects, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.AddObjects2, objects);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Get an object from this index
     * 
     * @param objectID the unique identifier of the object to retrieve
     */
    public JSONObject getObject(String objectID) throws AlgoliaException {
        try {
            return client.getRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get an object from this index
     * 
     * @param objectID the unique identifier of the object to retrieve
     * @param attributesToRetrieve, contains the list of attributes to retrieve.
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
            return client.getRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8") + params.toString(), false);
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
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.GetObject, objectID, (List)null);
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
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.GetObject, objectID, attributesToRetrieve);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Get several objects from this index
     *
     * @param objectIDs the array of unique identifier of objects to retrieve
     * @throws AlgoliaException
     */
    public JSONObject getObjects(List<String> objectIDs) throws AlgoliaException {
        try {
	    JSONArray requests = new JSONArray();
	    for (String id : objectIDs) {
		JSONObject request = new JSONObject();
		request.put("indexName", this.indexName);
		request.put("objectID", id);
		requests.put(request);
	    }
	    JSONObject body = new JSONObject();
	    body.put("requests", requests);
	    return client.postRequest("/1/indexes/*/objects", body.toString(), true);
        } catch (JSONException e){
	    throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Get several objects from this index asynchronously
     *
     * @param objectIDs the array of unique identifier of objects to retrieve
     * @throws AlgoliaException
     */
    public void getObjectsASync(List<String> objectIDs, IndexListener listener) throws AlgoliaException {
	ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.GetObjects, objectIDs);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Update partially an object (only update attributes passed in argument)
     * 
     * @param partialObject the object attributes to override
     */
    public JSONObject partialUpdateObject(JSONObject partialObject, String objectID) throws AlgoliaException {
        try {
            return client.postRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8") + "/partial", partialObject.toString(), false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update partially an object asynchronously (only update attributes passed in argument)
     * 
     * @param partialObject the object attributes to override, the 
     *  object must contains an objectID attribute
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void partialUpdateObjectASync(JSONObject partialObject, String objectID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.PartialSaveObject, objectID, partialObject);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Partially Override the content of several objects
     * 
     * @param objects the array of objects to update (each object must contains an objectID attribute)
     */
    public JSONObject partialUpdateObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for(int n = 0; n < inputArray.length(); n++)
            {
            	JSONObject obj = inputArray.getJSONObject(n);
                JSONObject action = new JSONObject();
                action.put("action", "partialUpdateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body", obj);
                array.put(action);
            }
            return batch(array);
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
    public void partialUpdateObjectsASync(JSONArray objects, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.PartialSaveObjects2, objects);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Partially Override the content of several objects
     * 
     * @param objects the array of objects to update (each object must contains an objectID attribute)
     */
    public JSONObject partialUpdateObjects(List<JSONObject> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (JSONObject obj : objects) {
                JSONObject action = new JSONObject();
                action.put("action", "partialUpdateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body",obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Partially Override the content of several objects asynchronously
     * 
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void partialUpdateObjectsASync(List<JSONObject> objects, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.PartialSaveObjects, objects);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Override the content of object
     * 
     * @param object the object to save
     */
    public JSONObject saveObject(JSONObject object, String objectID) throws AlgoliaException {
        try {
            return client.putRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), object.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override the content of object asynchronously
     * 
     * @param object the object to save
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void saveObjectASync(JSONObject object, String objectID, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.SaveObject, objectID, object);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Override the content of several objects
     * 
     * @param objects an array of objects to update (each object must contains an objectID attribute)
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
            return batch(array);
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
     * Override the content of several objects
     * 
     * @param objects contains an array of objects to update (each object must contains an objectID attribute)
     */
    public JSONObject saveObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for(int n = 0; n < inputArray.length(); n++)
            {
            	JSONObject obj = inputArray.getJSONObject(n);
                JSONObject action = new JSONObject();
                action.put("action", "updateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body", obj);
                array.put(action);
            }
            return batch(array);
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
    public void saveObjectsASync(JSONArray objects, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.SaveObjects2, objects);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Delete an object from the index 
     * 
     * @param objectID the unique identifier of object to delete
     */
    public JSONObject deleteObject(String objectID) throws AlgoliaException {
        if (objectID.length() == 0 || objectID == null)
            throw new AlgoliaException("Invalid objectID");
        try {
            return client.deleteRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"));
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
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.DeleteObject, objectID, (List)null);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Delete several objects
     * 
     * @param objects the array of objectIDs to delete
     */
    public JSONObject deleteObjects(List<String> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (String id : objects) {
                JSONObject obj = new JSONObject();
                obj.put("objectID", id);
                JSONObject action = new JSONObject();
                action.put("action", "deleteObject");
                action.put("body",obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete several objects
     * 
     * @param objects the array of objectIDs to delete
     */
    public JSONObject deleteObjects2(List<JSONObject> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (JSONObject obj : objects) {
                JSONObject action = new JSONObject();
                action.put("action", "deleteObject");
                action.put("body", obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete several objects asynchronously
     * 
     * @param objects the array of objectIDs to delete
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
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
     * Delete all objects matching a query
     * 
     * @param query the query string
     * @throws AlgoliaException 
     */
    public void deleteByQuery(Query query) throws AlgoliaException {
        List<String> attributesToRetrieve = new ArrayList<String>();
        attributesToRetrieve.add("objectID");
        query.setAttributesToRetrieve(attributesToRetrieve);
        query.setHitsPerPage(100);

        JSONObject results = this.search(query);
        try {
            while (results.getInt("nbHits") != 0) {
                List<String> objectIDs = new ArrayList<String>();
                for (int i = 0; i < results.getJSONArray("hits").length(); ++i) {
                    JSONObject hit = results.getJSONArray("hits").getJSONObject(i);
                    objectIDs.add(hit.getString("objectID"));
                }
                JSONObject task = this.deleteObjects(objectIDs);
                this.waitTask(task.getString("taskID"));
                results = this.search(query);
            }
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete all objects matching a query asynchronously
     * 
     * @param query the query string
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void deleteByQueryASync(Query query, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.DeleteByQuery, query);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Search inside the index
     */
    public JSONObject search(Query query) throws AlgoliaException {
        String paramsString = query.getQueryString();
        if (paramsString.length() > 0)
            return client.getRequest("/1/indexes/" + encodedIndexName + "?" + paramsString, true);
        else
            return client.getRequest("/1/indexes/" + encodedIndexName, true);
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
     */
    public void waitTask(String taskID) throws AlgoliaException {
        try {
            while (true) {
                JSONObject obj = client.getRequest("/1/indexes/" + encodedIndexName + "/task/" + URLEncoder.encode(taskID, "UTF-8"), false);
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
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.WaitTask, taskID, (List)null);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Get settings of this index
     */
    public JSONObject getSettings() throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/settings", false);
    }

    /**
     * Get settings of this index asynchronously
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void getSettingsASync(IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.GetSettings, null, (List)null);
        new ASyncIndexTask().execute(params);
    }

    /**
     * Set settings for this index
     * 
     * @param settigns the settings object that can contains :
     * - minWordSizefor1Typo: (integer) the minimum number of characters to accept one typo (default = 3).
     * - minWordSizefor2Typos: (integer) the minimum number of characters to accept two typos (default = 7).
     * - hitsPerPage: (integer) the number of hits per page (default = 10).
     * - attributesToRetrieve: (array of strings) default list of attributes to retrieve in objects. 
     *   If set to null, all attributes are retrieved.
     * - attributesToHighlight: (array of strings) default list of attributes to highlight. 
     *   If set to null, all indexed attributes are highlighted.
     * - attributesToSnippet**: (array of strings) default list of attributes to snippet alongside the number of words to return (syntax is attributeName:nbWords).
     *   By default no snippet is computed. If set to null, no snippet is computed.
     * - attributesToIndex: (array of strings) the list of fields you want to index.
     *   If set to null, all textual and numerical attributes of your objects are indexed, but you should update it to get optimal results.
     *   This parameter has two important uses:
     *     - Limit the attributes to index: For example if you store a binary image in base64, you want to store it and be able to 
     *       retrieve it but you don't want to search in the base64 string.
     *     - Control part of the ranking*: (see the ranking parameter for full explanation) Matches in attributes at the beginning of 
     *       the list will be considered more important than matches in attributes further down the list. 
     *       In one attribute, matching text at the beginning of the attribute will be considered more important than text after, you can disable 
     *       this behavior if you add your attribute inside `unordered(AttributeName)`, for example attributesToIndex: ["title", "unordered(text)"].
     * - attributesForFaceting: (array of strings) The list of fields you want to use for faceting. 
     *   All strings in the attribute selected for faceting are extracted and added as a facet. If set to null, no attribute is used for faceting.
     * - ranking: (array of strings) controls the way results are sorted.
     *   We have six available criteria: 
     *    - typo: sort according to number of typos,
     *    - geo: sort according to decreassing distance when performing a geo-location based search,
     *    - proximity: sort according to the proximity of query words in hits,
     *    - attribute: sort according to the order of attributes defined by attributesToIndex,
     *    - exact: sort according to the number of words that are matched identical to query word (and not as a prefix),
     *    - custom: sort according to a user defined formula set in **customRanking** attribute.
     *   The standard order is ["typo", "geo", "proximity", "attribute", "exact", "custom"]
     * - customRanking: (array of strings) lets you specify part of the ranking.
     *   The syntax of this condition is an array of strings containing attributes prefixed by asc (ascending order) or desc (descending order) operator.
     *   For example `"customRanking" => ["desc(population)", "asc(name)"]`  
     * - queryType: Select how the query words are interpreted, it can be one of the following value:
     *   - prefixAll: all query words are interpreted as prefixes,
     *   - prefixLast: only the last word is interpreted as a prefix (default behavior),
     *   - prefixNone: no query word is interpreted as a prefix. This option is not recommended.
     * - highlightPreTag: (string) Specify the string that is inserted before the highlighted parts in the query result (default to "<em>").
     * - highlightPostTag: (string) Specify the string that is inserted after the highlighted parts in the query result (default to "</em>").
     * - optionalWords: (array of strings) Specify a list of words that should be considered as optional when found in the query.
     */

    public JSONObject setSettings(JSONObject settings) throws AlgoliaException {
        return client.putRequest("/1/indexes/" + encodedIndexName + "/settings", settings.toString());
    }

    /**
     * Delete the index content without removing settings and index specific API keys.
     */
    public JSONObject clearIndex() throws AlgoliaException {
        return client.postRequest("/1/indexes/" + encodedIndexName + "/clear", "", false);
    }

    /**
     * Set settings for this index asynchronously
     * @param listener the listener that will receive the result or error. If the listener is an instance of Activity, the result will be received directly on UIthread
     */
    public void setSettingsASync(JSONObject settings, IndexListener listener) {
        ASyncIndexTaskParams params = new ASyncIndexTaskParams(listener, ASyncIndexTaskKind.SetSettings, null, settings);
        new ASyncIndexTask().execute(params);
    }

    /**
     * List all existing user keys with their associated ACLs
     */
    public JSONObject listUserKeys() throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/keys", false);
    }

    /**
     * Get ACL of a user key
     */
    public JSONObject getUserKeyACL(String key) throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/keys/" + key, false);
    }

    /**
     * Delete an existing user key
     */
    public JSONObject deleteUserKey(String key) throws AlgoliaException {
        return client.deleteRequest("/1/indexes/" + encodedIndexName + "/keys/" + key);
    }

    /**
     * Create a new user key
     *
     * @param acls the list of ACL for this key. Defined by an array of strings that 
     * can contains the following values:
     *   - search: allow to search (https and http)
     *   - addObject: allows to add/update an object in the index (https only)
     *   - deleteObject : allows to delete an existing object (https only)
     *   - deleteIndex : allows to delete index content (https only)
     *   - settings : allows to get index settings (https only)
     *   - editSettings : allows to change index settings (https only)
     */
    public JSONObject addUserKey(List<String> acls) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return client.postRequest("/1/indexes/" + encodedIndexName + "/keys", jsonObject.toString(), false);
    }

     /**
     * Update a user key
     *
     * @param acls the list of ACL for this key. Defined by an array of strings that 
     * can contains the following values:
     *   - search: allow to search (https and http)
     *   - addObject: allows to add/update an object in the index (https only)
     *   - deleteObject : allows to delete an existing object (https only)
     *   - deleteIndex : allows to delete index content (https only)
     *   - settings : allows to get index settings (https only)
     *   - editSettings : allows to change index settings (https only)
     */
    public JSONObject updateUserKey(String key, List<String> acls) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return client.putRequest("/1/indexes/" + encodedIndexName + "/keys/" + key, jsonObject.toString());
    }

    /**
     * Create a new user key
     *
     * @param acls the list of ACL for this key. Defined by an array of strings that 
     * can contains the following values:
     *   - search: allow to search (https and http)
     *   - addObject: allows to add/update an object in the index (https only)
     *   - deleteObject : allows to delete an existing object (https only)
     *   - deleteIndex : allows to delete index content (https only)
     *   - settings : allows to get index settings (https only)
     *   - editSettings : allows to change index settings (https only)
     * @param validity the number of seconds after which the key will be automatically removed (0 means no time limit for this key)
     * @param maxQueriesPerIPPerHour Specify the maximum number of API calls allowed from an IP address per hour.  Defaults to 0 (no rate limit).
     * @param maxHitsPerQuery Specify the maximum number of hits this API key can retrieve in one call. Defaults to 0 (unlimited) 
    */
    public JSONObject addUserKey(List<String> acls, int validity, int maxQueriesPerIPPerHour, int maxHitsPerQuery) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
            jsonObject.put("validity", validity);
            jsonObject.put("maxQueriesPerIPPerHour", maxQueriesPerIPPerHour);
            jsonObject.put("maxHitsPerQuery", maxHitsPerQuery);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return client.postRequest("/1/indexes/" + encodedIndexName + "/keys", jsonObject.toString(), false);
    }

    /**
     * Update a user key
     *
     * @param acls the list of ACL for this key. Defined by an array of strings that 
     * can contains the following values:
     *   - search: allow to search (https and http)
     *   - addObject: allows to add/update an object in the index (https only)
     *   - deleteObject : allows to delete an existing object (https only)
     *   - deleteIndex : allows to delete index content (https only)
     *   - settings : allows to get index settings (https only)
     *   - editSettings : allows to change index settings (https only)
     * @param validity the number of seconds after which the key will be automatically removed (0 means no time limit for this key)
     * @param maxQueriesPerIPPerHour Specify the maximum number of API calls allowed from an IP address per hour.  Defaults to 0 (no rate limit).
     * @param maxHitsPerQuery Specify the maximum number of hits this API key can retrieve in one call. Defaults to 0 (unlimited) 
     */
    public JSONObject updateUserKey(String key, List<String> acls, int validity, int maxQueriesPerIPPerHour, int maxHitsPerQuery) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
            jsonObject.put("validity", validity);
            jsonObject.put("maxQueriesPerIPPerHour", maxQueriesPerIPPerHour);
            jsonObject.put("maxHitsPerQuery", maxHitsPerQuery);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return client.putRequest("/1/indexes/" + encodedIndexName + "/keys/" + key, jsonObject.toString());
    }

    /**
     * Browse all index content
     *
     * @param page Pagination parameter used to select the page to retrieve.
     *             Page is zero-based and defaults to 0. Thus, to retrieve the 10th page you need to set page=9
     */
    public JSONObject browse(int page) throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/browse?page=" + page, false);
    }

    /**
     * Browse all index content
     *
     * @param page Pagination parameter used to select the page to retrieve.
     *             Page is zero-based and defaults to 0. Thus, to retrieve the 10th page you need to set page=9
     * @param hitsPerPage: Pagination parameter used to select the number of hits per page. Defaults to 1000.
     */
    public JSONObject browse(int page, int hitsPerPage) throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/browse?page=" + page + "&hitsPerPage=" + hitsPerPage, false);
    }

    private enum ASyncIndexTaskKind
    {
        GetObject,
        AddObject,
        AddObjects,
        AddObjects2,
        SaveObject,
        SaveObjects,
        SaveObjects2,
        PartialSaveObject,
        PartialSaveObjects,
        PartialSaveObjects2,
        DeleteObject,
        DeleteObjects,
        DeleteByQuery,
        WaitTask,
        Query,
        GetSettings,
        SetSettings,
        GetObjects
    };

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

        public ASyncIndexTaskParams(IndexListener listener, Query query) {
            this.listener = listener;
            this.query = query;
            this.kind = ASyncIndexTaskKind.Query;
        }

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
            case AddObject:
                p.listener.addObjectResult(Index.this, p.objectContent, res);
                break;
            case AddObjects:
                p.listener.addObjectsResult(Index.this, p.list, res);
                break;
            case AddObjects2:
            	p.listener.addObjectsResult(Index.this, p.objects2, res);
            	break;
            case WaitTask:
                p.listener.waitTaskResult(Index.this, p.objectID);
                break;
            case SaveObject:
                p.listener.saveObjectResult(Index.this, p.objectContent, p.objectID, res);
                break;
            case SaveObjects:
                p.listener.saveObjectsResult(Index.this, p.list, res);
                break;
            case SaveObjects2:
            	p.listener.saveObjectsResult(Index.this, p.objects2, res);
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
            case GetObject:
                p.listener.getObjectResult(Index.this, p.objectID, res);
                break;
            case GetObjects:
		p.listener.getObjectsResult(Index.this, p.list, res);
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
                    res = addObjects(p.list);
                } catch (AlgoliaException e) {
                    p.listener.addObjectsError(Index.this, p.list, e);
                    return null;
                }
                break;
            case AddObjects2:
            	try {
            		res = addObjects(p.objects2);
            	} catch (AlgoliaException e) {
            		p.listener.addObjectsError(Index.this, p.objects2, e);
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
                    res = saveObjects(p.list);
                } catch (AlgoliaException e) {
                    p.listener.saveObjectsError(Index.this, p.list, e);
                    return null;
                }
                break;
            case SaveObjects2:
                try {
                    res = saveObjects(p.objects2);
                } catch (AlgoliaException e) {
                    p.listener.saveObjectsError(Index.this, p.objects2, e);
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
	    case GetObjects:
		try {
		    res = getObjects(p.list);
		} catch (AlgoliaException e) {
		    p.listener.getObjectsError(Index.this, p.list, e);
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

    /**
     * Perform a search with disjunctive facets generating as many queries as number of disjunctive facets
     * @param query the query
     * @param disjunctiveFacets the array of disjunctive facets
     * @param refinements Map<String, List<String>> representing the current refinements
     *     ex: { "my_facet1" => ["my_value1", "my_value2"], "my_disjunctive_facet1" => ["my_value1", "my_value2"] }
     * @throws AlgoliaException 
     */
    public JSONObject disjunctiveFaceting(Query query, List<String> disjunctiveFacets, Map<String, List<String>> refinements) throws AlgoliaException {
    	if (refinements == null) {
    		refinements = new HashMap<String, List<String>>();
    	}
    	HashMap<String, List<String>> disjunctiveRefinements = new HashMap<String, List<String>>();
    	for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
    		if (disjunctiveFacets.contains(elt.getKey())) {
    			disjunctiveRefinements.put(elt.getKey(), elt.getValue());
    		}
    	}
    	
    	// build queries
    	List<IndexQuery> queries = new ArrayList<IndexQuery>();
    	// hits + regular facets query
    	StringBuilder filters = new StringBuilder();
    	boolean first_global = true;
    	for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
    		StringBuilder or = new StringBuilder();
    		or.append("(");
    		boolean first = true;
    		for (String val : elt.getValue()) {
        		if (disjunctiveRefinements.containsKey(elt.getKey())) {
        			// disjunctive refinements are ORed
        			if (!first) {
        				or.append(',');
        			}
        			first = false;
        			or.append(String.format("%s:%s", elt.getKey(), val));
        		} else {
        			if (!first_global) {
        				filters.append(',');
        			}
        			first_global = false;
        			filters.append(String.format("%s:%s", elt.getKey(), val));
        		}
    		}
    		// Add or
    		if (disjunctiveRefinements.containsKey(elt.getKey())) {
    			or.append(')');
    			if (!first_global) {
    				filters.append(',');
    			}
    			first_global = false;
    			filters.append(or.toString());
    		}
    	}
    	
    	queries.add(new IndexQuery(this.indexName, new Query(query).setFacetFilters(filters.toString())));
    	// one query per disjunctive facet (use all refinements but the current one + hitsPerPage=1 + single facet
    	for (String disjunctiveFacet : disjunctiveFacets) {
    		filters = new StringBuilder();
    		first_global = true;
    		for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
    			if (disjunctiveFacet.equals(elt.getKey())) {
    				continue;
    			}
    			StringBuilder or = new StringBuilder();
        		or.append("(");
        		boolean first = true;
        		for (String val : elt.getValue()) {
            		if (disjunctiveRefinements.containsKey(elt.getKey())) {
            			// disjunctive refinements are ORed
            			if (!first) {
            				or.append(',');
            			}
            			first = false;
            			or.append(String.format("%s:%s", elt.getKey(), val));
            		} else {
            			if (!first_global) {
            				filters.append(',');
            			}
            			first_global = false;
            			filters.append(String.format("%s:%s", elt.getKey(), val));
            		}
        		}
        		// Add or
        		if (disjunctiveRefinements.containsKey(elt.getKey())) {
        			or.append(')');
        			if (!first_global) {
        				filters.append(',');
        			}
        			first_global = false;
        			filters.append(or.toString());
        		}
    		}
    		List<String> facets = new ArrayList<String>();
    		facets.add(disjunctiveFacet);
    		queries.add(new IndexQuery(this.indexName, new Query(query).setHitsPerPage(1).setAttributesToRetrieve(new ArrayList<String>()).setAttributesToHighlight(new ArrayList<String>()).setAttributesToSnippet(new ArrayList<String>()).setFacets(facets).setFacetFilters(filters.toString())));
    	}
    	JSONObject answers = this.client.multipleQueries(queries);
    	
    	// aggregate answers
    	// first answer stores the hits + regular facets
    	try {
    		JSONArray results = answers.getJSONArray("results");
			JSONObject aggregatedAnswer = results.getJSONObject(0);
			JSONObject disjunctiveFacetsJSON = new JSONObject();
			for (int i = 1; i < results.length(); ++i) {
				JSONObject facets = results.getJSONObject(i).getJSONObject("facets");
				Iterator<String> keys = facets.keys();
				while(keys.hasNext()) {
					String key = keys.next();
					// Add the facet to the disjunctive facet hash
					disjunctiveFacetsJSON.put(key, facets.getJSONObject(key));
					// concatenate missing refinements
					if (!disjunctiveRefinements.containsKey(key)) {
						continue;
					}
					for (String refine : disjunctiveRefinements.get(key)) {
						if (!disjunctiveFacetsJSON.getJSONObject(key).has(refine)) {
							disjunctiveFacetsJSON.getJSONObject(key).put(refine, 0);
						}
					}
				}
			}
			aggregatedAnswer.put("disjunctiveFacets", disjunctiveFacetsJSON);
			return aggregatedAnswer;
		} catch (JSONException e) {
			throw new Error(e);
		}
    }
    public JSONObject disjunctiveFaceting(Query query, List<String> disjunctiveFacets) throws AlgoliaException {
    	return disjunctiveFaceting(query, disjunctiveFacets, null);
    }

}
