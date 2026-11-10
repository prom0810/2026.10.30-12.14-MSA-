#!/usr/bin/env bash
set -euo pipefail

: "${HOST:=localhost}"
: "${PORT:=8080}"
: "${WAIT_ATTEMPTS:=100}"
: "${WAIT_INTERVAL:=3}"
BASE_URL="http://${HOST}:${PORT}"
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

START=false
STOP=false
for arg in "$@"; do
  case "$arg" in
    start) START=true ;;
    stop) STOP=true ;;
    *) echo "Usage: $0 [start] [stop]" >&2; exit 2 ;;
  esac
done

for command in curl jq; do
  command -v "$command" >/dev/null || { echo "Required command not found: $command" >&2; exit 1; }
done
if $START || $STOP; then
  command -v docker >/dev/null || { echo "Required command not found: docker" >&2; exit 1; }
fi

RESPONSE_FILE=$(mktemp)
function cleanup() {
  local status=$?
  trap - EXIT
  rm -f -- "$RESPONSE_FILE"
  if $STOP; then
    echo "Stopping the test environment..."
    docker compose down || { if [[ $status == 0 ]]; then status=1; fi; }
  fi
  exit "$status"
}
trap cleanup EXIT

# Store the body separately so HTTP errors cannot be mistaken for success.
function assertCurl() {
  local expectedStatus=$1
  shift
  local actualStatus
  if ! actualStatus=$(curl --silent --show-error --connect-timeout 5 --max-time 30 \
      --output "$RESPONSE_FILE" --write-out '%{http_code}' "$@"); then
    echo "Request failed: $*" >&2
    return 1
  fi
  if [[ "$actualStatus" != "$expectedStatus" ]]; then
    echo "Expected HTTP $expectedStatus, got $actualStatus: $*" >&2
    cat "$RESPONSE_FILE" >&2
    return 1
  fi
}

function waitForService() {
  local attempt status
  echo "Waiting for all core services through composite DELETE..."
  for ((attempt=1; attempt<=WAIT_ATTEMPTS; attempt++)); do
    if status=$(curl --silent --show-error --connect-timeout 5 --max-time 30 \
        -X DELETE "$BASE_URL/product-composite/13" --output "$RESPONSE_FILE" \
        --write-out '%{http_code}') && [[ "$status" == 200 ]]; then
      echo "Services ready; product 13 removed for the not-found test."
      return 0
    fi
    if ((attempt < WAIT_ATTEMPTS)); then sleep "$WAIT_INTERVAL"; fi
  done
  echo "Services did not become ready after $WAIT_ATTEMPTS attempts." >&2
  cat "$RESPONSE_FILE" >&2
  return 1
}

function recreateComposite() {
  local productId=$1
  local composite=$2
  assertCurl 200 -X DELETE "$BASE_URL/product-composite/$productId"
  assertCurl 200 -X POST "$BASE_URL/product-composite" \
    -H 'Content-Type: application/json' --data "$composite"
}

function setupTestdata() {
  local body
  body='{
    "productId":1,"name":"product 1","weight":1,
    "recommendations":[
      {"recommendationId":1,"author":"author 1","rate":1,"content":"content 1"},
      {"recommendationId":2,"author":"author 2","rate":2,"content":"content 2"},
      {"recommendationId":3,"author":"author 3","rate":3,"content":"content 3"}
    ],
    "reviews":[
      {"reviewId":1,"author":"author 1","subject":"subject 1","content":"content 1"},
      {"reviewId":2,"author":"author 2","subject":"subject 2","content":"content 2"},
      {"reviewId":3,"author":"author 3","subject":"subject 3","content":"content 3"}
    ]
  }'
  recreateComposite 1 "$body"

  body='{
    "productId":113,"name":"product 113","weight":113,
    "reviews":[
      {"reviewId":1,"author":"author 1","subject":"subject 1","content":"content 1"},
      {"reviewId":2,"author":"author 2","subject":"subject 2","content":"content 2"},
      {"reviewId":3,"author":"author 3","subject":"subject 3","content":"content 3"}
    ]
  }'
  recreateComposite 113 "$body"

  body='{
    "productId":213,"name":"product 213","weight":213,
    "recommendations":[
      {"recommendationId":1,"author":"author 1","rate":1,"content":"content 1"},
      {"recommendationId":2,"author":"author 2","rate":2,"content":"content 2"},
      {"recommendationId":3,"author":"author 3","rate":3,"content":"content 3"}
    ]
  }'
  recreateComposite 213 "$body"
}

function verifyProduct() {
  local productId=$1 recommendations=$2 reviews=$3
  assertCurl 200 "$BASE_URL/product-composite/$productId"
  jq -e --argjson id "$productId" --argjson recs "$recommendations" --argjson reviews "$reviews" '
    .productId == $id and .name == ("product " + ($id | tostring)) and .weight == $id
    and (.recommendations | type == "array" and length == $recs)
    and (.reviews | type == "array" and length == $reviews)
    and all(.recommendations[]; .content == ("content " + (.recommendationId | tostring)))
    and all(.reviews[]; .content == ("content " + (.reviewId | tostring)))
  ' "$RESPONSE_FILE" >/dev/null || {
    echo "Unexpected product response for $productId:" >&2
    cat "$RESPONSE_FILE" >&2
    return 1
  }
}

if $START; then
  echo "Restarting the test environment..."
  docker compose down --remove-orphans
  docker compose up -d
fi

echo "Test data IDs 1, 13, 113 and 213 will be deleted/recreated at $BASE_URL."
waitForService
setupTestdata
verifyProduct 1 3 3
verifyProduct 113 0 3
verifyProduct 213 3 0
assertCurl 404 "$BASE_URL/product-composite/13"
jq -e '.path == "/product-composite/13"' "$RESPONSE_FILE" >/dev/null
assertCurl 422 "$BASE_URL/product-composite/-1"
jq -e '.message == "Invalid productId: -1"' "$RESPONSE_FILE" >/dev/null

echo "All API assertions passed: $(date)"
