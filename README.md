# Algolia Search API Client for Android

[Algolia Search](https://www.algolia.com) is a hosted full-text, numerical, and faceted search engine capable of delivering realtime results from the first keystroke.

[![Build Status](https://travis-ci.org/algolia/algoliasearch-client-android.svg?branch=master)](https://travis-ci.org/algolia/algoliasearch-client-android) [![GitHub version](https://badge.fury.io/gh/algolia%2Falgoliasearch-client-android.svg)](http://badge.fury.io/gh/algolia%2Falgoliasearch-client-android)
_Note: If you were using **version 2.x** of our Android client, read the [migration guide to version 3.x](https://github.com/algolia/algoliasearch-client-android/wiki/Migration-guide-to-version-3.x)._

Our Android client lets you easily use the [Algolia Search API](https://www.algolia.com/doc/rest) from your Android application. It wraps the [Algolia Search REST API](https://www.algolia.com/doc/rest).
This project is open-source under the [MIT License](./LICENSE). [Your contributions](https://github.com/algolia/algoliasearch-client-android/pull/new) are welcome! Please use our [formatting configuration](https://github.com/algolia/CodingStyle#android) to keep the coding style consistent.


# Table of Contents


**Getting Started**

1. [Install](#install)
1. [Init index - `initIndex`](#init-index---initindex)
1. [Quick Start](#quick-start)

**Search**

1. [Search in an index - `searchAsync`](#search-in-an-index---searchasync)
1. [Search Response Format](#search-response-format)
1. [Search Parameters](#search-parameters)
1. [Search in indices - `multipleQueriesAsync`](#search-in-indices---multiplequeriesasync)
1. [Get Objects - `getObjectsAsync`](#get-objects---getobjectsasync)
1. [Search for facet values - `searchForFacetValues`](#search-for-facet-values---searchforfacetvalues)
1. [Search cache](#search-cache)

**Indexing**

1. [Add Objects - `addObjectsAsync`](#add-objects---addobjectsasync)
1. [Update objects - `saveObjectsAsync`](#update-objects---saveobjectsasync)
1. [Partial update objects - `partialUpdateObjectsAsync`](#partial-update-objects---partialupdateobjectsasync)
1. [Delete objects - `deleteObjectsAsync`](#delete-objects---deleteobjectsasync)
1. [Delete by query - `deleteByQueryAsync`](#delete-by-query---deletebyqueryasync)
1. [Wait for operations - `waitTaskAsync`](#wait-for-operations---waittaskasync)

**Settings**

1. [Get settings - `getSettingsAsync`](#get-settings---getsettingsasync)
1. [Set settings - `setSettingsAsync`](#set-settings---setsettingsasync)
1. [Index settings parameters](#index-settings-parameters)

**Parameters**

1. [Overview](#overview)
1. [Search](#search)
1. [Attributes](#attributes)
1. [Ranking](#ranking)
1. [Filtering / Faceting](#filtering--faceting)
1. [Highlighting / Snippeting](#highlighting--snippeting)
1. [Pagination](#pagination)
1. [Typos](#typos)
1. [Geo-Search](#geo-search)
1. [Query Strategy](#query-strategy)
1. [performance](#performance)
1. [Advanced](#advanced)

**Manage Indices**

1. [Create an index](#create-an-index)
1. [List indices - `listIndexesAsync`](#list-indices---listindexesasync)

**Advanced**

1. [Custom batch - `batchAsync`](#custom-batch---batchasync)
1. [Backup / Export an index - `browseAsync`](#backup--export-an-index---browseasync)
1. [REST API](#rest-api)


# Guides & Tutorials

Check our [online guides](https://www.algolia.com/doc):

* [Data Formatting](https://www.algolia.com/doc/indexing/formatting-your-data)
* [Import and Synchronize data](https://www.algolia.com/doc/indexing/import-synchronize-data/php)
* [Autocomplete](https://www.algolia.com/doc/search/auto-complete)
* [Instant search page](https://www.algolia.com/doc/search/instant-search)
* [Filtering and Faceting](https://www.algolia.com/doc/search/filtering-faceting)
* [Sorting](https://www.algolia.com/doc/relevance/sorting)
* [Ranking Formula](https://www.algolia.com/doc/relevance/ranking)
* [Typo-Tolerance](https://www.algolia.com/doc/relevance/typo-tolerance)
* [Geo-Search](https://www.algolia.com/doc/geo-search/geo-search-overview)
* [Security](https://www.algolia.com/doc/security/best-security-practices)
* [API-Keys](https://www.algolia.com/doc/security/api-keys)
* [REST API](https://www.algolia.com/doc/rest)


# Getting Started



## Install

Add the following dependency to your Gradle build file:

```gradle
dependencies {
    // [...]
    compile 'com.algolia:algoliasearch-android:3.5'
}
```

## Init index - `initIndex` 

To initialize the client, you need your **Application ID** and **API Key**. You can find both of them on [your Algolia account](https://www.algolia.com/api-keys).

```java
Client client = new Client("YOUR_APP_ID", "YOUR_API_KEY");
``````csharp
using Algolia.Search;

AlgoliaClient client = new AlgoliaClient("YourApplicationID", "YourAPIKey");
```

**Note**: If you're using Algolia in an ASP.NET project you might experience some deadlocks while using our asynchronous API. You can fix it by calling the following method:

## Quick Start

In 30 seconds, this quick start tutorial will show you how to index and search objects.

Without any prior configuration, you can start indexing contacts in the ```contacts``` index using the following code:

```java
Index index = client.initIndex("contacts");
index.addObjectAsync(new JSONObject()
      .put("firstname", "Jimmie")
      .put("lastname", "Barninger")
      .put("followers", 93)
      .put("company", "California Paint"), null);
index.addObjectAsync(new JSONObject()
      .put("firstname", "Warren")
      .put("lastname", "Speach")
      .put("followers", 42)
      .put("company", "Norwalk Crmc"), null);
```

You can now search for contacts using firstname, lastname, company, etc. (even with typos):

```java
CompletionHandler completionHandler = new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
};
// search by firstname
index.searchAsync(new Query("jimmie"), completionHandler);
// search a firstname with typo
index.searchAsync(new Query("jimie"), completionHandler);
// search for a company
index.searchAsync(new Query("california paint"), completionHandler);
// search for a firstname & company
index.searchAsync(new Query("jimmie paint"), completionHandler);
```

Settings can be customized to tune the search behavior. For example, you can add a custom sort by number of followers to the already great built-in relevance:

```java
JSONObject settings = new JSONObject().append("customRanking", "desc(followers)");
index.setSettingsAsync(settings, null);
```

You can also configure the list of attributes you want to index by order of importance (first = most important):

```java
JSONObject settings = new JSONObject()
    .append("searchableAttributes", "lastname")
    .append("searchableAttributes", "firstname")
    .append("searchableAttributes", "company");
index.setSettingsAsync(settings, null);
```

Since the engine is designed to suggest results as you type, you'll generally search by prefix. In this case the order of attributes is very important to decide which hit is the best:

```java
index.searchAsync(new Query("or"), new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
index.searchAsync(new Query("jim"), new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```


# Search



## Search in an index - `searchAsync` 

To perform a search, you only need to initialize the index and perform a call to the search function.

The search query allows only to retrieve 1000 hits. If you need to retrieve more than 1000 hits (e.g. for SEO), you can use [Backup / Export an index](#backup--export-an-index).

```java
Index index = client.initIndex("contacts");
Query query = new Query("query string")
    .setAttributesToRetrieve(Arrays.asList("firstname", "lastname"))
    .setNbHitsPerPage(50);
index.searchAsync(query, new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```

## Search Response Format

### Sample

The server response will look like:

```json
{
  "hits": [
    {
      "firstname": "Jimmie",
      "lastname": "Barninger",
      "objectID": "433",
      "_highlightResult": {
        "firstname": {
          "value": "<em>Jimmie</em>",
          "matchLevel": "partial"
        },
        "lastname": {
          "value": "Barninger",
          "matchLevel": "none"
        },
        "company": {
          "value": "California <em>Paint</em> & Wlpaper Str",
          "matchLevel": "partial"
        }
      }
    }
  ],
  "page": 0,
  "nbHits": 1,
  "nbPages": 1,
  "hitsPerPage": 20,
  "processingTimeMS": 1,
  "query": "jimmie paint",
  "params": "query=jimmie+paint&attributesToRetrieve=firstname,lastname&hitsPerPage=50"
}
```

### Fields

- `hits` (array): The hits returned by the search, sorted according to the ranking formula.

    Hits are made of the JSON objects that you stored in the index; therefore, they are mostly schema-less. However, Algolia does enrich them with a few additional fields:

    - `_highlightResult` (object, optional): Highlighted attributes. *Note: Only returned when [attributesToHighlight](#attributestohighlight) is non-empty.*

        - `${attribute_name}` (object): Highlighting for one attribute.

            - `value` (string): Markup text with occurrences highlighted. The tags used for highlighting are specified via [highlightPreTag](#highlightpretag) and [highlightPostTag](#highlightposttag).

            - `matchLevel` (string, enum) = {`none` | `partial` | `full`}: Indicates how well the attribute matched the search query.

            - `matchedWords` (array): List of words *from the query* that matched the object.

            - `fullyHighlighted` (boolean): Whether the entire attribute value is highlighted.

    - `_snippetResult` (object, optional): Snippeted attributes. *Note: Only returned when [attributesToSnippet](#attributestosnippet) is non-empty.*

        - `${attribute_name}` (object): Snippeting for the corresponding attribute.

            - `value` (string): Markup text with occurrences highlighted and optional ellipsis indicators. The tags used for highlighting are specified via [highlightPreTag](#highlightpretag) and [highlightPostTag](#highlightposttag). The text used to indicate ellipsis is specified via [snippetEllipsisText](#snippetellipsistext).

            - `matchLevel` (string, enum) = {`none` | `partial` | `full`}: Indicates how well the attribute matched the search query.

    - `_rankingInfo` (object, optional): Ranking information. *Note: Only returned when [getRankingInfo](#getrankinginfo) is `true`.*

        - `nbTypos` (integer): Number of typos encountered when matching the record. Corresponds to the `typos` ranking criterion in the ranking formula.

        - `firstMatchedWord` (integer): Position of the most important matched attribute in the attributes to index list. Corresponds to the `attribute` ranking criterion in the ranking formula.

        - `proximityDistance` (integer): When the query contains more than one word, the sum of the distances between matched words. Corresponds to the `proximity` criterion in the ranking formula.

        - `userScore` (integer): Custom ranking for the object, expressed as a single numerical value. Conceptually, it's what the position of the object would be in the list of all objects sorted by custom ranking. Corresponds to the `custom` criterion in the ranking formula.

        - `geoDistance` (integer): Distance between the geo location in the search query and the best matching geo location in the record, divided by the geo precision.

        - `geoPrecision` (integer): Precision used when computed the geo distance, in meters. All distances will be floored to a multiple of this precision.

        - `nbExactWords` (integer): Number of exactly matched words. If `alternativeAsExact` is set, it may include plurals and/or synonyms.

        - `words` (integer): Number of matched words, including prefixes and typos.

        - `filters` (integer): *This field is reserved for advanced usage.* It will be zero in most cases.

    - `_distinctSeqID` (integer): *Note: Only returned when [distinct](#distinct) is non-zero.* When two consecutive results have the same value for the attribute used for "distinct", this field is used to distinguish between them.

- `nbHits` (integer): Number of hits that the search query matched.

- `page` (integer): Index of the current page (zero-based). See the [page](#page) search parameter. *Note: Not returned if you use `offset`/`length` for pagination.*

- `hitsPerPage` (integer): Maximum number of hits returned per page. See the [hitsPerPage](#hitsperpage) search parameter. *Note: Not returned if you use `offset`/`length` for pagination.*

- `nbPages` (integer): Number of pages corresponding to the number of hits. Basically, `ceil(nbHits / hitsPerPage)`. *Note: Not returned if you use `offset`/`length` for pagination.*

- `processingTimeMS` (integer): Time that the server took to process the request, in milliseconds. *Note: This does not include network time.*

- `query` (string): An echo of the query text. See the [query](#query) search parameter.

- `queryAfterRemoval` (string, optional): *Note: Only returned when [removeWordsIfNoResults](#removewordsifnoresults) is set to `lastWords` or `firstWords`.* A markup text indicating which parts of the original query have been removed in order to retrieve a non-empty result set. The removed parts are surrounded by `<em>` tags.

- `params` (string, URL-encoded): An echo of all search parameters.

- `message` (string, optional): Used to return warnings about the query.

- `aroundLatLng` (string, optional): *Note: Only returned when [aroundLatLngViaIP](#aroundlatlngviaip) is set.* The computed geo location. **Warning: for legacy reasons, this parameter is a string and not an object.** Format: `${lat},${lng}`, where the latitude and longitude are expressed as decimal floating point numbers.

- `automaticRadius` (integer, optional): *Note: Only returned for geo queries without an explicitly specified radius (see `aroundRadius`).* The automatically computed radius. **Warning: for legacy reasons, this parameter is a string and not an integer.**

When [getRankingInfo](#getrankinginfo) is set to `true`, the following additional fields are returned:

- `serverUsed` (string): Actual host name of the server that processed the request. (Our DNS supports automatic failover and load balancing, so this may differ from the host name used in the request.)

- `parsedQuery` (string): The query string that will be searched, after normalization. Normalization includes removing stop words (if [removeStopWords](#removestopwords) is enabled), and transforming portions of the query string into phrase queries (see [advancedSyntax](#advancedsyntax)).

- `timeoutCounts` (boolean): Whether a timeout was hit when computing the facet counts. When `true`, the counts will be interpolated (i.e. approximate). See also `exhaustiveFacetsCount`.

- `timeoutHits` (boolean): Whether a timeout was hit when retrieving the hits. When true, some results may be missing.

... and ranking information is also added to each of the hits (see above).

When [facets](#facets) is non-empty, the following additional fields are returned:

- `facets` (object): Maps each facet name to the corresponding facet counts:

    - `${facet_name}` (object): Facet counts for the corresponding facet name:

        - `${facet_value}` (integer): Count for this facet value.

- `facets_stats` (object, optional): *Note: Only returned when at least one of the returned facets contains numerical values.* Statistics for numerical facets:

    - `${facet_name}` (object): The statistics for a given facet:

        - `min` (integer | float): The minimum value in the result set.

        - `max` (integer | float): The maximum value in the result set.

        - `avg` (integer | float): The average facet value in the result set.

        - `sum` (integer | float): The sum of all values in the result set.

- `exhaustiveFacetsCount` (boolean): Whether the counts are exhaustive (`true`) or approximate (`false`). *Note: When using [distinct](#distinct), the facet counts cannot be exhaustive.*

## Search Parameters

Here is the list of parameters you can use with the search method (`search` [scope](#scope)):
Parameters that can also be used in a setSettings also have the `indexing` [scope](#scope)

**Search**

- [query](#query) `search`

**Attributes**

- [attributesToRetrieve](#attributestoretrieve) `settings`, `search`
- [restrictSearchableAttributes](#restrictsearchableattributes) `search`

**Filtering / Faceting**

- [filters](#filters) `search`
- [facets](#facets) `search`
- [maxValuesPerFacet](#maxvaluesperfacet) `settings`, `search`
- [facetFilters](#facetfilters) `search`

**Highlighting / Snippeting**

- [attributesToHighlight](#attributestohighlight) `settings`, `search`
- [attributesToSnippet](#attributestosnippet) `settings`, `search`
- [highlightPreTag](#highlightpretag) `settings`, `search`
- [highlightPostTag](#highlightposttag) `settings`, `search`
- [snippetEllipsisText](#snippetellipsistext) `settings`, `search`
- [restrictHighlightAndSnippetArrays](#restricthighlightandsnippetarrays) `settings`, `search`

**Pagination**

- [page](#page) `search`
- [hitsPerPage](#hitsperpage) `settings`, `search`
- [offset](#offset) `search`
- [length](#length) `search`

**Typos**

- [minWordSizefor1Typo](#minwordsizefor1typo) `settings`, `search`
- [minWordSizefor2Typos](#minwordsizefor2typos) `settings`, `search`
- [typoTolerance](#typotolerance) `settings`, `search`
- [allowTyposOnNumericTokens](#allowtyposonnumerictokens) `settings`, `search`
- [ignorePlurals](#ignoreplurals) `settings`, `search`
- [disableTypoToleranceOnAttributes](#disabletypotoleranceonattributes) `settings`, `search`

**Geo-Search**

- [aroundLatLng](#aroundlatlng) `search`
- [aroundLatLngViaIP](#aroundlatlngviaip) `search`
- [aroundRadius](#aroundradius) `search`
- [aroundPrecision](#aroundprecision) `search`
- [minimumAroundRadius](#minimumaroundradius) `search`
- [insideBoundingBox](#insideboundingbox) `search`
- [insidePolygon](#insidepolygon) `search`

**Query Strategy**

- [removeWordsIfNoResults](#removewordsifnoresults) `settings`, `search`
- [advancedSyntax](#advancedsyntax) `settings`, `search`
- [optionalWords](#optionalwords) `settings`, `search`
- [removeStopWords](#removestopwords) `settings`, `search`
- [exactOnSingleWordQuery](#exactonsinglewordquery) `settings`, `search`
- [alternativesAsExact](#alternativesasexact) `setting`, `search`

**Advanced**

- [minProximity](#minproximity) `settings`, `search`
- [responseFields](#responsefields) `settings`, `search`
- [distinct](#distinct) `settings`, `search`
- [getRankingInfo](#getrankinginfo) `search`
- [numericFilters](#numericfilters) `search`
- [tagFilters (deprecated)](#tagfilters-deprecated) `search`
- [analytics](#analytics) `search`
- [analyticsTags](#analyticstags) `search`
- [synonyms](#synonyms) `search`
- [replaceSynonymsInHighlight](#replacesynonymsinhighlight) `settings`, `search`

## Search in indices - `multipleQueriesAsync` 

You can send multiple queries with a single API call using a batch of queries:

```java
// perform 3 queries in a single API call:
//  - 1st query targets index `categories`
//  - 2nd and 3rd queries target index `products`

List<IndexQuery> queries = new ArrayList<>();

queries.add(new IndexQuery("categories", new Query(myQueryString).setHitsPerPage(3)));
queries.add(new IndexQuery("products", new Query(myQueryString).setHitsPerPage(3).set("filters", "_tags:promotion"));
queries.add(new IndexQuery("products", new Query(myQueryString).setHitsPerPage(10)));

client.multipleQueriesAsync(queries, new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```

You can specify a `strategy` parameter to optimize your multiple queries:

- `none`: Execute the sequence of queries until the end.
- `stopIfEnoughMatches`: Execute the sequence of queries until the number of hits is reached by the sum of hits.

### Response

The resulting JSON contains the following fields:

- `results` (array): The results for each request, in the order they were submitted. The contents are the same as in [Search in an index](#search-in-an-index).
    Each result also includes the following additional fields:

    - `index` (string): The name of the targeted index.
    - `processed` (boolean, optional): *Note: Only returned when `strategy` is `stopIfEnoughmatches`.* Whether the query was processed.

## Get Objects - `getObjectsAsync` 

You can easily retrieve an object using its `objectID` and optionally specify a comma separated list of attributes you want:

```java
// Retrieves all attributes
index.getObjectAsync("myID", new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
// Retrieves only the firstname attribute
index.getObjectAsync("myID", Arrays.asList("firstname"), new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```

You can also retrieve a set of objects:

```java
index.getObjectsAsync(Arrays.asList("myID1", "myID2"), new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```

## Search for facet values - `searchForFacetValues` 

When a facet can take many different values, it can be useful to search within them. The typical use case is to build
an autocomplete menu for facet refinements, but of course other use cases may apply as well.

The facet search is different from a regular search in the sense that it retrieves *facet values*, not *objects*.
In other words, a value will only be returned once, even if it matches many different objects. How many objects it
matches is indicated by a count.

The results are sorted by decreasing count. Maximum 10 results are returned. No pagination is possible.

The facet search can optionally be restricted by a regular search query. In that case, it will return only facet values
that both:

1. match the facet query; and
2. are contained in objects matching the regular search query.

**Warning:** *For a facet to be searchable, it must have been declared with the `searchable()` modifier in the [attributesForFaceting](#attributesforfaceting) index setting.*

#### Example

Let's imagine we have objects similar to this one:

```json
{
    "name": "iPhone 7 Plus",
    "brand": "Apple",
    "category": [
        "Mobile phones",
        "Electronics"
    ]
}
```

Then:

```java
# Search the values of the "category" facet matching "phone".
index.searchForFacetValues("category", "phone", new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```

... could return:

```json
{
    "facetHits": [
        {
            "value": "Mobile phones",
            "highlighted": "Mobile <em>phone</em>s",
            "count": 507
        },
        {
            "value": "Phone cases",
            "highlighted": "<em>Phone</em> cases",
            "count": 63
        }
    ]
}
```

Let's filter with an additional, regular search query:

```java
// Search the "category" facet for values matching "phone" in records
// having "Apple" in their "brand" facet.
Query query = new Query().setFilters("brand:Apple");
index.searchForFacetValues("category", "phone", query, new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```

... could return:

```json
{
    "facetHits": [
        {
            "value": "Mobile phones",
            "highlighted": "Mobile <em>phone</em>s",
            "count": 41
        }
    ]
}
```

## Search cache

You can easily cache the results of the search queries by enabling the search cache.
The results will be cached during a defined amount of time (default: 2 min).
There is no pre-caching mechanism but you can simulate it by making a preemptive search query.

By default, the cache is disabled.

```java
// Enable the search cache with default settings.
index.enableSearchCache();
```

... or:

```java
// Enable the search cache with a TTL of 5 minutes and maximum 20 requests in the cache.
index.enableSearchCache(300, 20);
```


# Indexing



## Add Objects - `addObjectsAsync` 

Each entry in an index has a unique identifier called `objectID`. There are two ways to add an entry to the index:

 1. Supplying your own `objectID`.
 2. Using automatic `objectID` assignment. You will be able to access it in the answer.

You don't need to explicitly create an index, it will be automatically created the first time you add an object.
Objects are schema less so you don't need any configuration to start indexing.
If you wish to configure things, the settings section provides details about advanced settings.

Example with automatic `objectID` assignments:

```java
List<JSONObject> array = new ArrayList<JSONObject>();
array.add(new JSONObject().put("firstname", "Jimmie").put("lastname", "Barninger"));
array.add(new JSONObject().put("firstname", "Warren").put("lastname", "Speach"));
index.addObjectsAsync(new JSONArray(array), null);
```

Example with manual `objectID` assignments:

```java
List<JSONObject> array = new ArrayList<JSONObject>();
array.add(new JSONObject().put("objectID", "1").put("firstname", "Jimmie").put("lastname", "Barninger"));
array.add(new JSONObject().put("objectID", "2").put("firstname", "Warren").put("lastname", "Speach"));
index.addObjectsAsync(new JSONArray(array), null);
```

To add a single object, use the [Add Objects](#add-objects) method:

```java
JSONObject object = new JSONObject()
    .put("firstname", "Jimmie")
    .put("lastname", "Barninger");
index.addObjectAsync(object, "myID", null);
```

## Update objects - `saveObjectsAsync` 

You have three options when updating an existing object:

 1. Replace all its attributes.
 2. Replace only some attributes.
 3. Apply an operation to some attributes.

Example on how to replace all attributes existing objects:

```java
List<JSONObject> array = new ArrayList<JSONObject>();
array.add(new JSONObject().put("firstname", "Jimmie").put("lastname", "Barninger").put("objectID", "SFO"));
array.add(new JSONObject().put("firstname", "Warren").put("lastname", "Speach").put("objectID", "LA"));
index.saveObjectsAsync(new JSONArray(array), null);
```

To update a single object, you can use the following method:

```java
JSONObject object = new JSONObject()
    .put("firstname", "Jimmie")
    .put("lastname", "Barninger")
    .put("city", "New York");
index.saveObjectAsync(object, "myID", null);
```

## Partial update objects - `partialUpdateObjectsAsync` 

You have many ways to update an object's attributes:

 1. Set the attribute value
 2. Add a string or number element to an array
 3. Remove an element from an array
 4. Add a string or number element to an array if it doesn't exist
 5. Increment an attribute
 6. Decrement an attribute

Example to update only the city attribute of an existing object:

```java
index.partialUpdateObjectAsync(new JSONObject("{\"city\": \"San Francisco\"}"), "myID", null);
```

Example to add a tag:

```java
index.partialUpdateObjectAsync(new JSONObject("{\"_tags\": {\"value\": \"MyTags\", \"_operation\": \"Add\"}}"), "myID", null);
```

Example to remove a tag:

```java
index.partialUpdateObjectAsync(new JSONObject("{\"_tags": {\"value\": \"MyTags\", \"_operation\": \"Remove\"}}"), "myID", null);
```

Example to add a tag if it doesn't exist:

```java
index.partialUpdateObjectAsync(new JSONObject("{\"_tags\": {\"value\": \"MyTags\", \"_operation\": \"AddUnique\"}}", "myID", null);
```

Example to increment a numeric value:

```java
index.partialUpdateObjectAsync(new JSONObject("{\"price\": {\"value\": 42, \"_operation\": \"Increment\"}}"), "myID", null);
```

Note: Here we are incrementing the value by `42`. To increment just by one, put
`value:1`.

Example to decrement a numeric value:

```java
index.partialUpdateObjectAsync(new JSONObject("{\"price\": {\"value\": 42, \"_operation\": \"Decrement\"}}", "myID", null);
```

Note: Here we are decrementing the value by `42`. To decrement just by one, put
`value:1`.

To partial update multiple objects using one API call, you can use the `[Partial update objects](#partial-update-objects)` method:

```java
List<JSONObject> array = new ArrayList<JSONObject>();
array.add(new JSONObject().put("firstname", "Jimmie").put("objectID", "SFO"));
array.add(new JSONObject().put("firstname", "Warren").put("objectID", "LA"));
index.partialUpdateObjectsAsync(new JSONArray(array), null);
```

## Delete objects - `deleteObjectsAsync` 

You can delete objects using their `objectID`:

```java
index.deleteObjectsAsync(Arrays.asList("myID1", "myID2"), null);
```

To delete a single object, you can use the `[Delete objects](#delete-objects)` method:

```java
index.deleteObjectAsync("myID", null);
```

## Delete by query - `deleteByQueryAsync` 

You can delete all objects matching a single query with the following code. Internally, the API client performs the query, deletes all matching hits, and waits until the deletions have been applied.

Take your precautions when using this method. Calling it with an empty query will result in cleaning the index of all its records.

```java
Query query = /* [ ... ] */;
index.deleteByQueryAsync(query, null);
```

## Wait for operations - `waitTaskAsync` 

All write operations in Algolia are asynchronous by design.

It means that when you add or update an object to your index, our servers will
reply to your request with a `taskID` as soon as they understood the write
operation.

The actual insert and indexing will be done after replying to your code.

You can wait for a task to complete using the `waitTask` method on the `taskID` returned by a write operation.

For example, to wait for indexing of a new object:

```java
JSONObject object = new JSONObject().put("firstname", "Jimmie").put("lastname", "Barninger");
index.addObjectAsync(object, new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        if (error != null) {
            // Handle error.
        } else {
            String taskID = content.optString("taskID", null);
            if (taskID == null) {
                // Handle error.
            } else {
                index.waitTask(taskID, new CompletionHandler() {
                    @Override
                    public void requestCompleted(JSONObject content, AlgoliaException error) {
                        if (error == null) {
                            // Task is published.
                        }
                    }
                });
            }
        }
    }
});
```

If you want to ensure multiple objects have been indexed, you only need to check
the biggest `taskID`.


# Settings



## Get settings - `getSettingsAsync` 

You can retrieve settings:

```java
index.getSettingsAsync(new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```

## Set settings - `setSettingsAsync` 

```java
index.setSettingsAsync(new JSONObject().append("customRanking", "desc(followers)"), null);
```

You can find the list of parameters you can set in the [Settings Parameters](#index-settings-parameters) section

**Warning**

Performance wise, it's better to do a `setSettingsAsync` before pushing the data

### Replica settings

You can forward all settings updates to the replicas of an index by using the `forwardToReplicas` option:

```android
JSONObject settings = new JSONObject("{\"attributesToRetrieve\": [\"name\", \"birthdate\"]}");
JSONObject setSettingsResult = index.setSettings(settings, true);
index.waitTask(setSettingsResult.getString("taskID"));
```

## Index settings parameters

Here is the list of parameters you can use with the set settings method (`settings` [scope](#scope)).

Parameters that can be overridden at search time also have the `search` [scope](#scope).

**Attributes**

- [searchableAttributes](#searchableattributes) `settings`
- [attributesForFaceting](#attributesforfaceting) `settings`
- [unretrievableAttributes](#unretrievableattributes) `settings`
- [attributesToRetrieve](#attributestoretrieve) `settings`, `search`

**Ranking**

- [ranking](#ranking) `settings`
- [customRanking](#customranking) `settings`
- [replicas](#replicas) `settings`

**Filtering / Faceting**

- [maxValuesPerFacet](#maxvaluesperfacet) `settings`, `search`

**Highlighting / Snippeting**

- [attributesToHighlight](#attributestohighlight) `settings`, `search`
- [attributesToSnippet](#attributestosnippet) `settings`, `search`
- [highlightPreTag](#highlightpretag) `settings`, `search`
- [highlightPostTag](#highlightposttag) `settings`, `search`
- [snippetEllipsisText](#snippetellipsistext) `settings`, `search`
- [restrictHighlightAndSnippetArrays](#restricthighlightandsnippetarrays) `settings`, `search`

**Pagination**

- [hitsPerPage](#hitsperpage) `settings`, `search`
- [paginationLimitedTo](#paginationlimitedto) `settings`

**Typos**

- [minWordSizefor1Typo](#minwordsizefor1typo) `settings`, `search`
- [minWordSizefor2Typos](#minwordsizefor2typos) `settings`, `search`
- [typoTolerance](#typotolerance) `settings`, `search`
- [allowTyposOnNumericTokens](#allowtyposonnumerictokens) `settings`, `search`
- [ignorePlurals](#ignoreplurals) `settings`, `search`
- [disableTypoToleranceOnAttributes](#disabletypotoleranceonattributes) `settings`, `search`
- [disableTypoToleranceOnWords](#disabletypotoleranceonwords) `settings`
- [separatorsToIndex](#separatorstoindex) `settings`

**Query Strategy**

- [queryType](#querytype) `settings`
- [removeWordsIfNoResults](#removewordsifnoresults) `settings`, `search`
- [advancedSyntax](#advancedsyntax) `settings`, `search`
- [optionalWords](#optionalwords) `settings`, `search`
- [removeStopWords](#removestopwords) `settings`, `search`
- [disableExactOnAttributes](#disableexactonattributes) `settings`
- [exactOnSingleWordQuery](#exactonsinglewordquery) `settings`, `search`

**performance**

- [numericAttributesForFiltering](#numericattributesforfiltering) `settings`
- [allowCompressionOfIntegerArray](#allowcompressionofintegerarray) `settings`

**Advanced**

- [attributeForDistinct](#attributefordistinct) `settings`
- [placeholders](#placeholders) `settings`
- [altCorrections](#altcorrections) `settings`
- [minProximity](#minproximity) `settings`, `search`
- [responseFields](#responsefields) `settings`, `search`
- [distinct](#distinct) `settings`, `search`
- [replaceSynonymsInHighlight](#replacesynonymsinhighlight) `settings`, `search`


# Parameters



## Overview

### Scope

Each parameter in this page has a scope. Depending on the scope, you can use the parameter within the `setSettings`
and/or the `search` method

They are three scopes:

- `settings`: The setting can only be used in the `setSettings` method
- `search`: The setting can only be used in the `search` method
- `settings` `search`: The setting can be used in the `setSettings` method and be override in the`search` method

### Parameters List

**Search**

- [query](#query) `search`

**Attributes**

- [searchableAttributes](#searchableattributes) `settings`
- [attributesForFaceting](#attributesforfaceting) `settings`
- [unretrievableAttributes](#unretrievableattributes) `settings`
- [attributesToRetrieve](#attributestoretrieve) `settings`, `search`
- [restrictSearchableAttributes](#restrictsearchableattributes) `search`

**Ranking**

- [ranking](#ranking) `settings`
- [customRanking](#customranking) `settings`
- [replicas](#replicas) `settings`

**Filtering / Faceting**

- [filters](#filters) `search`
- [facets](#facets) `search`
- [maxValuesPerFacet](#maxvaluesperfacet) `settings`, `search`
- [facetFilters](#facetfilters) `search`

**Highlighting / Snippeting**

- [attributesToHighlight](#attributestohighlight) `settings`, `search`
- [attributesToSnippet](#attributestosnippet) `settings`, `search`
- [highlightPreTag](#highlightpretag) `settings`, `search`
- [highlightPostTag](#highlightposttag) `settings`, `search`
- [snippetEllipsisText](#snippetellipsistext) `settings`, `search`
- [restrictHighlightAndSnippetArrays](#restricthighlightandsnippetarrays) `settings`, `search`

**Pagination**

- [page](#page) `search`
- [hitsPerPage](#hitsperpage) `settings`, `search`
- [offset](#offset) `search`
- [length](#length) `search`
- [paginationLimitedTo](#paginationlimitedto) `settings`

**Typos**

- [minWordSizefor1Typo](#minwordsizefor1typo) `settings`, `search`
- [minWordSizefor2Typos](#minwordsizefor2typos) `settings`, `search`
- [typoTolerance](#typotolerance) `settings`, `search`
- [allowTyposOnNumericTokens](#allowtyposonnumerictokens) `settings`, `search`
- [ignorePlurals](#ignoreplurals) `settings`, `search`
- [disableTypoToleranceOnAttributes](#disabletypotoleranceonattributes) `settings`, `search`
- [disableTypoToleranceOnWords](#disabletypotoleranceonwords) `settings`
- [separatorsToIndex](#separatorstoindex) `settings`

**Geo-Search**

- [aroundLatLng](#aroundlatlng) `search`
- [aroundLatLngViaIP](#aroundlatlngviaip) `search`
- [aroundRadius](#aroundradius) `search`
- [aroundPrecision](#aroundprecision) `search`
- [minimumAroundRadius](#minimumaroundradius) `search`
- [insideBoundingBox](#insideboundingbox) `search`
- [insidePolygon](#insidepolygon) `search`

**Query Strategy**

- [queryType](#querytype) `settings`
- [removeWordsIfNoResults](#removewordsifnoresults) `settings`, `search`
- [advancedSyntax](#advancedsyntax) `settings`, `search`
- [optionalWords](#optionalwords) `settings`, `search`
- [removeStopWords](#removestopwords) `settings`, `search`
- [disablePrefixOnAttributes](#disableprefixonattributes) `seetings`
- [disableExactOnAttributes](#disableexactonattributes) `settings`
- [exactOnSingleWordQuery](#exactonsinglewordquery) `settings`, `search`
- [alternativesAsExact](#alternativesasexact) `setting`, `search`

**performance**

- [numericAttributesForFiltering](#numericattributesforfiltering) `settings`
- [allowCompressionOfIntegerArray](#allowcompressionofintegerarray) `settings`

**Advanced**

- [attributeForDistinct](#attributefordistinct) `settings`
- [placeholders](#placeholders) `settings`
- [altCorrections](#altcorrections) `settings`
- [minProximity](#minproximity) `settings`, `search`
- [responseFields](#responsefields) `settings`, `search`
- [distinct](#distinct) `settings`, `search`
- [getRankingInfo](#getrankinginfo) `search`
- [numericFilters](#numericfilters) `search`
- [tagFilters (deprecated)](#tagfilters-deprecated) `search`
- [analytics](#analytics) `search`
- [analyticsTags](#analyticstags) `search`
- [synonyms](#synonyms) `search`
- [replaceSynonymsInHighlight](#replacesynonymsinhighlight) `settings`, `search`

## Search

#### query

- scope: `search`
- type: `string`
- default: ""

The search query string, used to set the string you want to search in your index.
If no query parameter is set, the textual search will match with all the objects.

## Attributes

#### searchableAttributes

- scope: `settings`
- type: `array of strings`
- default: * (all string attributes)
- formerly known as: `attributesToIndex`

The list of attributes you want index (i.e. to make searchable).

If set to null, all textual and numerical attributes of your objects are indexed.
Make sure you updated this setting to get optimal results.

This parameter has two important uses:

1. **Limit the attributes to index.** For example, if you store the URL of a picture, you want to store it and be able to retrieve it,
    but you probably don't want to search in the URL.

2. **Control part of the ranking.** The contents of the `searchableAttributes` parameter impacts ranking in two complementary ways:
    First, the order in which attributes are listed defines their ranking priority: matches in attributes at the beginning of the
    list will be considered more important than matches in attributes further down the list. To assign the same priority to several attributes,
    pass them within the same string, separated by commas. For example, by specifying `["title,"alternative_title", "text"]`,
    `title` and `alternative_title` will have the same priority, but a higher priority than `text`.

    Then, within the same attribute, matches near the beginning of the text will be considered more important than matches near the end.
    You can disable this behavior by wrapping your attribute name inside an `unordered()` modifier. For example, `["title", "unordered(text)"]`
    will consider all positions inside the `text` attribute as equal, but positions inside the `title` attribute will still matter.

    You can decide to have the same priority for several attributes by passing them in the same string using comma as separator.
    For example: \n`title` and `alternative_title` have the same priority in this example: `searchableAttributes:["title,alternative_title", "text"]`

To get a full description of how the ranking works, you can have a look at our [Ranking guide](https://www.algolia.com/doc/guides/relevance/ranking).

#### attributesForFaceting

- scope: `settings`
- type: `array of strings`

The list of attributes you want to use for faceting.
All strings within these attributes will be extracted and added as facets.
If set to `null`, no attribute is used for faceting.

If you only need to filter on a given facet, you can specify filterOnly(attributeName). It reduces the size of the index and the build time.

If you want to search inside values of a given facet (using the [Search for facet values](#search-for-facet-values) method) you need to specify searchable(attributeName).

#### unretrievableAttributes

- scope: `settings`
- type: `array of strings`

The list of attributes that cannot be retrieved at query time.
This feature allows you to have attributes that are used for indexing
and/or ranking but cannot be retrieved.

This setting will be bypassed if the query is done with the ADMIN API key
{.alert .alert-info}

#### attributesToRetrieve

- scope: `settings` `search`
- type: `array of strings`
- default: * (all attributes)

List of attributes you want to retrieve in the search response.
This can be use to minimize the size of the JSON answer.

You can use `*` to retrieve all values.

**Note:** `objectID` is always retrieved, even when not specified.

#### restrictSearchableAttributes

- scope: `search`
- type: `array of strings`
- default: all attributes in searchableAttributes

List of attributes you want to use for textual search.

It must be a subset of the `searchableAttributes` index setting.

SearchableAttributes must not be empty/null to be able to use this parameter.

## Ranking

#### ranking

- scope: `settings`
- type: `array of strings`
- default: ['typo', 'geo', 'words', 'filters', 'proximity', 'attribute', 'exact', 'custom']

Controls the way results are sorted.

We have nine available criterion:

* `typo`: Sort according to number of typos.
* `geo`: Sort according to decreasing distance when performing a geo location based search.
* `words`: Sort according to the number of query words matched by decreasing order. This parameter is useful when you use the `optionalWords` query parameter to have results with the most matched words first.
* `proximity`: Sort according to the proximity of the query words in hits.
* `attribute`: Sort according to the order of attributes defined by searchableAttributes.
* `exact`:
  * If the user query contains one word: sort objects having an attribute that is exactly the query word before others. For example, if you search for the TV show "V", you want to find it with the "V" query and avoid getting all popular TV shows starting by the letter V before it.
  * If the user query contains multiple words: sort according to the number of words that matched exactly (not as a prefix).
* `custom`: Sort according to a user defined formula set in the `customRanking` attribute.
* `asc(attributeName)`: Sort according to a numeric attribute using ascending order. `attributeName` can be the name of any numeric attribute in your records (integer, double or boolean).
* `desc(attributeName)`: Sort according to a numeric attribute using descending order. `attributeName` can be the name of any numeric attribute in your records (integer, double or boolean).

To get a full description of how the Ranking works, you can have a look at our [Ranking guide](https://www.algolia.com/doc/guides/relevance/ranking).

#### customRanking

- scope: `settings`
- type: `array of strings`
- default: []

Lets you specify part of the ranking.

The syntax of this condition is an array of strings containing attributes
prefixed by the asc (ascending order) or desc (descending order) operator.

For example, `"customRanking" => ["desc(population)", "asc(name)"]`.

To get a full description of how the Custom Ranking works,
you can have a look at our [Ranking guide](https://www.algolia.com/doc/guides/relevance/ranking).

#### replicas

- scope: `settings`
- type: `array of strings`
- default: []
- formerly known as: `slaves`

The list of indices on which you want to replicate all write operations.

In order to get response times in milliseconds, we pre-compute part of the ranking during indexing.

If you want to use different ranking configurations depending of the use case,
you need to create one index per ranking configuration.

This option enables you to perform write operations only on this index and automatically
update replica indices with the same operations.

## Filtering / Faceting

#### filters

- scope: `search`
- type: `string`
- default: ""

Filter the query with numeric, facet or/and tag filters.

The syntax is a SQL like syntax, you can use the OR and AND keywords.
The syntax for the underlying numeric, facet and tag filters is the same than in the other filters:

`available=1 AND (category:Book OR NOT category:Ebook) AND _tags:public`
`date: 1441745506 TO 1441755506 AND inStock > 0 AND author:"John Doe"`

The list of keywords is:

- **OR**: create a OR between two filters.
- **AND**: create a AND between two filters.
- **TO**: used to specify a range for a numeric filter.
- **NOT**: used to negate a filter. The syntax with the '-' isn't allowed.

If no attribute name is specified,
the filter applies to `_tags`.
For example: `public OR user_42` will translate to `_tags:public OR _tags:user_42`.

To specify a value with spaces or with a value equal to a keyword, it's possible to add quotes.

Like for the other filter for performance reason, it's not possible to have FILTER1 OR (FILTER2 AND FILTER3).

It's not possible to mix different category of filter inside a OR like num=3 OR tag1 OR facet:value

It's not possible to negate an group, it's only possible to negate a filter:  NOT(FILTER1 OR FILTER2) is not allowed.

#### facets

- scope: `search`
- type: `array of string`
- default: []

You can use [facets](#facets) to retrieve only a part of your attributes declared in
**[attributesForFaceting](#attributesforfaceting)** attributes.

For each of the declared attributes, you'll be able to retrieve a list of the most relevant facet values,
and their associated count for the current query.

It will not filter your results, if you want to filter results you should use [filters](#filters).

**Example**

If you have defined in your **[attributesForFaceting](#attributesforfaceting)**:

```
["category", "author", "nb_views", "nb_downloads"]
```

... but, for the current search, you want to retrieve facet values only for `category` and `author`, then you can specify:

```
["category", "author"]
```

When using [facets](#facets) in a search query, only attributes that have been added in **attributesForFaceting** index setting can be used in this parameter.

You can also use `*` to perform faceting on all attributes specified in `attributesForFaceting`.

If the number of results is important, the count can be approximate, the attribute `exhaustiveFacetsCount` in the response is true when the count is exact.

#### maxValuesPerFacet

- scope: `settings` `search`
- type: `integer`
- default: 100

Limit the number of facet values returned for each facet.

For example, `maxValuesPerFacet=10` will retrieve a maximum of 10 values per facet.

**Warnings**
- The engine has a hard limit on the `maxValuesPerFacet` of `1000`. Any value above that will be interpreted by the engine as being `1000`.

#### facetFilters

- scope: `search`
- type: `array of string`
- default: ""

**Warning**: We introduce the [filters](#filters) parameter that provide a SQL like syntax
and is easier to use for most usecases

Filter the query with a list of facets. A Facet is encoded as key value like this: `attributeName:value`.

`["category:Book","author:John%20Doe"]` will translate to `category:Book` AND `author:John%20Doe`

You can also OR facets: `[["category:Book","category:Movie"],"author:John%20Doe"]`, will translate to
`(category:Book OR category:Movie) AND author:John%20Doe`

## Highlighting / Snippeting

#### attributesToHighlight

- scope: `settings` `search`
- type: `array of string`

List of attributes to highlight.
If set to null, all indexed attributes are highlighted.

An attribute has no match for the query, the raw value is returned.

By default, all indexed attributes are highlighted (as long as they are strings).
You can use `*` if you want to highlight all attributes.

A matchLevel is returned for each highlighted attribute and can contain:

* `full`: If all the query terms were found in the attribute.
* `partial`: If only some of the query terms were found.
* `none`: If none of the query terms were found.

#### attributesToSnippet

- scope: `settings` `search`
- type: `array of strings`
- default: [] (no attribute is snippeted)

List of attributes to snippet alongside the number of words to return (syntax is `attributeName:nbWords`).

#### highlightPreTag

- scope: `settings` `search`
- type: `string`
- default: <em>

Specify the string that is inserted before the highlighted parts in the query result

#### highlightPostTag

- scope: `settings` `search`
- type: `string`
- default: </em>

Specify the string that is inserted after the highlighted parts in the query result.

#### snippetEllipsisText

- scope: `settings` `search`
- type: `string`
- default: ...

String used as an ellipsis indicator when a snippet is truncated.

Defaults to an empty string for all accounts created before 10/2/2016, and to `…` (U+2026) for accounts created after that date.

#### restrictHighlightAndSnippetArrays

- scope: `settings` `search`
- type: `boolean`
- default: false

If set to true, restrict arrays in highlights and snippets to items that matched the query at least partially else return all array items in highlights and snippets.

## Pagination

#### page

- scope: `search`
- type: `integer`
- default: 0

Pagination parameter used to select the page to retrieve.

**Warning:** Page is zero based. Thus, to retrieve the 10th page, you need to set `page=9`.

#### hitsPerPage

- scope: `settings` `search`
- type: `integer`
- default: 20

Pagination parameter used to select the number of hits per page.

#### offset

- scope: `search`
- type: `integer`

Offset of the first hit to return (zero-based).

**Warning:** In most cases, `page`/`hitsPerPage` is the recommended method for pagination.

#### length

- scope: `search`
- type: `integer`

Offset of the first hit to return (zero-based).

**Warning:** In most cases, `page`/`hitsPerPage` is the recommended method for pagination.

#### paginationLimitedTo

- scope: `settings`
- type: `integer`
- default: 1000

Allows to control the maximum number of hits accessible via pagination.
By default, this parameter is limited to 1000 to guarantee good performance.

**Warning:** We recommend to keep the default value to guarantee excellent performance.
Increasing this limit will have a direct impact on the performance of search.
A big value will also make it very easy for anyone to download all your dataset.

## Typos

#### minWordSizefor1Typo

- scope: `settings` `search`
- type: `integer`
- default: 4

The minimum number of characters in the query string needed to accept results matching with one typo.

#### minWordSizefor2Typos

- scope: `settings` `search`
- type: `integer`
- default: 8

The minimum number of characters in the query string needed to accept results matching with two typos.

#### typoTolerance

- scope: `settings` `search`
- type: `string`
- default: true

This option allows you to control the number of typos allowed in the result set:

* `TYPO_TRUE`:
  The typo tolerance is enabled and all matching hits are retrieved (default behavior).

* `TYPO_FALSE`:
  The typo tolerance is disabled. All results with typos will be hidden.

* `TYPO_MIN`:
  Only keep results with the minimum number of typos. For example, if one result matches without typos, then all results with typos will be hidden.

* `TYPO_STRICT`:
  Hits matching with 2 typos or more are not retrieved if there are some matching without typos.
  This option is useful if you want to avoid as much as possible false positive.

#### allowTyposOnNumericTokens

- scope: `settings` `search`
- type: `boolean`
- default: true

If set to false, disable typo-tolerance on numeric tokens (numbers) in the query string.
For example the query `\"304\"` will match with `\"30450\"`, but not with `\"40450\"`
that would have been the case with typo-tolerance enabled.

This option can be very useful on serial numbers and zip codes searches.

#### ignorePlurals

- scope: `settings` `search`
- type: `boolean` `array of strings`
- default: true

Consider singular and plurals forms a match without typo.
For example, car and cars, or foot and feet will be considered equivalent.

This parameter can be:

- a **boolean**: enable or disable plurals for all 59 supported languages.
- a **list of language ISO codes** for which plurals should be enabled.

This option is set to `false` by default.

Here is the list of supported languages:

Afrikaans=`af`, Arabic=`ar`, Azeri=`az`, Bulgarian=`bg`, Catalan=`ca`,
Czech=`cs`, Welsh=`cy`, Danis=`da`, German=`de`, English=`en`,
Esperanto=`eo`, Spanish=`es`, Estonian=`et`, Basque=`eu`, Finnish=`fi`,
Faroese=`fo`, French=`fr`, Galician=`gl`, Hebrew=`he`, Hindi=`hi`,
Hungarian=`hu`, Armenian=`hy`, Indonesian=`id`, Icelandic=`is`, Italian=`it`,
Japanese=`ja`, Georgian=`ka`, Kazakh=`kk`, Korean=`ko`, Kyrgyz=`ky`,
Lithuanian=`lt`, Maori=`mi`, Mongolian=`mn`, Marathi=`mr`, Malay=`ms`,
Maltese=`mt`, Norwegian=`nb`, Dutch=`nl`, Northern Sotho=`ns`, Polish=`pl`,
Pashto=`ps`, Portuguese=`pt`, Quechua=`qu`, Romanian=`ro`, Russian=`ru`,
Slovak=`sk`, Albanian=`sq`, Swedish=`sv`, Swahili=`sw`, Tamil=`ta`,
Telugu=`te`, Tagalog=`tl`, Tswana=`tn`, Turkish=`tr`, Tatar=`tt`,

#### disableTypoToleranceOnAttributes

- scope: `settings` `search`
- type: `array of strings`
- default: []

List of attributes on which you want to disable typo tolerance
(must be a subset of the `searchableAttributes` index setting).

#### disableTypoToleranceOnWords

- scope: `settings`
- type: `array of strings`
- default: []

Specify a list of words on which the automatic typo tolerance will be disabled.

#### separatorsToIndex

- scope: `settings`
- type: `string`
- default: ""

Specify the separators (punctuation characters) to index.

By default, separators are not indexed.

**Example:** Use `+#` to be able to search for "Google+" or "C#".

## Geo-Search

Geo search requires that you provide at least one geo location in each record at indexing time, under the `_geoloc` attribute. Each location must be an object with two numeric `lat` and `lng` attributes. You may specify either one location:

```
{
  "_geoloc": {
    "lat": 48.853409,
    "lng": 2.348800
  }
}
```

... or an array of locations:

```
{
  "_geoloc": [
    {
      "lat": 48.853409,
      "lng": 2.348800
    },
    {
      "lat": 48.547456,
      "lng": 2.972075
    }
  ]
}
```

#### aroundLatLng

- scope: `search`
- type: `string`
- default: ""

Search for entries around a given location (specified as two floats separated by a comma).

For example, `aroundLatLng=47.316669,5.016670`.

- By default the maximum distance is automatically guessed based on the density of the area
  but you can specify it manually in meters with the **aroundRadius** parameter.
  The precision for ranking can be set with **aroundPrecision** parameter.
- If you set aroundPrecision=100, the distances will be considered by ranges of 100m.
- For example all distances 0 and 100m will be considered as identical for the "geo" ranking parameter.

#### aroundLatLngViaIP

- scope: `search`
- type: `boolean`
- default: false

Search for entries around a given latitude/longitude automatically computed from user IP address.

To enable it, use `aroundLatLngViaIP=true`.

You can specify the maximum distance in meters with the `aroundRadius` parameter
and the precision for ranking with `aroundPrecision`.

For example:
- if you set aroundPrecision=100,
two objects that are in the range 0-99m
will be considered as identical in the ranking for the "geo" ranking parameter (same for 100-199, 200-299, ... ranges).

#### aroundRadius

- scope: `search`
- type: `integer` `string`

Control the radius associated with a geo search. Defined in meters.

If not set, the radius is computed automatically using the density of the area.
You can retrieve the computed radius in the `automaticRadius` attribute of the response.
You can also specify a minimum value for the automatic radius by using the `minimumAroundRadius` query parameter.

You can specify `aroundRadius=all` if you want to compute the geo distance without filtering in a geo area;
this option will be faster than specifying a big integer value.

#### aroundPrecision

- scope: `search`
- type: `integer`

Control the precision of a geo search.
Defined in meters.

For example, if you set `aroundPrecision=100`, two objects that are in the range 0-99m will be considered as
identical in the ranking for the `geo` ranking parameter (same for 100-199, 200-299, … ranges).

#### minimumAroundRadius

- scope: `search`
- type: `integer`

Define the minimum radius used for a geo search when `aroundRadius` is not set.
The radius is computed automatically using the density of the area.
You can retrieve the computed radius in the `automaticRadius` attribute of the answer.

#### insideBoundingBox

- scope: `search`
- type: `string`

Search entries inside a given area defined by the two extreme points of a rectangle
(defined by 4 floats: p1Lat,p1Lng,p2Lat,p2Lng).

For example:

- `insideBoundingBox=47.3165,4.9665,47.3424,5.0201`

You can use several bounding boxes (OR) by passing more than 4 values.
For example: instead of having 4 values you can pass 8 to search inside the UNION of two bounding boxes.

#### insidePolygon

- scope: `search`
- type: `string`
- default: ""

Search entries inside a given area defined by a set of points
  (defined by a minimum of 6 floats: p1Lat,p1Lng,p2Lat,p2Lng,p3Lat,p3Long).

  For example:
  
  - `InsidePolygon=47.3165,4.9665,47.3424,5.0201,47.32,4.98`
  

## Query Strategy

#### queryType

- scope: `settings`
- type: `string`
- default: prefixLast

Selects how the query words are interpreted. It can be one of the following values:

* `PREFIX_LAST`:
  Only the last word is interpreted as a prefix (default behavior).

* `PREFIX_ALL`:
  All query words are interpreted as prefixes. This option is not recommended.

* `PREFIX_NONE`:
  No query word is interpreted as a prefix. This option is not recommended.

#### removeWordsIfNoResults

- scope: `settings` `search`
- type: `string`
- default: none

This option is used to select a removing of words strategy when the query doesn't retrieve any results.
It can be used to avoid having an empty result page

There are four different options:

- `REMOVE_NONE`:
  No specific processing is done when a query does not return any results (default behavior).

- `REMOVE_LAST_WORDS`:
  When a query does not return any results, the last word will be added as optional.
  The process is repeated with n-1 word, n-2 word, ... until there are results.

- `REMOVE_FIRST_WORDS`:
  When a query does not return any results, the first word will be added as optional.
  The process is repeated with second word, third word, ... until there are results.
- `REMOVE_ALL_OPTIONAL`:
  When a query does not return any results, a second trial will be made with all words as optional.
  This is equivalent to transforming the AND operand between query terms to an OR operand.

#### advancedSyntax

- scope: `settings` `search`
- type: `boolean`
- default: false

Enables the advanced query syntax.

This syntax allow to do two things:

- **Phrase query**: A phrase query defines a particular sequence of terms. A phrase query needs to be surrounded by `"`.
  For example, `"search engine"` will retrieve records having `search` next to `engine` only.

  Typo tolerance is disabled inside the phrase (inside the `"`).
  

- **Prohibit operator**: The prohibit operator excludes records that contain the term after the `-` symbol.
  For example, `search -engine` will retrieve records containing `search` but not `engine`.

#### optionalWords

- scope: `settings` `search`
- type: `string` `array of string`
- default: ""

The list of words that should be considered as optional when found in the query.

If you use the specify the optionnal words with a string you don't need to put coma in between words.
The engine will tokenize the string into words that will all be considered as optional.

#### removeStopWords

- scope: `settings` `search`
- type: `boolean` `array of string`
- default: false

Remove stop words from the query **before** executing it. It can be:

- a **boolean**: enable or disable stop words for all 41 supported languages; or
- a **list of language ISO codes** (as a comma-separated string) for which stop words should be enabled.

In most use-cases, **we don’t recommend enabling this option**.

List of 41 supported languages with their associated iso code: Arabic=`ar`, Armenian=`hy`, Basque=`eu`, Bengali=`bn`, Brazilian=`pt-br`, Bulgarian=`bg`, Catalan=`ca`, Chinese=`zh`, Czech=`cs`, Danish=`da`, Dutch=`nl`, English=`en`, Finnish=`fi`, French=`fr`, Galician=`gl`, German=`de`, Greek=`el`, Hindi=`hi`, Hungarian=`hu`, Indonesian=`id`, Irish=`ga`, Italian=`it`, Japanese=`ja`, Korean=`ko`, Kurdish=`ku`, Latvian=`lv`, Lithuanian=`lt`, Marathi=`mr`, Norwegian=`no`, Persian (Farsi)=`fa`, Polish=`pl`, Portugese=`pt`, Romanian=`ro`, Russian=`ru`, Slovak=`sk`, Spanish=`es`, Swedish=`sv`, Thai=`th`, Turkish=`tr`, Ukranian=`uk`, Urdu=`ur`.

Stop words removal is applied on query words that are not interpreted as a prefix. The behavior depends of the `queryType` parameter:

* `queryType=prefixLast` means the last query word is a prefix and it won’t be considered for stop words removal
* `queryType=prefixNone` means no query word are prefix, stop words removal will be applied on all query words
* `queryType=prefixAll` means all query terms are prefix, stop words won’t be removed

This parameter is useful when you have a query in natural language like “what is a record?”.
In this case, before executing the query, we will remove “what”, “is” and “a” in order to just search for “record”.
This removal will remove false positive because of stop words, especially when combined with optional words.
For most use cases, it is better to not use this feature as people search by keywords on search engines.

#### disablePrefixOnAttributes

- scope: `seetings`
- type: `array of strings`
- default: []

List of attributes on which you want to disable prefix matching
(must be a subset of the `searchableAttributes` index setting).

This setting is useful on attributes that contain string that should not be matched as a prefix
(for example a product SKU).

#### disableExactOnAttributes

- scope: `settings`
- type: `search`
- default: []

List of attributes on which you want to disable the computation of `exact` criteria
(must be a subset of the `searchableAttributes` index setting).

#### exactOnSingleWordQuery

- scope: `settings` `search`
- type: `string`
- default: attribute

This parameter control how the `exact` ranking criterion is computed when the query contains one word. There are three different values:

* `none`: no exact on single word query
* `word`: exact set to 1 if the query word is found in the record. The query word needs to have at least 3 chars and not be part of our stop words dictionary
* `attribute` (default): exact set to 1 if there is an attribute containing a string equals to the query

#### alternativesAsExact

- scope: `setting` `search`
- type: `array of string`
- default: ['ignorePlurals', 'singleWordSynonym']

Specify the list of approximation that should be considered as an exact match in the ranking formula:

* `ignorePlurals`: alternative words added by the ignorePlurals feature
* `singleWordSynonym`: single-word synonym (For example "NY" = "NYC")
* `multiWordsSynonym`: multiple-words synonym (For example "NY" = "New York")

## performance

#### numericAttributesForFiltering

- scope: `settings`
- type: `array of strings`
- default: []
- formerly known as: `numericAttributesToIndex`

All numerical attributes are automatically indexed as numerical filters
(allowing filtering operations like `<` and `<=`).
If you don't need filtering on some of your numerical attributes,
you can specify this list to speed up the indexing.

If you only need to filter on a numeric value with the operator `=` or `!=`,
you can speed up the indexing by specifying the attribute with `equalOnly(AttributeName)`.
The other operators will be disabled.

#### allowCompressionOfIntegerArray

- scope: `settings`
- type: `boolean`
- default: false

Allows compression of big integer arrays.

In data-intensive use-cases,
we recommended enabling this feature and then storing the list of user IDs or rights as an integer array.
When enabled, the integer array is reordered to reach a better compression ratio.

## Advanced

#### attributeForDistinct

- scope: `settings`
- type: `string`

The name of the attribute used for the `Distinct` feature.

This feature is similar to the SQL "distinct" keyword.
When enabled in queries with the `distinct=1` parameter,
all hits containing a duplicate value for this attribute are removed from the results.

For example, if the chosen attribute is `show_name` and several hits have the same value for `show_name`,
then only the first one is kept and the others are removed from the results.

To get a full understanding of how `Distinct` works,
you can have a look at our [guide on distinct](https://www.algolia.com/doc/search/distinct).

#### placeholders

- scope: `settings`
- type: `hash of array of words`
- default: ""

This is an advanced use-case to define a token substitutable by a list of words
without having the original token searchable.

It is defined by a hash associating placeholders to lists of substitutable words.

For example, `"placeholders": { "<streetnumber>": ["1", "2", "3", ..., "9999"]}`
would allow it to be able to match all street numbers. We use the `< >` tag syntax
to define placeholders in an attribute.

For example:

* Push a record with the placeholder:
`{ "name" : "Apple Store", "address" : "&lt;streetnumber&gt; Opera street, Paris" }`.
* Configure the placeholder in your index settings:
`"placeholders": { "<streetnumber>" : ["1", "2", "3", "4", "5", ... ], ... }`.

#### altCorrections

- scope: `settings`
- type: `array of objects`
- default: []

Specify alternative corrections that you want to consider.

Each alternative correction is described by an object containing three attributes:

* `word` (string): The word to correct.
* `correction` (string): The corrected word.
* `nbTypos` (integer): The number of typos (1 or 2) that will be considered for the ranking algorithm (1 typo is better than 2 typos).

For example:

```
"altCorrections": [
  { "word" : "foot", "correction": "feet", "nbTypos": 1 },
  { "word": "feet", "correction": "foot", "nbTypos": 1 }
]
```

#### minProximity

- scope: `settings` `search`
- type: `integer`
- default: 1

Configure the precision of the `proximity` ranking criterion.
By default, the minimum (and best) proximity value distance between 2 matching words is 1.

Setting it to 2 (or 3) would allow 1 (or 2) words to be found between the matching words without degrading the proximity ranking value.

Considering the query *“javascript framework”*, if you set `minProximity=2`, the records *“JavaScript framework”* and *“JavaScript charting framework”*
will get the same proximity score, even if the second contains a word between the two matching words.

**Note:** the maximum `minProximity` that can be set is 7. Any higher value will disable the `proximity` criterion from the ranking formula.

#### responseFields

- scope: `settings` `search`
- type: `array of strings`
- default: * (all fields)

Choose which fields the response will contain. Applies to search and browse queries.

By default, all fields are returned. If this parameter is specified, only the fields explicitly
listed will be returned, unless `*` is used, in which case all fields are returned.
Specifying an empty list or unknown field names is an error.

This parameter is mainly intended to limit the response size.
For example, for complex queries, echoing of request parameters in the response's `params` field can be undesirable.

Here is the list of field that can be filtered:

- `aroundLatLng`
- `automaticRadius`
- `exhaustiveFacetsCount`
- `facets`
- `facets_stats`
- `hits`
- `hitsPerPage`
- `index`
- `length`
- `nbHits`
- `nbPages`
- `offset`
- `page`
- `params`
- `processingTimeMS`
- `query`
- `queryAfterRemoval`

Here is the list of fields cannot be filtered out:

- `message`
- `warning`
- `cursor`
- `serverUsed`
- `timeoutCounts`
- `timeoutHits`
- `parsedQuery`
- fields triggered explicitly via [getRankingInfo](#getrankinginfo)

#### distinct

- scope: `settings` `search`
- type: `boolean`
- default: 0

If set to YES it
enables the distinct feature, disabled by default, if the `attributeForDistinct` index setting is set.

This feature is similar to the SQL "distinct" keyword.
When enabled in a query with the `distinct=1` parameter,
all hits containing a duplicate value for the attributeForDistinct attribute are removed from results.

For example, if the chosen attribute is `show_name` and several hits have the same value for `show_name`,
then only the best one is kept and the others are removed.

To get a full understanding of how `Distinct` works,
you can have a look at our [guide on distinct](https://www.algolia.com/doc/search/distinct).

#### getRankingInfo

- scope: `search`
- type: `boolean`
- default: false

If set to YES,
the result hits will contain ranking information in the **_rankingInfo** attribute.

#### numericFilters

- scope: `search`
- type: `array of strings`
- default: []

*If you are not using this parameter to generate filters programatically you should use [filters](#filters) instead*

List of numeric filters you want to apply.
The filter syntax is `attributeName` followed by `operand` followed by `value`.
Supported operands are `<`, `<=`, `=`, `>` and `>=`.

You can easily perform range queries via the `:` operator.
This is equivalent to combining a `>=` and `<=` operand.
For example, `numericFilters=price:10 to 1000`.

You can also mix OR and AND operators.
The OR operator is defined with a parenthesis syntax.
For example, `code=1 AND (price:[0-100] OR price:[1000-2000])`
translates to `code=1,(price:0 to 100,price:1000 to 2000)`.

#### tagFilters (deprecated)

- scope: `search`
- type: `array of string`
- default: ""

**This parameter is deprecated. You should use [filters](#filters) instead.**

Filter the query by a set of tags.

You can AND tags by separating them with commas.
To OR tags, you must add parentheses.

For example: `["tag1",["tag2","tag3"]]` means `tag1 AND (tag2 OR tag3)`.

Negations are supported via the `-` operator, prefixing the value.

For example: `["tag1", "-tag2"]`.

At indexing, tags should be added in the **_tags** attribute of objects.

For example `{"_tags":["tag1","tag2"]}`.

#### analytics

- scope: `search`
- type: `boolean`
- default: true

If set to false, this query will not be taken into account in the analytics.

#### analyticsTags

- scope: `search`
- type: `array of strings`

If set, tag your query with the specified identifiers. Tags can then be used in the Analytics to analyze a subset of searches only.

#### synonyms

- scope: `search`
- type: `boolean`
- default: true

If set to `false`, the search will not use the synonyms defined for the targeted index.

#### replaceSynonymsInHighlight

- scope: `settings` `search`
- type: `boolean`
- default: true

If set to `false`, words matched via synonyms expansion will not be replaced by the matched synonym in the highlighted result.


# Manage Indices



## Create an index

To create an index, you need to perform any indexing operation like:
- set settings
- add object

## List indices - `listIndexesAsync` 

You can list all your indices along with their associated information (number of entries, disk size, etc.) with the `listIndexesAsync` method:

```java
client.listIndexesAsync(new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject content, AlgoliaException error) {
        // [...]
    }
});
```


# Advanced



## Custom batch - `batchAsync` 

You may want to perform multiple operations with one API call to reduce latency.

If you have one index per user, you may want to perform a batch operations across several indices.
We expose a method to perform this type of batch:

```java
List<JSONObject> array = new ArrayList<>();
array.add(new JSONObject()
  .put("action", "addObject")
  .put("indexName", "index1")
  .put("body", new JSONObject()
      .put("firstname", "Jimmie")
      .put("lastname", "Barninger")
  )
);
array.add(new JSONObject()
  .put("action", "addObject")
  .put("indexName", "index2")
  .put("body", new JSONObject()
      .put("firstname", "Warren")
      .put("lastname", "Speach")
  )
);
client.batchAsync(new JSONArray(array), null);
```

The attribute **action** can have these values:

- addObject
- updateObject
- partialUpdateObject
- partialUpdateObjectNoCreate
- deleteObject

## Backup / Export an index - `browseAsync` 

The `search` method cannot return more than 1,000 results. If you need to
retrieve all the content of your index (for backup, SEO purposes or for running
a script on it), you should use the `browse` method instead. This method lets
you retrieve objects beyond the 1,000 limit.

This method is optimized for speed. To make it fast, distinct, typo-tolerance,
word proximity, geo distance and number of matched words are disabled. Results
are still returned ranked by attributes and custom ranking.

#### Response Format

##### Sample

```json
{
  "hits": [
    {
      "firstname": "Jimmie",
      "lastname": "Barninger",
      "objectID": "433"
    }
  ],
  "processingTimeMS": 7,
  "query": "",
  "params": "filters=level%3D20",
  "cursor": "ARJmaWx0ZXJzPWxldmVsJTNEMjABARoGODA4OTIzvwgAgICAgICAgICAAQ=="
}
```

##### Fields

- `cursor` (string, optional): A cursor to retrieve the next chunk of data. If absent, it means that the end of the index has been reached.
- `query` (string): Query text used to filter the results.
- `params` (string, URL-encoded): Search parameters used to filter the results.
- `processingTimeMS` (integer): Time that the server took to process the request, in milliseconds. *Note: This does not include network time.*

The following fields are provided for convenience purposes, and **only when the browse is not filtered**:

- `nbHits` (integer): Number of objects in the index.
- `page` (integer): Index of the current page (zero-based).
- `hitsPerPage` (integer): Maximum number of hits returned per page.
- `nbPages` (integer): Number of pages corresponding to the number of hits. Basically, `ceil(nbHits / hitsPerPage)`.

#### Example

Using the low-level methods:

```java
index.browseAsync(query, new CompletionHandler() {
    @Override
    public void requestCompleted(JSONObject result, AlgoliaException error) {
        if (error != null) return;
        // Handle the content. [...]
        // If there is more content, continue browse.
        String cursor = result.optString("cursor", null);
        if (cursor != null) {
            index.browseFrom(cursor, new CompletionHandler() {
                @Override
                public void requestCompleted(JSONObject result, AlgoliaException error) {
                    // Handle more content. [...]
                }
            });
        }
    }
});
```

Using the browse helper:

```java
BrowseIterator iterator = new BrowseIterator(index, query, new BrowseIterator.BrowseIteratorHandler() {
    @Override
    public void handleBatch(@NonNull BrowseIterator iterator, JSONObject result, AlgoliaException error) {
        // Handle the result/error. [...]
        // You may optionally cancel the iteration by calling:
        iterator.cancel();
    }
});
iterator.start();
```

## REST API

We've developed API clients for the most common programming languages and platforms.
These clients are advanced wrappers on top of our REST API itself and have been made
in order to help you integrating the service within your apps:
for both indexing and search.

Everything that can be done using the REST API can be done using those clients.

The REST API lets your interact directly with Algolia platforms from anything that can send an HTTP request
[Go to the REST API doc](https://algolia.com/doc/rest)


