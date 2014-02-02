package com.algolia.search.saas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SimpleTest {
    private static final String indexName = safe_name("test?java");

    private static APIClient client;
    private static Index index;

    public static String safe_name(String name) {
    	if (System.getenv("TRAVIS") != null) {
    		String id = System.getenv("TRAVIS_JOB_NUMBER");
    		return name + "_travis" + id;
    	}
    	return name;
    	
    }
    
    public static boolean isPresent(JSONArray array, String search, String attr) throws JSONException {
    	boolean isPresent = false;
    	for (int i = 0; i < array.length(); ++i) {
    		isPresent = isPresent || array.getJSONObject(i).getString(attr).equals(search);
    	}
    	return isPresent;
    }
    
    @BeforeClass
    public static void init() {
    	String applicationID = System.getenv("ALGOLIA_APPLICATION_ID");
        String apiKey = System.getenv("ALGOLIA_API_KEY");
        Assume.assumeFalse("You must set environement variables ALGOLIA_APPLICATION_ID and ALGOLIA_API_KEY to run the tests.", applicationID == null || apiKey == null);
        client = new APIClient(applicationID, apiKey);
        index = client.initIndex(indexName);
    }
    
    @Before
    public void eachInit() {
        try {
        	index.clearIndex();
        }
        catch (AlgoliaException e) {
        	//Normal
        }
    }

    @Test
    public void test01_deleteIndexIfExists() {
        try {
            client.deleteIndex(indexName);
        } catch (AlgoliaException e) {
            // not fatal
        }
    }

    @Test
    public void test02_pushObject() throws AlgoliaException, JSONException {
        JSONObject obj = index.addObject(new JSONObject().put("i", 42).put("s", "foo").put("b", true));
        index.waitTask(obj.getString("taskID"));
    }

    @Test
    public void test03_search() throws AlgoliaException, JSONException {
    	JSONObject obj = index.addObject(new JSONObject().put("i", 42).put("s", "foo").put("b", true));
        index.waitTask(obj.getString("taskID"));
        JSONObject res = index.search(new Query("foo"));
        assertEquals(1, res.getJSONArray("hits").length());
        assertEquals("foo", res.getJSONArray("hits").getJSONObject(0).getString("s"));
        assertEquals(42, res.getJSONArray("hits").getJSONObject(0).getLong("i"));
        assertEquals(true, res.getJSONArray("hits").getJSONObject(0).getBoolean("b"));
    }

    @Test
    public void test04_saveObject() throws AlgoliaException, JSONException {
    	JSONObject obj = index.addObject(new JSONObject().put("i", 42).put("s", "foo").put("b", true));
        index.waitTask(obj.getString("taskID"));
        JSONObject res = index.search(new Query("foo"));
        assertEquals(1, res.getJSONArray("hits").length());
        res = index.saveObject(new JSONObject().put("s", "bar"), res.getJSONArray("hits").getJSONObject(0).getString("objectID"));
        index.waitTask(res.getString("taskID"));
    }

    @Test
    public void test05_searchUpdated() throws AlgoliaException, JSONException {
    	JSONObject obj = index.addObject(new JSONObject().put("i", 42).put("s", "foo").put("b", true));
        index.waitTask(obj.getString("taskID"));
        JSONObject res = index.search(new Query("foo"));
        assertEquals(1, res.getJSONArray("hits").length());
        res = index.saveObject(new JSONObject().put("s", "bar"), res.getJSONArray("hits").getJSONObject(0).getString("objectID"));
        index.waitTask(res.getString("taskID"));
        res = index.search(new Query("foo"));
        assertEquals(0, res.getJSONArray("hits").length());

        res = index.search(new Query("bar"));
        assertEquals(1, res.getJSONArray("hits").length());
        assertEquals("bar", res.getJSONArray("hits").getJSONObject(0).getString("s"));
    }

    @Test
    public void test06_searchAll() throws AlgoliaException, JSONException {
    	JSONObject obj = index.addObject(new JSONObject().put("i", 42).put("s", "foo").put("b", true));
        index.waitTask(obj.getString("taskID"));
        JSONObject res = index.search(new Query("foo"));
        assertEquals(1, res.getJSONArray("hits").length());
        res = index.saveObject(new JSONObject().put("s", "bar"), res.getJSONArray("hits").getJSONObject(0).getString("objectID"));
        index.waitTask(res.getString("taskID"));
        res = index.search(new Query(""));
        assertEquals(1, res.getJSONArray("hits").length());
        res = index.search(new Query("*"));
        assertEquals(1, res.getJSONArray("hits").length());
    }
    
    @Test
    public void test07_addObject() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"));
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = index.search(new Query("jimie"));
    	assertEquals(1, res.getInt("nbHits"));
    }
    
    @Test
    public void test08_saveObject() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"), "a/go/?à");
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = index.search(new Query("jimie"));
    	assertEquals(1, res.getInt("nbHits"));
    }
    
    @Test
    public void test09_partialUpdateObject() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"), "a/go/?à");
    	index.waitTask(task.getString("taskID"));
    	task = index.partialUpdateObject(new JSONObject()
    	.put("firtname", "Roger"), "a/go/?à");
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = index.search(new Query("jimie"));
    	assertEquals(1, res.getInt("nbHits"));
    }
    
    @Test
    public void test10_getObject() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"), "a/go/?à");
    	index.waitTask(task.getString("taskID"));
    	JSONObject object = index.getObject("a/go/?à");
    	assertEquals("Jimmie", object.getString("firstname"));
    }
    
    @Test
    public void test11_getObjectWithAttr() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"), "a/go/?à");
    	index.waitTask(task.getString("taskID"));
    	JSONObject object = index.getObject("a/go/?à", Arrays.asList("lastname"));
    	assertEquals("Barninger", object.getString("lastname"));
    }
    
    @Test
    public void test12_deleteObject() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"));
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = index.search(new Query("jimie"));
    	task = index.deleteObject("a/go/?à");
    	assertEquals(1, res.getInt("nbHits"));
    }
    
    @Test
    public void test13_settings() throws AlgoliaException, JSONException {
    	JSONObject task = index.setSettings(new JSONObject()
    	.put("attributesToRetrieve", Arrays.asList("firstname")));
    	index.waitTask(new Long(task.getLong("taskID")).toString());
    	JSONObject settings = index.getSettings();
    	assertEquals("firstname", settings.getJSONArray("attributesToRetrieve").getString(0));
    }
    
    @Test
    public void test14_index() throws AlgoliaException, JSONException, InterruptedException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"), "a/go/?à");
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = client.listIndexes();
    	assertTrue(isPresent(res.getJSONArray("items"), indexName, "name"));
    	client.deleteIndex(indexName);
    	Thread.sleep(2000);
    	JSONObject resAfter = client.listIndexes();
    	assertFalse(isPresent(resAfter.getJSONArray("items"), indexName, "name"));
    }
    
    @Test
    public void test15_addObjects() throws JSONException, AlgoliaException {
    	List<JSONObject> array = new ArrayList<JSONObject>();
    	array.add(new JSONObject().put("firstname", "Jimmie").put("lastname", "Barninger"));
    	array.add(new JSONObject().put("firstname", "Warren").put("lastname", "Speach"));
    	JSONObject task = index.addObjects(array);
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = index.search(new Query(""));
    	assertEquals(2, res.getInt("nbHits"));
    }
    
    @Test
    public void test16_saveObjects() throws JSONException, AlgoliaException {
    	List<JSONObject> array = new ArrayList<JSONObject>();
    	array.add(new JSONObject().put("firstname", "Jimmie").put("lastname", "Barninger").put("objectID", "a/go/?à"));
    	array.add(new JSONObject().put("firstname", "Warren").put("lastname", "Speach").put("objectID", "a/go/ià"));
    	JSONObject task = index.saveObjects(array);
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = index.search(new Query(""));
    	assertEquals(2, res.getInt("nbHits"));
    }
    
    @Test
    public void test17_partialUpdateObjects() throws JSONException, AlgoliaException {
    	List<JSONObject> array = new ArrayList<JSONObject>();
    	array.add(new JSONObject().put("firstname", "Jimmie").put("lastname", "Barninger").put("objectID", "a/go/?à"));
    	array.add(new JSONObject().put("firstname", "Warren").put("lastname", "Speach").put("objectID", "a/go/ià"));
    	JSONObject task = index.saveObjects(array);
    	index.waitTask(task.getString("taskID"));
    	array = new ArrayList<JSONObject>();
    	array.add(new JSONObject().put("firstname", "Roger").put("objectID", "a/go/?à"));
    	array.add(new JSONObject().put("firstname", "Robert").put("objectID", "a/go/ià"));
    	task = index.partialUpdateObjects(array);
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = index.search(new Query("Ro"));
    	assertEquals(2, res.getInt("nbHits"));
    }
    
    @Test
    public void test18_user_key_index() throws AlgoliaException, JSONException {
    	JSONObject newKey = index.addUserKey(Arrays.asList("search"));
    	assertTrue(!newKey.getString("key").equals(""));
    	JSONObject res = index.listUserKeys();
    	assertTrue(isPresent(res.getJSONArray("keys"), newKey.getString("key"), "value"));
    	JSONObject getKey = index.getUserKeyACL(newKey.getString("key"));
    	assertEquals(newKey.getString("key"), getKey.getString("value"));
    	index.deleteUserKey(getKey.getString("value"));
    	JSONObject resAfter = index.listUserKeys();
    	assertTrue(!isPresent(resAfter.getJSONArray("keys"), newKey.getString("key"), "value"));
    }
    
    @Test
    public void test19_user_key() throws AlgoliaException, JSONException {
    	JSONObject newKey = client.addUserKey(Arrays.asList("search"));
    	assertTrue(!newKey.getString("key").equals(""));
    	JSONObject res = client.listUserKeys();
    	assertTrue(isPresent(res.getJSONArray("keys"), newKey.getString("key"), "value"));
    	JSONObject getKey = client.getUserKeyACL(newKey.getString("key"));
    	assertEquals(newKey.getString("key"), getKey.getString("value"));
    	client.deleteUserKey(getKey.getString("value"));
    	JSONObject resAfter = client.listUserKeys();
    	assertTrue(!isPresent(resAfter.getJSONArray("keys"), newKey.getString("key"), "value"));
    }
    
    @Test
    public void test20_moveIndex() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"));
    	index.waitTask(task.getString("taskID"));
    	task = client.moveIndex(indexName, indexName + "2");
    	Index newIndex = client.initIndex(indexName + "2");
    	newIndex.waitTask(task.getString("taskID"));
    	JSONObject res = newIndex.search(new Query("jimie"));
    	assertEquals(1, res.getInt("nbHits"));
    	try {
    		index.search(new Query("jimie"));
    		assertTrue(false);
    	} catch (AlgoliaException e) {
    		assertTrue(true);
    	}
    	client.deleteIndex(indexName + "2");
    }
    
    @Test
    public void test21_copyIndex() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"));
    	index.waitTask(task.getString("taskID"));
    	task = client.copyIndex(indexName, indexName + "2");
    	Index newIndex = client.initIndex(indexName + "2");
    	newIndex.waitTask(task.getString("taskID"));
    	JSONObject res = newIndex.search(new Query("jimie"));
    	assertEquals(1, res.getInt("nbHits"));
    	res = index.search(new Query("jimie"));
    	assertEquals(1, res.getInt("nbHits"));
    	client.deleteIndex(indexName + "2");
    }
    
    @Test
    public void test22_browse() throws AlgoliaException, JSONException {
    	JSONObject task = index.addObject(new JSONObject()
        .put("firstname", "Jimmie")
        .put("lastname", "Barninger")
        .put("followers", 93)
        .put("company", "California Paint"));
    	index.waitTask(task.getString("taskID"));
    	JSONObject res = index.browse(0);
    	assertEquals(1, res.getInt("nbHits"));
    }
    
    @Test
    public void test23_logs() throws AlgoliaException, JSONException {
    	JSONObject res = client.getLogs();
    	assertTrue(res.getJSONArray("logs").length() > 0);
    }

}
