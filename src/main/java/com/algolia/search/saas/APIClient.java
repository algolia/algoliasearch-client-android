package com.algolia.search.saas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
 * Entry point in the Java API.
 * You should instantiate a Client object with your ApplicationID, ApiKey and Hosts 
 * to start using Algolia Search API
 */
public class APIClient {
	private int httpSocketTimeoutMS = 30000;
	private int httpConnectTimeoutMS = 3000;
    
    private final static String version;
    static {
        String tmp = "N/A";
        try {
            InputStream versionStream = APIClient.class.getResourceAsStream("/version.properties");
            if (versionStream != null) {
                BufferedReader versionReader = new BufferedReader(new InputStreamReader(versionStream));
                tmp = versionReader.readLine();
                versionReader.close();
            }
        } catch (IOException e) {
            // not fatal
        }
        version = tmp;
    }
    
    private final String applicationID;
    private final String apiKey;
    private final List<String> buildHostsArray;
    private final List<String> queryHostsArray;
    private final List<Long> buildHostsEnabled;
    private final List<Long> queryHostsEnabled;
    private final HttpClient httpClient;
    private String forwardRateLimitAPIKey;
    private String forwardEndUserIP;
    private String forwardAdminAPIKey;
    private HashMap<String, String> headers;
    
    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     */
    public APIClient(String applicationID, String apiKey) {
        this(applicationID, apiKey, Arrays.asList(applicationID + "-1.algolia.net", 
						        		applicationID + "-2.algolia.net", 
						        		applicationID + "-3.algolia.net"));
        Collections.shuffle(this.buildHostsArray);
        this.buildHostsArray.add(0, applicationID + ".algolia.net");
        this.buildHostsEnabled.add(0L);
        Collections.shuffle(this.queryHostsArray);
        this.queryHostsArray.add(0, applicationID + "-dsn.algolia.net");
        this.queryHostsEnabled.add(0L);
    }
    
    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param hostsArray the list of hosts that you have received for the service
     */
    public APIClient(String applicationID, String apiKey, List<String> hostsArray) {
    	this(applicationID, apiKey, hostsArray, hostsArray);
    }
    
    /**
     * Algolia Search initialization
     * @param applicationID the application ID you have in your admin interface
     * @param apiKey a valid API key for the service
     * @param buildHostsArray the list of hosts that you have received for the service
     * @param queryHostsArray the list of hosts that you have received for the service
     */
    public APIClient(String applicationID, String apiKey, List<String> buildHostsArray, List<String> queryHostArray) {
    	forwardRateLimitAPIKey = forwardAdminAPIKey = forwardEndUserIP = null;
        if (applicationID == null || applicationID.length() == 0) {
            throw new RuntimeException("AlgoliaSearch requires an applicationID.");
        }
        this.applicationID = applicationID;
        if (apiKey == null || apiKey.length() == 0) {
            throw new RuntimeException("AlgoliaSearch requires an apiKey.");
        }
        this.apiKey = apiKey;
        if (buildHostsArray == null || buildHostsArray.size() == 0 || queryHostArray == null || queryHostArray.size() == 0) {
            throw new RuntimeException("AlgoliaSearch requires a list of hostnames.");
        }
        
        this.buildHostsArray = new ArrayList<String>(buildHostsArray);
        this.queryHostsArray = new ArrayList<String>(queryHostArray);
        this.buildHostsEnabled = new ArrayList<Long>();
        for (int i =0; i < this.buildHostsArray.size(); ++i)
        	this.buildHostsEnabled.add(0L);
        this.queryHostsEnabled = new ArrayList<Long>();
        for (int i =0; i < this.queryHostsArray.size(); ++i)
        	this.queryHostsEnabled.add(0L);
        httpClient = HttpClientBuilder.create().disableAutomaticRetries().build();
        headers = new HashMap<String, String>();
    }
    
    /**
     * Allow to use IP rate limit when you have a proxy between end-user and Algolia.
     * This option will set the X-Forwarded-For HTTP header with the client IP and the X-Forwarded-API-Key with the API Key having rate limits.
     * @param adminAPIKey the admin API Key you can find in your dashboard
     * @param endUserIP the end user IP (you can use both IPV4 or IPV6 syntax)
     * @param rateLimitAPIKey the API key on which you have a rate limit
     */
    public void enableRateLimitForward(String adminAPIKey, String endUserIP, String rateLimitAPIKey)
    {
    	this.forwardAdminAPIKey = adminAPIKey;
    	this.forwardEndUserIP = endUserIP;
    	this.forwardRateLimitAPIKey = rateLimitAPIKey;
    }
    
