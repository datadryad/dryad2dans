#!/bin/bash

# Script to generate a list of items to be transferred to DANS
# Assumes the PGHOST and PGPASSWORD environment variables are set.

echo Creating list of items to transfer to DANS...

NUM_ITEMS=5000;
DANS_METADATA_FIELD=162
DANS_FAILED_FIELD=161

psql -qt -U dryad_app -d dryad_repo -c "select item_id from item where in_archive = true and owning_collection=2 and item_id NOT IN (Select distinct item_id from metadatavalue where metadata_field_id=$DANS_METADATA_FIELD) and item_id NOT IN (Select distinct item_id from metadatavalue where metadata_field_id=$DANS_FAILED_FIELD) order by item_id asc limit $NUM_ITEMS;" -o items.txt

echo List of items is in items.txt

