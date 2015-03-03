#!/bin/sh
mvn -Darguments="-DskipTests" release:prepare
mvn -Darguments="-DskipTests" release:perform
