#!/usr/bin/env bash

ACH_ID_BIKE=$(curl -sX POST -d @achievement-ridebike.json "http://localhost:8080/api/achievements" --header "Content-Type: application/json" | jq -r '.id')
ACH_ID_MOTORCYCLE=$(curl -sX POST -d @achievement-ridemotorcycle.json "http://localhost:8080/api/achievements" --header "Content-Type: application/json" | jq -r '.id')
curl -sX POST -d '{"prerequisite_achievement": "'"$ACH_ID_BIKE"'"}' "http://localhost:8080/api/achievements/$ACH_ID_MOTORCYCLE/steps" --header "Content-Type: application/json" > /dev/null
curl -sX POST -d '{"description": "Learn traffic rules for highways"}' "http://localhost:8080/api/achievements/$ACH_ID_MOTORCYCLE/steps" --header "Content-Type: application/json" > /dev/null
echo "Achievements: $ACH_ID_BIKE $ACH_ID_MOTORCYCLE"

ORG_ID=$(curl -sX POST -d @organization-scouterna.json "http://localhost:8080/api/organizations" --header "Content-Type: application/json" | jq -r '.id')
echo "Organization: $ORG_ID"

PERSON_ID_MIKAEL=$(curl -sX POST -d @person-mikael.json "http://localhost:8080/api/organizations/$ORG_ID/people" --header "Content-Type: application/json" | jq -r '.id')
PERSON_ID_ALICE=$(curl -sX POST -d @person-alice.json "http://localhost:8080/api/organizations/$ORG_ID/people" --header "Content-Type: application/json" | jq -r '.id')
PERSON_ID_BOB=$(curl -sX POST -d @person-bob.json "http://localhost:8080/api/organizations/$ORG_ID/people" --header "Content-Type: application/json" | jq -r '.id')
echo "People: $PERSON_ID_MIKAEL $PERSON_ID_ALICE $PERSON_ID_BOB"