    /**
     * Disable IP rate limit enabled with enableRateLimitForward() function
     */
    public void disableRateLimitForward() {
        forwardAdminAPIKey = forwardEndUserIP = forwardRateLimitAPIKey = null;
    }
    
    /**
     * Allow to set custom headers
     */
    public void setExtraHeader(String key, String value) {
    	headers.put(key, value);
    }
    
    /**
     * Allow to set the timeout
     * @param connectTimeout connection timeout in MS
     * @param readTimeout socket timeout in MS
     */
    public void setTimeout(int connectTimeout, int readTimeout) {
    	httpSocketTimeoutMS = readTimeout;
    	httpConnectTimeoutMS = connectTimeout;
    }
    
    /**
     * List all existing indexes
     * return an JSON Object in the form:
     * { "items": [ {"name": "contacts", "createdAt": "2013-01-18T15:33:13.556Z"},
     *              {"name": "notes", "createdAt": "2013-01-18T15:33:13.556Z"}]}
     */
    public JSONObject listIndexes() throws AlgoliaException {
        return getRequest("/1/indexes/", false);
    }

    /**
     * Delete an index
     *
     * @param indexName the name of index to delete
     * return an object containing a "deletedAt" attribute
     */
    public JSONObject deleteIndex(String indexName) throws AlgoliaException {
        try {
            return deleteRequest("/1/indexes/" + URLEncoder.encode(indexName, "UTF-8"), true);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // $COVERAGE-IGNORE$
        }
    }
  
    /**
     * Move an existing index.
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     */
    public JSONObject moveIndex(String srcIndexName, String dstIndexName) throws AlgoliaException {
    	try {
	        JSONObject content = new JSONObject();
	        content.put("operation", "move");
	        content.put("destination", dstIndexName);
	        return postRequest("/1/indexes/" + URLEncoder.encode(srcIndexName, "UTF-8") + "/operation", content.toString(), true); 	
    	} catch (UnsupportedEncodingException e) {
    		throw new RuntimeException(e); // $COVERAGE-IGNORE$
    	} catch (JSONException e) {
    		throw new AlgoliaException(e.getMessage()); // $COVERAGE-IGNORE$
    	}
    }
    
    /**
     * Copy an existing index.
     * @param srcIndexName the name of index to copy.
     * @param dstIndexName the new index name that will contains a copy of srcIndexName (destination will be overriten if it already exist).
     */
    public JSONObject copyIndex(String srcIndexName, String dstIndexName) throws AlgoliaException {
    	try {
	        JSONObject content = new JSONObject();
	        content.put("operation", "copy");
	        content.put("destination", dstIndexName);
	        return postRequest("/1/indexes/" + URLEncoder.encode(srcIndexName, "UTF-8") + "/operation", content.toString(), true); 	
    	} catch (UnsupportedEncodingException e) {
    		throw new RuntimeException(e); // $COVERAGE-IGNORE$
    	} catch (JSONException e) {
    		throw new AlgoliaException(e.getMessage()); // $COVERAGE-IGNORE$
    	}   	
    }
    
    public enum LogType
    {
      /// all query logs
      LOG_QUERY,
      /// all build logs
      LOG_BUILD,
      /// all error logs
      LOG_ERROR,
      /// all logs
      LOG_ALL
    }
    
    /**
     * Return 10 last log entries.
     */
    public JSONObject getLogs() throws AlgoliaException {
    	 return getRequest("/1/logs", false);
    }

    /**
     * Return last logs entries.
     * @param offset Specify the first entry to retrieve (0-based, 0 is the most recent log entry).
     * @param length Specify the maximum number of entries to retrieve starting at offset. Maximum allowed value: 1000.
     */
    public JSONObject getLogs(int offset, int length) throws AlgoliaException {
    	 return getRequest("/1/logs?offset=" + offset + "&length=" + length, false);
    }
    
    /**
     * Return last logs entries.
     * @param offset Specify the first entry to retrieve (0-based, 0 is the most recent log entry).
     * @param length Specify the maximum number of entries to retrieve starting at offset. Maximum allowed value: 1000.
     * @param onlyErrors Retrieve only logs with an httpCode different than 200 and 201
     */
    public JSONObject getLogs(int offset, int length, boolean onlyErrors) throws AlgoliaException {
    	 return getRequest("/1/logs?offset=" + offset + "&length=" + length + "&onlyErrors=" + onlyErrors, false);
    }
    
