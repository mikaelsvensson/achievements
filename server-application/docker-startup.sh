#!/bin/bash -e

if [ -z "${ENVIRONMENT}" ]; then
  export ENVIRONMENT=local
fi

java -jar server-application-1.0-SNAPSHOT.jar db migrate "${ENVIRONMENT}.yaml"

exec java -jar server-application-1.0-SNAPSHOT.jar server "${ENVIRONMENT}.yaml"