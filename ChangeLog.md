Change Log
==========

## 3.10.0 (2017-04-04)

### New features

- Support `percentileCalculation` query parameter
- Support `length` and `offset` query parameter
- Support `disableExactOnAttributes` query parameter
- Support `restrictHighlightAndSnippetArrays` query parameter
- Support `percentileCalculation` query parameter

### Other changes

- Expose a getSettings overload using a default format version

## 3.9.0 (2017-02-08)

### New features

- Support `facetingAfterDistinct` query parameter
- Support `maxFacetHits` parameter when searching for facet values
- [offline] Support offline search for facet values

### Other changes

- Add `Async` suffixed aliases to `searchForFacetValues(...)`


## 3.8.0 (2016-12-29)

**This release includes backward-incompatible changes**:
- (#229) `getInsidePolygon()` now returns a `Query.Polygon` array

### New features
- (#229) `insidePolygon` now accepts one or several polygons
- [offline] Support manual building of local indices (`MirroredIndex` and `OfflineIndex`)
- [offline] Support fallback logic when getting individual objects

### Bug fixes
- Fix filtering by attribute in `getObjectAsync` and `getObjectsAsync`

### Other changes
- (#202) Mark `{get,set}NumericFilters` as deprecated, you should use `{get,set}Filters`
- (#202) Add Nullility annotations on Query methods

## 3.7.1 (2016-12-08)

- Improved retry logic in case of down host

## 3.7.0 (2016-11-29)

**This release includes backward-incompatible changes and requires the following updates**:
- (#178) `getIgnorePlurals()` should be replaced by `getIgnorePlurals().enabled`

### New features
- (#163) Search in your facets values with `Index.searchForFacetValues`
- (#178) Ignore plurals _only for some languages_: `Query.setIgnorePlurals` can take a list of language ISO codes for ignorePlurals.
- (#183) Whitelist the fields returned in the response for a search query with `Query.setResponseFields`

### Other changes
- (#182) Deprecate `setFacetFilters`: you should use `setFilters` instead.

## 3.6.0 (2016-11-10)

### New features

- (#175) Add support for [Algolia Places](https://community.algolia.com/places/)


## 3.5.0 (2016-10-03)

### New features

- Support the `createIfNoExists` parameter in partial updates
- [offline] Offline-only indices

### Bug fixes

- (#157) Use new format version when retrieving settings

### Other changes

- Rename "master"/"slave" to "primary"/"replica"
- Rename `attributesToIndex` to `searchableAttributes`
- Upgrade to Android API level 24. **Note:** As a consequence, the **minimum supported SDK version** is now 10 (was 7 previously).
- Upgrade to Build Tools 24.0.3
- Upgrade to Oracle JDK 8 in Travis

## 3.4.2 (2016-09-15)

- New `Client.getIndex()` method to automatically share `Index` instances with same name
- [Offline mode] Upgrade to Offline Core 1.0

## 3.4.1 (2016-09-08)

- Documentation improvements
- Fix getObjects missing a way to specify attributesToRetrieve
- Expose parseLatLng method

## 3.4.0 (2016-09-05)

- Implement new user-agent convention
- Fix propagation of exhaustiveFacetsCount on aggregated disjunctive faceting queries

## 3.3.0 (2016-07-28)

- removeStopWords can be either a list of language codes or a boolean, see [documentation](https://www.algolia.com/doc/rest-api/search#param-removeStopWords-2).
- Implement new exact API: see [documentation](https://www.algolia.com/doc/rest-api/search#param-exactOnSingleWordQuery) for the new `exactOnSingleWordQuery` and `alternativesAsExact` parameters.
- Remove app_name string resource (fixes #104)
- New `Index.multipleQueriesAsync()` method (convenience shortcut to `Client.multipleQueriesAsync()`)

The following changes impact the offline mode only:

- Require an Android `Context` to be provided during client construction. **Warning: breaking change.**
- Offline requests now also work for disjunctive faceting
- New offline fallback strategy. **Warning: breaking change:** preemptive search no longer supported.
- Improve detection of non-existent indices
- Improve error handling
- Fix crash on concurrent accesses to a local index
- Fix potential race condition in lazy instantiation of the local index


## 3.2.2 (2016-06-06)

- Better resource handling: close client streams and connection when appropriate
- Remove any dependency to Apache HTTP cliuent
- Update documentation: detailed needed ACLs for offline mode

## 3.2.1 (2016-05-31)

- Fix erroneous archive: remove extraneous `android.support.v7.appcompat.R` class (fixes #88)
- Improve release script
- Update documentation


## 3.2 (2016-05-26)

- Network requests are now run in parallel, whereas they were sequential before. This is required for better performance. **Warning: Client code must make sure to protect against out-of-order requests.**
- Better error reporting:
    - Always propagate cause exception when available
    - Always report HTTP status code when available
    - Add ability to distinguish transient errors from fatal errors after they are reported
- Update documentation
    - README updated
    - Clarify documentation of `AlgoliaException`’s status code
- [Test] Add test case for DNS time-out

### Experimental features

- Offline mode. *Note: requires the Algolia Search Offline Core library.* **Warning: beta version.**


## 3.1 (2016-05-09)

- Add typed accessors for missing query parameters `filters` and `numericFilters`
- Shuffle host array at init time, for better load balancing
- Perform stricter check on GZip `Content-Encoding` HTTP header
- Update documentation


## 3.0 (2016-04-14)

This major version brings new features as well as bug fixes. In addition, a lot of refactoring has been performed
to achieve better maintainability and to align the Android client with the other Algolia API clients.

As a consequence, the public interface has changed in an incompatible way. Please refer to our
[Migration guide](https://github.com/algolia/algoliasearch-client-android/wiki/Migration-guide-to-version-3.x) for
detailed instructions.

### New features

- In-memory search cache: disabled by default, can be enabled per index.
- Allow arbitrary query parameters to be specified: the `Query` class provides low-level, untyped accessors in addition
  to the higher-level, typed accessors.
- Allow arbitrary HTTP headers to be specified (`Client.setHeader`)
- Asynchronous requests are cancellable: asynchronous methods return a `Request` instance with a `cancel` method.
- Low-level browse methods (`Index.browse` and `Index.browseFrom`)
- New browse helper (`BrowseIterator`)
- New method to clear an index (`Index.clearIndexAsync`)

### Changes

- Now only one type of asynchronous listener (`CompletionHandler`) with an interface containing only one method,
  used for both success and error cases. In addition to simplifying the code and aligning the Android client with other
  languages, this makes it possible to use Java 8 **lambda expressions**.
- Remove operations requiring an admin API key. (The admin key should *never* be used on the client side.)
- Align `Query` class parameters with the [REST API](https://www.algolia.com/doc/rest)
- Improve the type safety of many `Query` parameters, especially the geo search-related parameters and the filters
- Remove obsolete (and unused) `enableDsn` and `dsnHost` initialization parameters
- Remove accessors to deprecated HTTP headers
- Space character is now URL-encoded as `%20` instead of `+`
- Better naming convention:
    - Rename `APIClient` class to `Client`
    - Rename `*ASync` to `*Async`
    - Remove prefix on enum values

### Fixes

- Search queries now use POST instead of GET, working around any potential hard-coded limit to the URL string.
- Delete by query uses browse instead of search, for both better performance and to work around any index settings
  (like `distinct`) that would silently prevent the query from selecting all objects.
- More consistent error handling
- Fix retry logic in case of host failure
- Alleviate critical section on internal request methods

### Misc. improvements

- Use `@NonNull` annotations to document mandatory parameters and return values
- Asynchronous methods no longer throw (declared) exceptions
- Update documentation (a lot!)
- Add test cases

## 2.6.1 (2016-01-28)

- Add `snippetEllipsisText` query parameter

## 2.6.0 (2016-01-07)

- Add disjunctive faceting method

## 2.5.1 (2016-01-07)

- Fixed method used to generate the query parameters of multiqueries

## 2.5.0 (2015-12-01)

- Added support of Android SDK >= 14

## 2.4.0 (2015-10-12)

- Added remove stop words query parameter
- Added support of similar queries

## 2.3.0 (2015-10-01)

- Added support of multiple bounding box for geo-search
- Added support	of polygon for geo-search
- Added	support	of automatic radius computation	for geo-search
- Added	support	of disableTypoToleranceOnAttributes

## 2.2.0 (2015-09-29)

- Ensure all requests accept the GZIP encoding to reduce the JSON payloads size

## 2.1.0 (2015-08-24)

- Publish on MavenCentral

## 2.0.0 (2015-08-01)

- Rewrite the API Client as a Android package
- Split the async listeners/interfaces

1.6.7 (2015-07-14)
------------------

- Added support of grouping (`distinct=3` to keep the 3 best hits for a distinct key)

1.6.6 (2015-06-05)
--------------------

- add new parameter on the Query: `setMinProximity` & `setHighlightingTags`
- new cursor-based browse implementation

1.6.5 (2015-05-26)
------------------

- Fix thread concurrency for method `_request`

1.6.4 (2015-05-04)
------------------

- Add new methods to add/update api key
- Add batch method to target multiple indices
- Add strategy parameter for the multipleQueries
- Add new method to generate secured api key from query parameters
- Add missing async methods

1.6.3 (2015-04-09)
------------------

- Better retry strategy using two different provider (Improve high-availability of the solution, retry is done on algolianet.com)
- Read operations are performed to APPID-dsn.algolia.net domain first to leverage Distributed Search Network (select the closest location)
- Improved timeout strategy: increasse timeout after 2 trials & have a different read timeout for search operations

1.6.2 (2015-02-18)
------------------

- Added support of AllOptional in removeWordsIfNoResult

1.6.1 (2015-02-12)
------------------

- Add missing getters.
- Add new typoTolerance parameter values.

1.6.0 (2014-12-01)
------------------

- Bump to 1.6.0. [Xavier Grand]

- Switch missing .io and add more debug. [Xavier Grand]

- Add stacktrack in travis. [Xavier Grand]

- Increase sleep. [Xavier Grand]

- Switch to .net. [Xavier Grand]

1.5.11 (2014-11-07)
-------------------

- Bump to 1.5.11. [Xavier Grand]

- Increase the delay for api key modifications. [Xavier Grand]

- Add DSN flag. [Xavier Grand]

1.5.10 (2014-10-22)
-------------------

- Bump to 1.5.10. [Xavier Grand]

- Add more information when hosts are unreachable. [Xavier Grand]

1.5.9 (2014-10-18)
------------------

- Temporary workarounded version in user agent. [Julien Lemoine]

- Workaround to fix Java code which is not valid under android. [Julien
  Lemoine]

- Add setExtraHeader. [Xavier Grand]

1.5.8 (2014-10-11)
------------------

- Bump to 1.5.8. [Sylvain UTARD]

- Embed the API client version in the User-Agent. [Sylvain UTARD]

- 1.5.7 release. [Sylvain UTARD]

1.5.7 (2014-09-21)
------------------

- Bump to 1.5.7. [Sylvain UTARD]

- Missing facetFilters setter. [Sylvain UTARD]

1.5.6 (2014-09-14)
------------------

- Version 1.5.6. [Julien Lemoine]

- Updated default typoTolerance setting & updated removedWordsIfNoResult
  documentation Add the documentation about the update of an APIKey.
  [Xavier Grand]

- Add update acl. [Xavier Grand]

- Updated default typoTolerance setting & updated removedWordsIfNoResult
  documentation. [Julien Lemoine]

- Reduce the sleep of waitTask from 1s to 100ms. [Xavier Grand]

- Add documentation about removeWordsIfNoResult. [Xavier Grand]

1.5.5 (2014-08-13)
------------------

- Version 1.5.5  - new prototype for removeWordsIfNoResult. [Julien
  Lemoine]

1.5.4 (2014-08-13)
------------------

- Android client version 1.5.4 with support of removeLastWordsIfNoResult
  & removeFirstWordsIfNoResult. [Julien Lemoine]

- Fixed doc. [Julien Lemoine]

- Fixed links. [Julien Lemoine]

- Version 1.5.3. [Julien Lemoine]

- Added aroundLatLngViaIP documentation. [Julien Lemoine]

1.5.3 (2014-08-04)
------------------

- Merge branch 'master' of https://github.com/algolia/algoliasearch-
  client-android. [Julien Lemoine]

- Added aroundLatitudeLongitudeViaIP. [Julien Lemoine]

- Added documentation of suffix/prefix index name matching in API key.
  [Julien Lemoine]

- Change the cluster. [Xavier Grand]

- Added restrictSearchableAttributes. [Julien Lemoine]

- Fixed typo. [Julien Lemoine]

1.5.2 (2014-07-24)
------------------

- Version 1.5.2. [Julien Lemoine]

- Version 1.5.2 : [Julien Lemoine]

- Added restrictSearchableAttributes. [Julien Lemoine]

1.5.1 (2014-07-24)
------------------

- 1.5.1 release. [Sylvain UTARD]

- Bump to 1.5.1. [Sylvain UTARD]

- Add missing setSecurityTags/setUserToken methods. [Sylvain UTARD]

- Fix synonyms and replace synonyms. [Xavier Grand]

- Documentation: Added deleteByQuery and multipleQueries. [Xavier Grand]

1.5.0 (2014-07-18)
------------------

- Bump to 1.5.0. [Sylvain UTARD]

- Missing deleteByQueryASync. [Sylvain UTARD]

- Increase waiting time of test14. [Xavier Grand]

- Trying to fix travis. [Xavier Grand]

- Update hmac value in the test. [Xavier Grand]

- Increase waiting time. [Xavier Grand]

- Rename function to contains. [Xavier Grand]

- Add test multipleQueries. [Xavier Grand]

- Update test Hmac. [Xavier Grand]

- Add test getLogs. [Xavier Grand]

- Fix flag analytics in Query.java. [Xavier Grand]

- Add multipleQueries. [Xavier Grand]

- Update generateSecuredApiKey. [Xavier Grand]

- Add missing getLogs method. [Xavier Grand]

- Rename indexName to encodedIndexName Rename originalIndexName to
  indexName. [Xavier Grand]

1.4.8 (2014-07-17)
------------------

- Fixed getObjects method Version 1.4.8 released. [Julien Lemoine]

- Fixed UT build (added encoding UTF8 to java command line) Added
  getObjects Added getObjectsASync Added deleteByQuery. [Julien Lemoine]

- Merge pull request #1 from kepae/master. [Xavier Grand]

  fix typo ("flot" -> "float")

- Added disableTypoToleranceOn & altCorrections index settings. [Julien
  Lemoine]

- Fixed waitTask snippet. [Sylvain UTARD]

- Add typoTolerance & allowsTyposOnNumericTokens query parameters.
  [Sylvain UTARD]

1.4.7 (2014-06-09)
------------------

- Bump to 1.4.7. [Sylvain UTARD]

- Add typoTolerance + allowTyposOnNumericTokens query parameters
  handling. [Sylvain UTARD]

- Documentation: Added words ranking parameter. [Julien Lemoine]

- Added asc(attributeName) & desc(attributeName) documentation in index
  settings. [Julien Lemoine]

1.4.6 (2014-05-21)
------------------

- Version 1.4.6 released. [Julien Lemoine]

- AddUserKey now support of List<String> instead of String for target
  indexes. [Julien Lemoine]

- Fix UTs. [Xavier Grand]

- Updated synonyms examples Add the note about distinct and empty
  queries. [Xavier Grand]

- Fix typo. [Xavier Grand]

- Add a note about distinct. [Xavier Grand]

- Version 1.4.5. [Julien Lemoine]

1.4.5 (2014-05-02)
------------------

- Added support of enableAnalytics, enableSynonyms &
  enableReplaceSynonymsInHighlight in Query class. [Julien Lemoine]

- Added analytics,synonyms,enableSynonymsInHighlight query parameters.
  [Julien Lemoine]

- New numericFilters documentation. [Julien Lemoine]

1.4.4 (2014-04-01)
------------------

- Bump to 1.4.4. [Sylvain UTARD]

- Improved API errors handling. [Sylvain UTARD]

  Conflicts:
  src/main/java/com/algolia/search/saas/APIClient.java

- S/setAvancedSyntax/enableAdvancedSyntax/ [Sylvain UTARD]

- Add advancedSyntax query parameter. [Sylvain UTARD]

- Updated README. [Sylvain UTARD]

- 1.4.3 release. [Sylvain UTARD]

- Bump to 1.4.3. [Sylvain UTARD]

- Ability to generate secured API keys + specify list of indexes
  targeted by user keys. [Sylvain UTARD]

  Conflicts:         README.md
  src/main/java/com/algolia/search/saas/APIClient.java

- Add deleteObjects. [Xavier Grand]

- Cosmetics. [Sylvain UTARD]

- Add badges. [Sylvain UTARD]

- Trying travis. [Sylvain UTARD]

- Copy java tests. [Sylvain UTARD]

- Added batch function. [Xavier Grand]

  Conflicts:         src/main/java/com/algolia/search/saas/Index.java

- Add maxNumberOfFacets query parameter. [Sylvain UTARD]

  Conflicts:         src/main/java/com/algolia/search/saas/Query.java

- Updated README. [Sylvain UTARD]

- S/setNbHitsPerPage/setHitsPerPage/ [Sylvain UTARD]

- Fixed typos (using automatic doc generator) Added slaves index setting
  documentation. [Julien Lemoine]

1.4.2 (2014-01-02)
------------------

- Added 1.4.2 jar. [Julien Lemoine]

-  version 1.4.2. [Julien Lemoine]

- Added support of distinct feature. [Julien Lemoine]

- Improved readability of search & settings parameters. [Julien Lemoine]

1.4.1 (2013-12-06)
------------------

- Added 1.4.1 jar. [Julien Lemoine]

- 1.4.1 version released Added partialUpdateObjects method Added browse
  method & ACL. [Julien Lemoine]

1.4.0 (2013-11-08)
------------------

- Version 1.4.0 released. [Julien Lemoine]

- Fixed typos about setFacetFilters, setNumericFilters & setTagFilters.
  [Julien Lemoine]

1.3.0 (2013-11-07)
------------------

- Version 1.3.0. [Julien Lemoine]

- Documented new features. [Julien Lemoine]

- Set HTTP timeout to 30sec. [Sylvain UTARD]

- Keep it DRY, amend a902212. [Sylvain UTARD]

- Consume the response when an error occurs. [Sylvain UTARD]

- Improve move & copy doc. [Nicolas Dessaigne]

1.2.0 (2013-10-19)
------------------

- Version 1.2.0 released. [Julien Lemoine]

- Improved setup doc Bumped gradle. [Julien Lemoine]

- New method that expose geoPrecision in aroundLatLng. [Julien Lemoine]

- Fixed dependencies, restore old DefaultHttpClient. [Sylvain UTARD]

- Release jar 1.1.0. [Sylvain UTARD]

- Gradle-ified. [Sylvain UTARD]

- Fixed code comment. [Julien Lemoine]

- Renamed parameter names for typo configuration. [Julien Lemoine]

- Fixed typos. [Julien Lemoine]

- QueryType=prefixLast is now the default setting. documented unordered.
  [Julien Lemoine]

- Added helpers saveObjects(JSONArray) and addObjects(JSONArray) [Julien
  Lemoine]

- Moved numerics close to tags in doc other minor corrections. [Nicolas
  Dessaigne]

- Fixed numerics doc. [Julien Lemoine]

- Document new features: move/copy/logs/numerics. [Julien Lemoine]

- New code that supports numerics/move/copy/logs features. [Julien
  Lemoine]

- Check that objectID is not empty. [Julien Lemoine]

- Added "your account" link. [Julien Lemoine]

- Added Link to zip. [Julien Lemoine]

- Typos. [Algolia-dev]

- Correction. [Nicolas Dessaigne]

- Replaced cities by contacts. [Julien Lemoine]

- Added a new constructor that automatically set hostnames. [Julien
  Lemoine]

- Updated ranking doc. [Julien Lemoine]

- Fixed method name error. [Julien Lemoine]

- Added List indexes / delete index / ACLS documentation. Added per
  Index ACLS. [Julien Lemoine]

- Updated list of hostnames. [Julien Lemoine]

- Updated list of hostnames. [Julien Lemoine]

- Added documentation and support of attributesToSnippet & queryType.
  [Julien Lemoine]

- Removed obsolete comment. [Julien Lemoine]

- Fixed typo in comment. [Julien Lemoine]

- Remote unfinished sentence. [Julien Lemoine]

- Removed obsolete comment. [Julien Lemoine]

- Refactor to avoid package clash with offline SDK. [Julien Lemoine]

- Added url encoding of Index name on delete call. [Julien Lemoine]

- Removed note about javascript. [Julien Lemoine]

- Initial import. [Julien Lemoine]