    /**
     * Return last logs entries.
     * @param offset Specify the first entry to retrieve (0-based, 0 is the most recent log entry).
     * @param length Specify the maximum number of entries to retrieve starting at offset. Maximum allowed value: 1000.
     * @param logType Specify the type of log to retrieve
     */
    public JSONObject getLogs(int offset, int length, LogType logType) throws AlgoliaException {
    	String type = null;
    	switch (logType) {
    	case LOG_BUILD:
    		type = "build";
    		break;
    	case LOG_QUERY:
    		type = "query";
    		break;
    	case LOG_ERROR:
    		type = "error";
    		break;
    	case LOG_ALL:
    		type = "all";
    		break;
    	}
    	 return getRequest("/1/logs?offset=" + offset + "&length=" + length + "&type=" + type, false);
    }
    
    /**
     * Get the index object initialized (no server call needed for initialization)
     *
     * @param indexName the name of index
     */
    public Index initIndex(String indexName) {
        return new Index(this, indexName);
    }

    /**
     * List all existing user keys with their associated ACLs
     */
    public JSONObject listUserKeys() throws AlgoliaException {
        return getRequest("/1/keys", false);
    }

    /**
     * Get ACL of a user key
     */
    public JSONObject getUserKeyACL(String key) throws AlgoliaException {
        return getRequest("/1/keys/" + key, false);
    }

