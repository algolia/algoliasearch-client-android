package com.algolia.search.saas;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


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
 * Asynchronously receive result of Index asynchronous methods
 */
public interface IndexListener {

    /**
     * Asynchronously receive result of Index.saveObjectASync methods.
     */
    public void addObjectResult(Index index, JSONObject object, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.saveObjectASync methods.
     */
    public void addObjectError(Index index, JSONObject object, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.saveObjectsASync method.
     */
    public void addObjectsResult(Index index, List<JSONObject> objects, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.saveObjectsASync method.
     */
    public void addObjectsError(Index index, List<JSONObject> objects, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.saveObjectsASync method.
     */
    public void addObjectsResult(Index index, JSONArray objects, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.saveObjectsASync method.
     */
    public void addObjectsError(Index index, JSONArray objects, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.searchASync method.
     */
    public void searchResult(Index index, Query query, JSONObject results);
    
    /**
     * Asynchronously receive error of Index.searchASync method.
     */
    public void searchError(Index index, Query query, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.deleteObjectASync method.
     */
    public void deleteObjectResult(Index index, String objectID, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.deleteObjectASync method.
     */
    public void deleteObjectError(Index index, String objectID, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.deleteObjectsASync method.
     */
    public void deleteObjectsResult(Index index, JSONArray objects, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.deleteByQueryASync method.
     */
    public void deleteByQueryError(Index index, Query query, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.deleteByQueryASync method.
     */
    public void deleteByQueryResult(Index index);
    
    /**
     * Asynchronously receive error of Index.deleteObjectsASync method.
     */
    public void deleteObjectsError(Index index, List<JSONObject> objects, AlgoliaException e);

    /**
     * Asynchronously receive result of Index.saveObjectASync methods.
     */
    public void saveObjectResult(Index index, JSONObject object, String objectID, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.saveObjectASync methods.
     */
    public void saveObjectError(Index index, JSONObject object, String objectID, AlgoliaException e);
  
    
    /**
     * Asynchronously receive result of Index.saveObjectsASync method.
     */
    public void saveObjectsResult(Index index, List<JSONObject> objects, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.saveObjectsASync method.
     */
    public void saveObjectsError(Index index, List<JSONObject> objects, AlgoliaException e);
   
    /**
     * Asynchronously receive result of Index.saveObjectsASync method.
     */
    public void saveObjectsResult(Index index, JSONArray objects, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.saveObjectsASync method.
     */
    public void saveObjectsError(Index index, JSONArray objects, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.partialUpdateObjectASync method.
     */
    public void partialUpdateResult(Index index, JSONObject object, String objectID, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.partialUpdateObjectASync method.
     */
    public void partialUpdateError(Index index, JSONObject object, String objectID, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.partialUpdateObjectsASync method.
     */
    public void partialUpdateObjectsResult(Index index, List<JSONObject> objects, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.partialUpdateObjectsASync method.
     */
    public void partialUpdateObjectsError(Index index, List<JSONObject> objects, AlgoliaException e);
   
    /**
     * Asynchronously receive result of Index.partialUpdateObjectsASync method.
     */
    public void partialUpdateObjectsResult(Index index, JSONArray objects, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.partialUpdateObjectsASync method.
     */
    public void partialUpdateObjectsError(Index index, JSONArray objects, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.getObjectASync method.
     */
    public void getObjectResult(Index index, String objectID, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.getObjectASync method.
     */
    public void getObjectError(Index index, String objectID, AlgoliaException e);

    /**
     * Asynchronously receive result of Index.getObjectsASync method.
     */
    public void getObjectsResult(Index index, List<String> objectIDs, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.getObjectsASync method.
     */
    public void getObjectsError(Index index, List<String> objectIDs, AlgoliaException e);

    /**
     * Asynchronously receive result of Index.waitTaskASync method.
     */
    public void waitTaskResult(Index index, String taskID);
    
    /**
     * Asynchronously receive error of Index.waitTaskASync method.
     */
    public void waitTaskError(Index index, String taskID, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.getSettingsASync method.
     */
    public void getSettingsResult(Index index, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.getSettingsASync method.
     */
    public void getSettingsError(Index index, AlgoliaException e);
    
    /**
     * Asynchronously receive result of Index.setSettingsASync method.
     */
    public void setSettingsResult(Index index, JSONObject settings, JSONObject result);
    
    /**
     * Asynchronously receive error of Index.setSettingsASync method.
     */
    public void setSettingsError(Index index, JSONObject settings, AlgoliaException e);
}
