#!/bin/sh
# upload snapshot repo

# Note: allow the public to read the bucket
# gsutil defacl set public-read gs://gwt-plugin-snapshot

cd repository

# upload snapshot
gsutil cp -r . gs://gwt-eclipse-plugin/snapshot