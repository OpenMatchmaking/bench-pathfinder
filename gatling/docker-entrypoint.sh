#!/usr/bin/env bash
set -e

local_development=${LOCAL_DEVELOPMENT:-false}


if [[ ${local_development} == true ]]; then
    timeout=${IDLE_TIMEOUT:-10000000000000000000}
    echo -e "\033[34mRunning container in an idle mode for a $timeout seconds...\033[0m"
    sleep ${timeout}
else
    ./bin/gatling.sh
fi
