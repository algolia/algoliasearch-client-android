

# Offline mode

**Table of contents**

* [Overview](#overview)
* [Setup](#setup)
* [Usage](#usage)
* [Unsupported features](#unsupported-features)
* [Troubleshooting](#troubleshooting)
* [Other resources](#other-resources)


## Overview

The API client can be enhanced with offline capabilities. This optional **offline mode** allows you to mirror online indices on local storage, and transparently switch to the local mirror in case of unavailability of the online index, thus providing uninterrupted user experience. You can also explicitly query the mirror if you want.

Because indices can be arbitrarily big, whereas mobile devices tend to be constrained by network bandwidth, disk space or memory consumption, only part of an index's data is usually synchronized (typically the most popular entries, or what's relevant to the user, or close to her location...). The offline mode lets you control which data subset you want to synchronize, and how often to synchronize it.

Index settings are automatically synchronized, so that your local index will behave exactly as your online index, with a few restrictions:

- If only part of the data is mirrored, queries may obviously return less objects. Counts (hits, facets...) may also differ.

- Some advanced features are not supported offline. See [Unsupported features](#unsupported-features) for more information.


### Availability

Offline features are brought by Algolia's **Offline SDK**, which is actually composed of two separate components:

- The **Offline API Client** is a superset of the regular, online API client (so all your existing code will work without any modification). It is open source; the source code is available on GitHub in the same repository as the online [Android API client](https://github.com/algolia/algoliasearch-client-android).

- The **Offline Core** is a closed source component using native libraries. Although it is readily available for download, *it is licensed separately*, and will not work without a valid **license key**. Please [contact us](https://www.algolia.com/) for more information.



## Setup

### Prerequisites

1. Obtain a **license key** from [Algolia](https://www.algolia.com/).

2. Make sure you use an **API key** with the following ACLs:

    - Search (`search`)
    - Browse (`browse`)
    - Get index settings (`settings`)

    *This is required because the offline mode needs to replicate the online index's settings and uses browse requests when syncing.*


### Steps

2. In your Gradle script, use the `algoliasearch-offline-android` package in place of `algoliasearch-android`. Typically:

    ```groovy
    dependencies {
        // [...] other dependencies
        compile "com.algolia:algoliasearch-offline-android:$LATEST_VERSION_HERE@aar"
    }
    ```

3. When initializing your client, instantiate an `OfflineClient` instead of a `Client`. This requires you to specify an additional argument: the directory in which local indices will be stored:

    ```java
    client = new OfflineClient(context, "YOUR_APP_ID", "YOUR_API_KEY");
    ```

    ... where `context` is a valid Android `Context`.

    By default, the client will store data for local indices in an `algolia` subdirectory of the application's files directory. Alternatively, you may specify it during the instantiation of the client.
    *Warning: although using the cache directory may be tempting, we advise you against doing so. There is no guarantee that all files will be deleted together, and a partial delete could leave an index in an inconsistent state.*

4. **Enable offline mode**:

    ```java
    client.enableOfflineMode("YOUR_OFFLINE_SDK_LICENSE_KEY")
    ```


## Usage

### Activation

An `OfflineClient` provides all the features of an online `Client`. It returns `MirroredIndex` instances, which in turn provide all the features of online `Index` instances.

However, until you explicitly enable the offline mode by calling `enableOfflineMode()`, your offline client behaves like a regular online client. The same goes for indices: *you must explicitly activate mirroring* by calling `setMirrored(true)`. The reason is that you might not want to mirror all of your indices.

*Warning: Calling offline features before enabling mirroring on an index is a programming error and will be punished by an `IllegalStateException`.*


### Synchronization

You have entire control over *what* is synchronized and *when*.

#### What

First, specify what subset of the data is to be synchronized. You do so by constructing **data selection queries** and calling `MirroredIndex.setDataSelectionQueries()`.

A *data selection query* is essentially a combination of a browse `Query` and a maximum object count. When syncing, the offline index will browse the online index, filtering objects through the provided query (which can be empty, hence selecting all objects), and stopping when the maximum object count has been reached (or at the end of the index, whichever comes first).

It will do so for every data selection query you have provided, then build (or re-build) the local index from the retrieved data. (When re-building, the previous version of the local index remains available for querying, until it is replaced by the new version.)

*Warning: The entire selected data is re-downloaded at every sync, so be careful about bandwidth usage!*

*Warning: It is a programming error to attempt a sync with no data selection queries. Doing so will result in an `IllegalStateException` being thrown.*

*Note: Because the sync uses a "browse" to retrieve objects, the number of objects actually mirrored may exceed the maximum object count specified in the data selection query (up to one page of results of difference).*


#### When

The easiest way to synchronize your index is to use the **semi-automatic mode**:

- Choose a minimum delay between two syncs by calling `MirroredIndex.setDelayBetweenSyncs()`. The default is 24 hours.

- Whenever conditions are met for a potential sync (e.g. the device is online), call `MirroredIndex.syncIfNeeded()`. If the last successful sync is older than the minimum delay, a sync will be launched. Otherwise, the call will be ignored.

Alternatively, you may call `MirroredIndex.sync()` to force a sync to happen now.

The reason you have to choose when to synchronize is that the decision depends on various factors that the SDK cannot know, in particular the specific business rules of your application, or the user's preferences. For example, you may want to sync only when connected to a non-metered Wi-Fi network, or during the night.

*Note: Syncs always happen in the background, and therefore should not impact the user's experience.*

*Note: You cannot have two syncs on the same index running in parallel. If a sync is already running, concurrent sync requests will be ignored.*


### Querying

#### Transparent fallback

You query a mirrored index using the same `searchAsync()` method as a purely online index. This will use the offline mirror as a fallback in case of failure of the online request.

There's a catch, however. A mirrored index can function in two modes:

1. **Preventive offline search** (default). In this mode, if the online request is too slow to return, an offline request is launched preventively after a certain delay.

    *Warning: This may lead to your completion handler being called twice:* a first time with the offline results, and a second time with the online results. However, if the online request is fast enough (and successful, or the error cannot be recovered), the callback will be called just once.

    You may adjust the delay using `MirroredIndex.setPreventiveOfflineSearchDelay()`. The default is `MirroredIndex.DEFAULT_PREVENTIVE_OFFLINE_SEARCH_DELAY`.

2. **Offline fallback.** In this mode, the index first tries an online request, and in case of failure, switches back to the offline mirror, but only after the online request has failed. Therefore, the completion handler is guaranteed to be called exactly once.

You can switch between those two modes by calling `MirroredIndex.setPreventiveOfflineSearch()`.

The origin of the data is indicated by the `origin` attribute in the returned result object: its value is `remote` if the content originates from the online API, and `local` if the content originates from the offline mirror.


#### Direct query

You may directly target the offline mirror if you want:

- for search queries: `searchMirrorAsync()`
- for browse queries: `browseMirrorAsync()` and `browseMirrorFromAsync()`

Those methods have the same semantics as their online counterpart.

Browsing is especially relevant when synchronizing [associated resources](#associated-resources).


### Events

You may be informed of sync events by registering a `SyncListener` on a `MirroredIndex` instance (using `addSyncListener()`).


### Associated resources

In a typical setup, objects from an index are not standalone. Rather, they reference other resources (for example images) that are stored outside of Algolia. They are called **associated resources**.

In order to offer the best user experience, you may want to pre-load some of those resources while you are online, so that they are available when offline.

To do so:

- register a listener on the mirrored index (see [Events](#events) above);
- when the sync is finished, browse the local mirror, parsing each object to detect associated resources;
- pre-fetch those that are not already available locally.

*Note: You should do so from a background thread with a low priority to minimize impact on the user's experience.*



## Unsupported features

Due mainly to binary size constraints, the following features are not supported by the offline mode:

- plurals dictionary (simple plurals with a final S are still handled)
- CJK segmentation
- IP geolocation (`aroundLatLngViaIP` query parameter)



## Troubleshooting

### Logs

The SDK logs messages using the regular Android logging system, which can be useful when troubleshooting.

If the SDK does not seem to work, first check for proper initialization in the logs. You should see something like:

```
Algolia SDK v1.2.3
Algolia SDK licensed to: Peanuts, Inc.
```

Any unexpected condition should result in a warning/error being logged.


### Support

If you think you found a bug in the offline client, please submit an issue on GitHub so that it can benefit the community.

If you have a crash in the offline core, please send us a support request. Make sure to include the *crash report*, so that we can pinpoint the problem.


## Other resources

The offline client bears extensive Javadoc documentation. Please refer to it for more details.

