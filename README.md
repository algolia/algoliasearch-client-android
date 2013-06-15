Algolia Search API Client for Java
==================

This Java client let you easily use the Algolia Search API from your Java Application.
The service is currently in Beta, you can request an invite on our [website](http://www.algolia.com/pricing/).

Setup
-------------
To setup your project, follow these steps:

 1. Download and add files in `src` folder to your project
 2. Add [JSON-Java](https://github.com/douglascrockford/JSON-java) and [Apache HttpClient](http://hc.apache.org/downloads.cgi) to your project
 3. Initialize the client with your ApplicationID, API-Key and list of hostnames (you can find all of them on your Algolia account)

```java
  AlgoliaClient client = new AlgoliaClient("YourApplicationID", "YourAPIKey", 
                                           Arrays.asList("user-1.algolia.io", "user-2.algolia.io", "user-3.algolia.io"));
```


Quick Start
-------------
This quick start is a 30 seconds tutorial where you can discover how to index and search objects.

Without any prior-configuration, you can index the 13 US's biggest cities in the ```cities``` index with the following code:
```java
Index index = client.initIndex("cities");

index.addObject("{ \"name\": \"New York City\", \"population\": 8175133 }");
index.addObject("{ \"name\": \"Los Angeles\",   \"population\": 3792621 }");
index.addObject("{ \"name\": \"Chicago\",       \"population\": 2695598 }");
index.addObject("{ \"name\": \"Houston\",       \"population\": 2099451 }");
index.addObject("{ \"name\": \"Philadelphia\",  \"population\": 1526006 }");
index.addObject("{ \"name\": \"Phoenix\",       \"population\": 1445632 }");
index.addObject("{ \"name\": \"San Antonio\",   \"population\": 1327407 }");
index.addObject("{ \"name\": \"San Diego\",     \"population\": 1307402 }");
index.addObject("{ \"name\": \"Dallas\",        \"population\": 1197816 }");
index.addObject("{ \"name\": \"San Jose\",      \"population\": 945942 }");
index.addObject("{ \"name\": \"Indianapolis\",  \"population\": 829718 }");
index.addObject("{ \"name\": \"Jacksonville\",  \"population\": 821784 }");     
index.addObject("{ \"name\": \"San Francisco\", \"population\": 805235 }");
```
Objects added can be any valid JSON.

You can then start to search for a city name (even with typos):
```java
System.out.println(index.search(new Query("san fran")));
System.out.println(index.search(new Query("loz anqel")));
```

Settings can be customized to tune the index behavior. For example you can add a custom sort by population to the already good out-of-the-box relevance to raise bigger cities above smaller ones. To update the settings, use the following code:
```java
index.setSettings("{ \"customRanking\": [\"desc(population)\", \"asc(name)\"]}");
```

And then search for all cities that start with an "s":
```java
System.out.println(index.search(new Query("s")));
```

Search 
-------------
> **Opening note:** If you are building a web application, you may be more interested in using our [javascript client](https://github.com/algolia/algoliasearch-client-js) to send queries. It brings two benefits: (i) your users get a better response time by avoiding to go threw your servers, and (ii) it will offload your servers of unnecessary tasks.

To perform a search, you just need to initialize the index and perform a call to the search function.<br/>
You can use the following optional arguments on Query class:

 * **setAttributesToRetrieve**: specify the list of attribute names to retrieve.<br/>By default all attributes are retrieved.
 * **setAttributesToHighlight**: specify the list of attribute names to highlight.<br/>By default indexed attributes are highlighted.
 * **setMinWordSizeToAllowOneTypo**: the minimum number of characters in a query word to accept one typo in this word.<br/>Defaults to 3.
 * **setMinWordSizeToAllowTwoTypos**: the minimum number of characters in a query word to accept two typos in this word.<br/>Defaults to 7.
 * **getRankingInfo**: if set, the result hits will contain ranking information in _rankingInfo attribute.
 * **setPage**: *(pagination parameter)* page to retrieve (zero base).<br/>Defaults to 0.
 * **setNbHitsPerPage**: *(pagination parameter)* number of hits per page.<br/>Defaults to 10.
 * **aroundLatitudeLongitude**: search for entries around a given latitude/longitude.<br/>You specify the maximum distance in meters with the **radius** parameter (in meters).<br/>At indexing, you should specify geoloc of an object with the _geoloc attribute (in the form `{"_geoloc":{"lat":48.853409, "lng":2.348800}}`)
 * **insideBoundingBox**: search entries inside a given area defined by the two extreme points of a rectangle.<br/>At indexing, you should specify geoloc of an object with the _geoloc attribute (in the form `{"_geoloc":{"lat":48.853409, "lng":2.348800}}`)
 * **tags**: filter the query by a set of tags. You can AND tags by separating them by commas. To OR tags, you must add parentheses. For example, `tag1,(tag2,tag3)` means *tag1 AND (tag2 OR tag3)*.<br/>At indexing, tags should be added in the _tags attribute of objects (for example `{"_tags":["tag1","tag2"]}` )

```java
Index index = client.initIndex("MyIndexName");
System.out.println(index.search(new Query("query string")));
System.out.println(index.search(new Query("query string").
                setAttributesToRetrieve(Arrays.asList("population")).
                setNbHitsPerPage(50)));
```

The server response will look like:

```javascript
{
    "hits":[
            { "name": "Betty Jane Mccamey",
              "company": "Vita Foods Inc.",
              "email": "betty@mccamey.com",
              "objectID": "6891Y2usk0",
              "_highlightResult": {"name": {"value": "Betty <em>Jan</em>e Mccamey", "matchLevel": "full"}, 
                                   "company": {"value": "Vita Foods Inc.", "matchLevel": "none"},
                                   "email": {"value": "betty@mccamey.com", "matchLevel": "none"} }
            },
            { "name": "Gayla Geimer Dan", 
              "company": "Ortman Mccain Co", 
              "email": "gayla@geimer.com", 
              "objectID": "ap78784310" 
              "_highlightResult": {"name": {"value": "Gayla Geimer <em>Dan</em>", "matchLevel": "full" },
                                   "company": {"value": "Ortman Mccain Co", "matchLevel": "none" },
                                   "email": {"highlighted": "gayla@geimer.com", "matchLevel": "none" } }
            }],
    "page":0,
    "nbHits":2,
    "nbPages":1,
    "hitsPerPage:":20,
    "processingTimeMS":1,
    "query":"jan"
}
```

Add a new object in the Index
-------------

Each entry in an index has a unique identifier called `objectID`. You have two ways to add en entry in the index:

 1. Using automatic `objectID` assignement, you will be able to retrieve it in the answer.
 2. Passing your own `objectID`

You don't need to explicitely create an index, it will be automatically created the first time you add an object.
Objects are schema less, you don't need any configuration to start indexing. The settings section provide details about advanced settings.

Example with automatic `objectID` assignement:

```java
JSONObject obj = index.addObject("{ \"name\": \"San Francisco\", \"population\": 805235 }");
System.out.println(obj.getString("objectID"));
```

Example with manual `objectID` assignement:

```java
JSONObject obj = index.addObject("{ \"name\": \"San Francisco\", \"population\": 805235 }", "myID");
System.out.println(obj.getString("objectID"));
```

Update an existing object in the Index
-------------

You have two options to update an existing object:

 1. Replace all its attributes.
 2. Replace only some attributes.

Example to replace all the content of an existing object:

```java
index.saveObject("{ \"name\": \"Los Angeles\", \"population\": 3792621 }", "myID");
```

Example to update only the population attribute of an existing object:

```java
index.partialUpdateObject("{ \"population\": 3792621 }", "myID");
```

Get an object
-------------

You can easily retrieve an object using its `objectID` and optionnaly a list of attributes you want to retrieve (using comma as separator):

```java
// Retrieves all attributes
index.getObject("myID");
// Retrieves only the name attribute
index.getObject("myID", Arrays.asList("name"));
```

Delete an object
-------------

You can delete an object using its `objectID`:

```java
index.deleteObject("myID");
```

Index Settings
-------------

You can retrieve all settings using the `getSettings` function. The result will contains the following attributes:

 * **minWordSizeForApprox1**: (integer) the minimum number of characters to accept one typo (default = 3).
 * **minWordSizeForApprox2**: (integer) the minimum number of characters to accept two typos (default = 7).
 * **hitsPerPage**: (integer) the number of hits per page (default = 10).
 * **attributesToRetrieve**: (array of strings) default list of attributes to retrieve in objects.
 * **attributesToHighlight**: (array of strings) default list of attributes to highlight
 * **attributesToIndex**: (array of strings) the list of fields you want to index.<br/>By default all textual attributes of your objects are indexed, but you should update it to get optimal results.<br/>This parameter has two important uses:
 * *Limit the attributes to index*.<br/>For example if you store a binary image in base64, you want to store it and be able to retrieve it but you don't want to search in the base64 string.
 * *Control part of the ranking*.<br/>Matches in attributes at the beginning of the list will be considered more important than matches in attributes further down the list. 
 * **ranking**: (array of strings) controls the way results are sorted.<br/>We have four available criteria: 
  * **typo**: sort according to number of typos,
  * **geo**: sort according to decreassing distance when performing a geo-location based search,
  * **position**: sort according to the proximity of query words in the object, 
  * **custom**: sort according to a user defined formula set in **customRanking** attribute.<br/>The standard order is ["typo", "geo", position", "custom"]
 * **customRanking**: (array of strings) lets you specify part of the ranking.<br/>The syntax of this condition is an array of strings containing attributes prefixed by asc (ascending order) or desc (descending order) operator.
 For example `"customRanking" => ["desc(population)", "asc(name)"]`

You can easily retrieve settings or update them:

```java
System.out.println(index.getSettings());
```

```java
index.setSettings("{ \"customRanking\": [\"desc(population)\", \"asc(name)\"]}");
```
