# Algolia Search API Client for Android

[Algolia Search](https://www.algolia.com) is a hosted full-text, numerical, and faceted search engine capable of delivering realtime results from the first keystroke.
The **Algolia Search API Client for Android** lets you easily use the [Algolia Search REST API](https://www.algolia.com/doc/rest-api/search) from your Android code.

[![Build Status](https://travis-ci.org/algolia/algoliasearch-client-android.svg?branch=master)](https://travis-ci.org/algolia/algoliasearch-client-android) [![GitHub version](https://badge.fury.io/gh/algolia%2Falgoliasearch-client-android.svg)](http://badge.fury.io/gh/algolia%2Falgoliasearch-client-android)
_Note: If you were using **version 2.x** of our Android client, read the [migration guide to version 3.x](https://github.com/algolia/algoliasearch-client-android/wiki/Migration-guide-to-version-3.x)._


You can browse the automatically generated [reference documentation](https://community.algolia.com/algoliasearch-client-android/).
(See also the [offline-enabled version](https://community.algolia.com/algoliasearch-client-android/offline/).)

This project is open-source under the [MIT License](./LICENSE).


## Contributing

[Your contributions](https://github.com/algolia/algoliasearch-client-android/pull/new) are welcome! Please use our [formatting configuration](https://github.com/algolia/CodingStyle#android) to keep the coding style consistent.


## API Documentation

You can find the full reference on [Algolia's website](https://www.algolia.com/doc/api-client/android/).


## Table of Contents


1. **[Contributing](#contributing)**


1. **[Install](#install)**


1. **[Quick Start](#quick-start)**

    * [Initialize the client](#initialize-the-client)

1. **[Push data](#push-data)**


1. **[Configure](#configure)**


1. **[Search](#search)**


1. **[Search UI](#search-ui)**

    * [index.html](#indexhtml)





# Getting Started




## Install

Add the following dependency to your `Gradle` build file:

```gradle
dependencies {
    // [...]
    compile 'com.algolia:algoliasearch-android:3.10.1'
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

## Search UI

**Warning:** If you are building a web application, you may be more interested in using one of our
[frontend search UI libraries](https://www.algolia.com/doc/guides/search-ui/search-libraries/)

The following example shows how to build a front-end search quickly using
[InstantSearch.js](https://community.algolia.com/instantsearch.js/)

### index.html

```html
<!doctype html>
<head>
  <meta charset="UTF-8">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/instantsearch.js/1/instantsearch.min.css">
</head>
<body>
  <header>
    <div>
       <input id="search-input" placeholder="Search for products">
       <!-- We use a specific placeholder in the input to guides users in their search. -->
    
  </header>
  <main>
      
      
  </main>

  <script type="text/html" id="hit-template">
    
      <p class="hit-name">{{{_highlightResult.firstname.value}}} {{{_highlightResult.lastname.value}}}</p>
    
  </script>

  <script src="https://cdn.jsdelivr.net/instantsearch.js/1/instantsearch.min.js"></script>
  <script src="app.js"></script>
</body>
```

### app.js

```js
var search = instantsearch({
  // Replace with your own values
  appId: 'YourApplicationID',
  apiKey: 'YourSearchOnlyAPIKey', // search only API key, no ADMIN key
  indexName: 'contacts',
  urlSync: true
});

search.addWidget(
  instantsearch.widgets.searchBox({
    container: '#search-input'
  })
);

search.addWidget(
  instantsearch.widgets.hits({
    container: '#hits',
    hitsPerPage: 10,
    templates: {
      item: document.getElementById('hit-template').innerHTML,
      empty: "We didn't find any results for the search <em>\"{{query}}\"</em>"
    }
  })
);

search.start();
```

## Getting Help

- **Need help**? Ask a question to the [Algolia Community](https://discourse.algolia.com/) or on [Stack Overflow](http://stackoverflow.com/questions/tagged/algolia).
- **Found a bug?** You can open a [GitHub issue](https://github.com/algolia/algoliasearch-client-android/issues).



