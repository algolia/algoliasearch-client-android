# Algolia Search API Client for Android

[Algolia Search](https://www.algolia.com) is a hosted full-text, numerical, and faceted search engine capable of delivering realtime results from the first keystroke.
The **Algolia Search API Client for Android** lets you easily use the [Algolia Search REST API](https://www.algolia.com/doc/rest-api/search) from your Android code.

[![Build Status](https://travis-ci.org/algolia/algoliasearch-client-android.svg?branch=master)](https://travis-ci.org/algolia/algoliasearch-client-android) [![GitHub version](https://badge.fury.io/gh/algolia%2Falgoliasearch-client-android.svg)](http://badge.fury.io/gh/algolia%2Falgoliasearch-client-android)
_Note: If you were using **version 2.x** of our Android client, read the [migration guide to version 3.x](https://github.com/algolia/algoliasearch-client-android/wiki/Migration-guide-to-version-3.x)._

*Note: If you were using **version 2.x** of our Android client, read the [migration guide to version 3.x](https://github.com/algolia/algoliasearch-client-android/wiki/Migration-guide-to-version-3.x).*



As a complement to this readme, you can browse the automatically generated [reference documentation](https://community.algolia.com/algoliasearch-client-android/).
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
    * [Push data](#push-data)
    * [Search](#search)
    * [Configure](#configure)

1. **[Getting Help](#getting-help)**





# Getting Started



## Install

Add the following dependency to your Gradle build file:

```gradle
dependencies {
    // [...]
    compile 'com.algolia:algoliasearch-android:3.5'
}
```

## Quick Start

In 30 seconds, this quick start tutorial will show you how to index and search objects.

### Initialize the client

You first need to initialize the client. For that you need your **Application ID** and **API Key**.
You can find both of them on [your Algolia account](https://www.algolia.com/api-keys).

```java
Client client = new Client("YOUR_APP_ID", "YOUR_API_KEY");
``````csharp
using Algolia.Search;

AlgoliaClient client = new AlgoliaClient("YourApplicationID", "YourAPIKey");
```

**Note:** **Note**: If you're using Algolia in an ASP.NET project you might experience some deadlocks while using our asynchronous API. You can fix it by calling the following method:

### Push data

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

### Search

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

### Configure

Settings can be customized to tune the search behavior. For example, you can add a custom sort by number of followers to the already great built-in relevance:

```java
JSONObject settings = new JSONObject().append("customRanking", "desc(followers)");
index.setSettingsAsync(settings, null);
```

You can also configure the list of attributes you want to index by order of importance (first = most important):

**Note:** Since the engine is designed to suggest results as you type, you'll generally search by prefix.
In this case the order of attributes is very important to decide which hit is the best:

```java
JSONObject settings = new JSONObject()
    .append("searchableAttributes", "lastname")
    .append("searchableAttributes", "firstname")
    .append("searchableAttributes", "company");
index.setSettingsAsync(settings, null);
```

## Getting Help

- **Need help**? Ask a question to the [Algolia Community](https://discourse.algolia.com/) or on [Stack Overflow](http://stackoverflow.com/questions/tagged/algolia).
- **Found a bug?** You can open a [GitHub issue](https://github.com/algolia/algoliasearch-client-android/issues).



