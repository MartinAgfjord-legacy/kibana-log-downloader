#!/usr/bin/env bash
. "settings.sh"

if [ -z "$1" ]; then
    echo "USAGE: ./run.sh <origin_host>"
    exit 1
fi

if [ -z "$KIBANA_HOST" ]; then
    echo "KIBANA_HOST must be set, terminating"
    exit 1
fi

if [[ -v $2 ]]; then
    BEFORE="-Dbefore=$2"
else
    BEFORE=""
fi

ORIGIN_HOST=$1
OUTPUT_FILE="kibana.log"

java -Dkibana_host=$KIBANA_HOST -Dorigin_host=$ORIGIN_HOST $BEFORE -jar ./target/console-application-1.0-SNAPSHOT-jar-with-dependencies.jar > $OUTPUT_FILE

if [ $? == 0 ]; then
    echo "Wrote results to $OUTPUT_FILE"
fi