# Algolia Search API Client for Android

[Algolia Search](https://www.algolia.com) is a hosted full-text, numerical,
and faceted search engine capable of delivering realtime results from the first keystroke.

The **Algolia Search API Client for Android** lets
you easily use the [Algolia Search REST API](https://www.algolia.com/doc/rest-api/search) from
your Android code.

[![Build Status](https://travis-ci.org/algolia/algoliasearch-client-android.svg?branch=master)](https://travis-ci.org/algolia/algoliasearch-client-android) [![GitHub version](https://badge.fury.io/gh/algolia%2Falgoliasearch-client-android.svg)](http://badge.fury.io/gh/algolia%2Falgoliasearch-client-android)
_Note: If you were using **version 2.x** of our Android client, read the [migration guide to version 3.x](https://github.com/algolia/algoliasearch-client-android/wiki/Migration-guide-to-version-3.x)._


You can browse the automatically generated [reference documentation](https://community.algolia.com/algoliasearch-client-android/).
(See also the [offline-enabled version](https://community.algolia.com/algoliasearch-client-android/offline/).)

This project is open-source under the [MIT License](https://github.com/algolia/algoliasearch-client-android/blob/master/LICENSE).



## Contributing

[Your contributions](https://github.com/algolia/algoliasearch-client-android/pull/new) are welcome! Please use our [formatting configuration](https://github.com/algolia/CodingStyle#android) to keep the coding style consistent.



## API Documentation

You can find the full reference on [Algolia's website](https://www.algolia.com/doc/api-client/android/).



1. **[Contributing](#contributing)**


1. **[Install](#install)**


1. **[Quick Start](#quick-start)**


1. **[Push data](#push-data)**


1. **[Configure](#configure)**


1. **[Search](#search)**


1. **[List of available methods](#list-of-available-methods)**


1. **[Getting Help](#getting-help)**


1. **[List of available methods](#list-of-available-methods)**


# Getting Started



## Install

Add the following dependency to your `Gradle` build file:

```gradle
dependencies {
    // [...]
    compile 'com.algolia:algoliasearch-android:3.+'
    // This will automatically update to the latest v3 release when you build your project.
}
```

## Quick Start

In 30 seconds, this quick start tutorial will show you how to index and search objects.

### Initialize the client

To begin, you will need to initialize the client. In order to do this you will need your **Application ID** and **API Key**.
You can find both on [your Algolia account](https://www.algolia.com/api-keys).

```java
Client client = new Client("YourApplicationID", "YourAPIKey");
Index index = client.getIndex("your_index_name");
```

**Warning:** If you are building a native app on mobile, be sure to **not include** the search API key directly in the source code.
 You should instead consider [fetching the key from your servers](https://www.algolia.com/doc/guides/security/best-security-practices/#api-keys-in-mobile-applications)
 during the app's startup.

## Push data

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

## Configure

Settings can be customized to fine tune the search behavior. For example, you can add a custom sort by number of followers to further enhance the built-in relevance:

```java
JSONObject settings = new JSONObject().append("customRanking", "desc(followers)");
index.setSettingsAsync(settings, null);
```

You can also configure the list of attributes you want to index by order of importance (most important first).

**Note:** The Algolia engine is designed to suggest results as you type, which means you'll generally search by prefix.
In this case, the order of attributes is very important to decide which hit is the best:

```java
JSONObject settings = new JSONObject()
    .append("searchableAttributes", "lastname")
    .append("searchableAttributes", "firstname")
    .append("searchableAttributes", "company");
index.setSettingsAsync(settings, null);
```

## Search

You can now search for contacts using `firstname`, `lastname`, `company`, etc. (even with typos):

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




## List of available methods





### Search

- [Search index](https://algolia.com/doc/api-reference/api-methods/search/?language=android)
- [Search for facet values](https://algolia.com/doc/api-reference/api-methods/search-for-facet-values/?language=android)
- [Search multiple indexes](https://algolia.com/doc/api-reference/api-methods/multiple-queries/?language=android)
- [Browse index](https://algolia.com/doc/api-reference/api-methods/browse/?language=android)




### Indexing

- [Add objects](https://algolia.com/doc/api-reference/api-methods/add-objects/?language=android)
- [Update objects](https://algolia.com/doc/api-reference/api-methods/update-objects/?language=android)
- [Partial update objects](https://algolia.com/doc/api-reference/api-methods/partial-update-objects/?language=android)
- [Delete objects](https://algolia.com/doc/api-reference/api-methods/delete-objects/?language=android)
- [Delete by](https://algolia.com/doc/api-reference/api-methods/delete-by/?language=android)
- [Get objects](https://algolia.com/doc/api-reference/api-methods/get-objects/?language=android)
- [Custom batch](https://algolia.com/doc/api-reference/api-methods/batch/?language=android)




### Settings

- [Get settings](https://algolia.com/doc/api-reference/api-methods/get-settings/?language=android)
- [Set settings](https://algolia.com/doc/api-reference/api-methods/set-settings/?language=android)




### Manage indices

- [List indexes](https://algolia.com/doc/api-reference/api-methods/list-indices/?language=android)
- [Delete index](https://algolia.com/doc/api-reference/api-methods/delete-index/?language=android)
- [Copy index](https://algolia.com/doc/api-reference/api-methods/copy-index/?language=android)
- [Move index](https://algolia.com/doc/api-reference/api-methods/move-index/?language=android)
- [Clear index](https://algolia.com/doc/api-reference/api-methods/clear-index/?language=android)




### API Keys

- [Create secured API Key](https://algolia.com/doc/api-reference/api-methods/generate-secured-api-key/?language=android)
- [Add API Key](https://algolia.com/doc/api-reference/api-methods/add-api-key/?language=android)
- [Update API Key](https://algolia.com/doc/api-reference/api-methods/update-api-key/?language=android)
- [Delete API Key](https://algolia.com/doc/api-reference/api-methods/delete-api-key/?language=android)
- [Get API Key permissions](https://algolia.com/doc/api-reference/api-methods/get-api-key/?language=android)
- [List API Keys](https://algolia.com/doc/api-reference/api-methods/list-api-keys/?language=android)




### Synonyms

- [Save synonym](https://algolia.com/doc/api-reference/api-methods/save-synonym/?language=android)
- [Batch synonyms](https://algolia.com/doc/api-reference/api-methods/batch-synonyms/?language=android)
- [Delete synonym](https://algolia.com/doc/api-reference/api-methods/delete-synonym/?language=android)
- [Clear all synonyms](https://algolia.com/doc/api-reference/api-methods/clear-synonyms/?language=android)
- [Get synonym](https://algolia.com/doc/api-reference/api-methods/get-synonym/?language=android)
- [Search synonyms](https://algolia.com/doc/api-reference/api-methods/search-synonyms/?language=android)
- [Export Synonyms](https://algolia.com/doc/api-reference/api-methods/export-synonyms/?language=android)




### Query rules

- [Save rule](https://algolia.com/doc/api-reference/api-methods/rules-save/?language=android)
- [Batch rules](https://algolia.com/doc/api-reference/api-methods/rules-save-batch/?language=android)
- [Get rule](https://algolia.com/doc/api-reference/api-methods/rules-get/?language=android)
- [Delete rule](https://algolia.com/doc/api-reference/api-methods/rules-delete/?language=android)
- [Clear rules](https://algolia.com/doc/api-reference/api-methods/rules-clear/?language=android)
- [Search rules](https://algolia.com/doc/api-reference/api-methods/rules-search/?language=android)
- [Export rules](https://algolia.com/doc/api-reference/api-methods/rules-export/?language=android)




### A/B Test

- [Add A/B test](https://algolia.com/doc/api-reference/api-methods/add-ab-test/?language=android)
- [Get A/B test](https://algolia.com/doc/api-reference/api-methods/get-ab-test/?language=android)
- [List A/B tests](https://algolia.com/doc/api-reference/api-methods/get-ab-tests/?language=android)
- [Stop A/B test](https://algolia.com/doc/api-reference/api-methods/stop-ab-test/?language=android)
- [Delete A/B test](https://algolia.com/doc/api-reference/api-methods/delete-ab-test/?language=android)




### MultiClusters

- [Assign or Move userID](https://algolia.com/doc/api-reference/api-methods/assign-user-id/?language=android)
- [Get top userID](https://algolia.com/doc/api-reference/api-methods/get-top-user-id/?language=android)
- [Get userID](https://algolia.com/doc/api-reference/api-methods/get-user-id/?language=android)
- [List clusters](https://algolia.com/doc/api-reference/api-methods/list-clusters/?language=android)
- [List userIDs](https://algolia.com/doc/api-reference/api-methods/list-user-id/?language=android)
- [Remove userID](https://algolia.com/doc/api-reference/api-methods/remove-user-id/?language=android)
- [Search userID](https://algolia.com/doc/api-reference/api-methods/search-user-id/?language=android)




### Advanced

- [Get logs](https://algolia.com/doc/api-reference/api-methods/get-logs/?language=android)
- [Configuring timeouts](https://algolia.com/doc/api-reference/api-methods/configuring-timeouts/?language=android)
- [Set extra header](https://algolia.com/doc/api-reference/api-methods/set-extra-header/?language=android)
- [Wait for operations](https://algolia.com/doc/api-reference/api-methods/wait-task/?language=android)





## Getting Help

- **Need help**? Ask a question to the [Algolia Community](https://discourse.algolia.com/) or on [Stack Overflow](http://stackoverflow.com/questions/tagged/algolia).
- **Found a bug?** You can open a [GitHub issue](https://github.com/algolia/algoliasearch-client-android/issues).

