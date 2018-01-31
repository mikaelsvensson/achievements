#!/usr/bin/env bash

#
# Prerequisites for using this script:
# - Heroku CLI installed
# - You have signed in to Heroku in the CLI
#
# Usage:
# $ ./heroku-database-download.sh
#

# Kill all database connections (the service itself gobbles up all of them otherwise)
heroku pg:killall

heroku pg:backups:capture

heroku pg:backups:download

# Rename the downloaded database backup
TIMESTAMP=$(date "+%Y%m%d")
mv latest.dump heroku-database-$TIMESTAMP.dump