    /**
     * Delete an existing user key
     */
    public JSONObject deleteUserKey(String key) throws AlgoliaException {
        return deleteRequest("/1/keys/" + key, true);
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
            throw new RuntimeException(e); // $COVERAGE-IGNORE$
        }
        return postRequest("/1/keys", jsonObject.toString(), true);
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
            throw new RuntimeException(e); // $COVERAGE-IGNORE$
        }
        return putRequest("/1/keys/" + key, jsonObject.toString(), true);
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
        return addUserKey(acls, validity, maxQueriesPerIPPerHour, maxHitsPerQuery, null);
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
        return updateUserKey(key, acls, validity, maxQueriesPerIPPerHour, maxHitsPerQuery, null);
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
     * @param indexes the list of targeted indexes 
     */
    public JSONObject addUserKey(List<String> acls, int validity, int maxQueriesPerIPPerHour, int maxHitsPerQuery, List<String> indexes) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
            jsonObject.put("validity", validity);
            jsonObject.put("maxQueriesPerIPPerHour", maxQueriesPerIPPerHour);
            jsonObject.put("maxHitsPerQuery", maxHitsPerQuery);
            if (indexes != null) {
                jsonObject.put("indexes", new JSONArray(indexes));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e); // $COVERAGE-IGNORE$
        }
        return postRequest("/1/keys", jsonObject.toString(), true);
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
     * @param indexes the list of targeted indexes 
     */
    public JSONObject updateUserKey(String key, List<String> acls, int validity, int maxQueriesPerIPPerHour, int maxHitsPerQuery, List<String> indexes) throws AlgoliaException {
        JSONArray array = new JSONArray(acls);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("acl", array);
            jsonObject.put("validity", validity);
            jsonObject.put("maxQueriesPerIPPerHour", maxQueriesPerIPPerHour);
            jsonObject.put("maxHitsPerQuery", maxHitsPerQuery);
            if (indexes != null) {
                jsonObject.put("indexes", new JSONArray(indexes));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e); // $COVERAGE-IGNORE$
        }
        return putRequest("/1/keys/" + key, jsonObject.toString(), true);
    }
    
    /**
     * Generate a secured and public API Key from a list of tagFilters and an
     * optional user token identifying the current user
     *
     * @param privateApiKey your private API Key
     * @param tagFilters the list of tags applied to the query (used as security)
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     */
    public String generateSecuredApiKey(String privateApiKey, String tagFilters) throws NoSuchAlgorithmException, InvalidKeyException {
        return generateSecuredApiKey(privateApiKey, tagFilters, null);
    }
    
    /**
     * Generate a secured and public API Key from a query and an
     * optional user token identifying the current user
     *
     * @param privateApiKey your private API Key
     * @param query contains the parameter applied to the query (used as security)
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     */
    public String generateSecuredApiKey(String privateApiKey, Query query) throws NoSuchAlgorithmException, InvalidKeyException {
        return generateSecuredApiKey(privateApiKey, query.toString(), null);
    }
    
    /**
     * Generate a secured and public API Key from a list of tagFilters and an
     * optional user token identifying the current user
     *
     * @param privateApiKey your private API Key
     * @param tagFilters the list of tags applied to the query (used as security)
     * @param userToken an optional token identifying the current user
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     */
    public String generateSecuredApiKey(String privateApiKey, String tagFilters, String userToken) throws NoSuchAlgorithmException, InvalidKeyException {
    	return hmac(privateApiKey, tagFilters + (userToken != null ? userToken : ""));
        
    }
    
    /**
     * Generate a secured and public API Key from a query and an
     * optional user token identifying the current user
     *
     * @param privateApiKey your private API Key
     * @param query contains the parameter applied to the query (used as security)
     * @param userToken an optional token identifying the current user
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     */
    public String generateSecuredApiKey(String privateApiKey, Query query, String userToken) throws NoSuchAlgorithmException, InvalidKeyException {
    	return hmac(privateApiKey, query.toString() + (userToken != null ? userToken : ""));
        
    }
    
    static String hmac(String key, String msg) {
    	Mac hmac;
		try {
			hmac = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		}
    	try {
			hmac.init(new SecretKeySpec(key.getBytes(), "HmacSHA256"));
		} catch (InvalidKeyException e) {
			throw new Error(e);
		}
    	byte[] rawHmac = hmac.doFinal(msg.getBytes());
        byte[] hexBytes = new Hex().encode(rawHmac);
        return new String(hexBytes);
    }
    
    private static enum Method {
        GET, POST, PUT, DELETE, OPTIONS, TRACE, HEAD;
    }
    
    protected JSONObject getRequest(String url, boolean build) throws AlgoliaException {
    	return _request(Method.GET, url, null, build);
    }
    
    protected JSONObject deleteRequest(String url, boolean build) throws AlgoliaException {
    	return _request(Method.DELETE, url, null, build);
    }
    
    protected JSONObject postRequest(String url, String obj, boolean build) throws AlgoliaException {
    	return _request(Method.POST, url, obj, build);
    }
    
    protected JSONObject putRequest(String url, String obj, boolean build) throws AlgoliaException {
    	return _request(Method.PUT, url, obj, build);
    }
    
    private JSONObject _requestByHost(HttpRequestBase req, String host, String url, String json, HashMap<String, String> errors) throws AlgoliaException {
    	req.reset();

        // set URL
        try {
			req.setURI(new URI("https://" + host + url));
		} catch (URISyntaxException e) {
			// never reached
			throw new IllegalStateException(e);
		}
    	
    	// set auth headers
    	req.setHeader("X-Algolia-Application-Id", this.applicationID);
    	if (forwardAdminAPIKey == null) {
    		req.setHeader("X-Algolia-API-Key", this.apiKey);
    	} else {
    		req.setHeader("X-Algolia-API-Key", this.forwardAdminAPIKey);
    		req.setHeader("X-Forwarded-For", this.forwardEndUserIP);
    		req.setHeader("X-Forwarded-API-Key", this.forwardRateLimitAPIKey);
    	}
    	for (Entry<String, String> entry : headers.entrySet()) {
    		req.setHeader(entry.getKey(), entry.getValue());
    	}
    	
    	// set user agent
    	req.setHeader("User-Agent", "Algolia for Java " + version);
    	
        // set JSON entity
        if (json != null) {
        	if (!(req instanceof HttpEntityEnclosingRequestBase)) {
        		throw new IllegalArgumentException("Method " + req.getMethod() + " cannot enclose entity");
        	}
            req.setHeader("Content-type", "application/json");
            try {
                StringEntity se = new StringEntity(json, "UTF-8"); 
                se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                ((HttpEntityEnclosingRequestBase) req).setEntity(se); 
            } catch (UnsupportedEncodingException e) {
                throw new AlgoliaException("Invalid JSON Object: " + json); // $COVERAGE-IGNORE$
            }
        }
        
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(httpSocketTimeoutMS)
                .setConnectTimeout(httpConnectTimeoutMS)
                .setConnectionRequestTimeout(httpConnectTimeoutMS)
                .build();
        req.setConfig(config);

        HttpResponse response;
        try {
        	response = httpClient.execute(req);
        } catch (IOException e) {
        	// on error continue on the next host
        	errors.put(host, String.format("%s=%s", e.getClass().getName(), e.getMessage()));
        	return null;
        }
        try {
            int code = response.getStatusLine().getStatusCode();
        	if (code / 100 == 4) {
        		String message = "";
        		try {
					message = EntityUtils.toString(response.getEntity());
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
        		if (code == 400) {
                    throw new AlgoliaException(code, message.length() > 0 ? message : "Bad request");
                } else if (code == 403) {
                    throw new AlgoliaException(code, message.length() > 0 ? message : "Invalid Application-ID or API-Key");
                } else if (code == 404) {
                    throw new AlgoliaException(code, message.length() > 0 ? message : "Resource does not exist");
                } else {
                    throw new AlgoliaException(code, message.length() > 0 ? message : "Error");
                }
        	}
            if (code / 100 != 2) {
            	try {
					errors.put(host, EntityUtils.toString(response.getEntity()));
				} catch (IOException e) {
					errors.put(host, String.valueOf(code));
				}
            	// KO, continue
                return null;
            }
            try {
                InputStream istream = response.getEntity().getContent();
                InputStreamReader is = new InputStreamReader(istream, "UTF-8");
                JSONTokener tokener = new JSONTokener(is);
                JSONObject res = new JSONObject(tokener);
                is.close();
                return res;
            } catch (IOException e) {
            	return null;
            } catch (JSONException e) {
                throw new AlgoliaException("JSON decode error:" + e.getMessage());
            }
        } finally {
            req.releaseConnection();
        }
    }
    
    private JSONObject _request(Method m, String url, String json, boolean build) throws AlgoliaException {
    	HttpRequestBase req;
    	switch (m) {
		case DELETE:
			req = new HttpDelete();
			break;
		case GET:
			req = new HttpGet();
			break;
		case POST:
			req = new HttpPost();
			break;
		case PUT:
			req = new HttpPut();
			break;
		default:
			throw new IllegalArgumentException("Method " + m + " is not supported");
    	}
    	HashMap<String, String> errors = new HashMap<String, String>();
    	List<String> hosts = null;
    	List<Long> enabled = null;
    	if (build) {
    		hosts = this.buildHostsArray;
    		enabled = this.buildHostsEnabled;
    	} else {
    		hosts = this.queryHostsArray;
    		enabled = this.queryHostsEnabled;
    	}
		
    	// for each host
    	for (int i = 0; i < hosts.size(); ++i) {
    		String host = hosts.get(i);
    		if (enabled.get(i) > System.currentTimeMillis())
    			continue;
    		JSONObject res = _requestByHost(req, host, url, json, errors);
    		if (res != null)
    			return res;
    		enabled.set(i, System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS));
    	}
    	enabled.set(enabled.size() - 1, 0L); // Keep the last host up;
    	StringBuilder builder = new StringBuilder("Hosts unreachable: ");
    	Boolean first = true;
    	for (Map.Entry<String, String> entry : errors.entrySet()) {
    		if (!first) {
    			builder.append(", ");
    		}
    		builder.append(entry.toString());
    		first = false;
    	}
        throw new AlgoliaException(builder.toString());
    }
    
    static public class IndexQuery {
    	private String index;
    	private Query query;
    	public IndexQuery(String index, Query q)  {
    		this.index = index;
    		this.query = q;
    	}
		public String getIndex() {
			return index;
		}
		public void setIndex(String index) {
			this.index = index;
		}
		public Query getQuery() {
			return query;
		}
		public void setQuery(Query query) {
			this.query = query;
		}
    }
    /**
     * This method allows to query multiple indexes with one API call
     */
    public JSONObject multipleQueries(List<IndexQuery> queries) throws AlgoliaException {
    		try {
    			JSONArray requests = new JSONArray();
    			for (IndexQuery indexQuery : queries) {
    				String paramsString = indexQuery.getQuery().getQueryString();
				requests.put(new JSONObject().put("indexName", indexQuery.getIndex()).put("params", paramsString));
    			}
				JSONObject body = new JSONObject().put("requests", requests);
				return postRequest("/1/indexes/*/queries", body.toString(), false);
			} catch (JSONException e) {
				new AlgoliaException(e.getMessage());
			}
    		return null;
    }
    
}
