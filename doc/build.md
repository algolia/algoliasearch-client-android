Title: Build Process


# Online and offline flavors

## Overview

The client exists in two different flavors:

- The **online** flavor, which is the regular API client. Its source code is located under the `algoliasearch/src/main` directory. It gets published to Maven Central as `instantsearch-android-client`.

- The **offline** flavor, which is a superset of the online flavor. In addition to the main source code, it adds the `algoliasearch/src/offline` directory. It gets published to Maven Central as `instantsearch-android-client-offline`. This flavor has a dependency on the Algolia Search Offline Core module (`algoliasearch-offline-core-android` in Maven).


## Problem

While the flavors work (rather) well with Android Studio, things become more complicated when it comes to publishing the compiled libraries to Maven. Publishing to Maven via Gradle can be achieved with two different plugins:

1. The **legacy** [`maven` plugin](https://docs.gradle.org/current/userguide/maven_plugin.html). Unfortunately, this plugin does not allow overriding the artifact's names. Therefore, the AAR for each flavor would be published with an `onlineRelease` or `offlineRelease` specifier, thus resulting in a module with no main artifact, thus in an invalid module.

2. The **experimental** [`maven-publish` plugin](https://docs.gradle.org/current/userguide/publishing_maven.html). Unfortunately, this plugin does not support signatures, which are required to publish to Maven Central.

As we can see, *none of two solutions available allow us to publish each flavor as a distinct artifact*.


## Solution

Consequently, the approach taken is as follows:

- There are **two Gradle scripts** in the `algoliasearch` directory: `build-online.gradle` and `build-offline.gradle`. Each of them compiles just one flavor.

- Most of the common settings are factorized in the `_common.gradle` script.

- When building, use the `select-flavor.sh` script to choose which flavor to compile. Practically, this will create a `gradle.build` as a symbolic link to the selected flavor.

- Then use Gradle as you would with a normal module.


# Build-time dependencies

We rely on the [Pegdown Doclet](https://github.com/Abnaxos/pegdown-doclet) to generate our reference documentation ("Javadoc"). This is a built-time dependency that does not impact pre-built binaries.

However, when targeting the source version of the API Client (e.g. when developing or debugging), this dependency bubbles up to the dependent project. After literally hours of searching the Internet, I found no way to persuade Gradle to automatically draw this dependency. All normal tricks (like using `compileOnly`, `provided` or `classpath`) failed miserably. Therefore, you have to put this in the *top-level* `build.gradle` from the dependent project:

```
buildscript {
    // [...]
    dependencies {
        // [...]
        classpath 'ch.raffael.pegdown-doclet:pegdown-doclet:1.3' // FIXME: DO NOT COMMIT
    }
}
```

An alternative is just to comment out the line referring to the plugin in the API Client's `common.gradle`.

**Warning:** In either case, please make sure not to commit those changes!
