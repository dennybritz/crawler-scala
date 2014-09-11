#! /usr/bin/env bash

export BLIKK_REDIS_PORT=$REDIS_PORT_6379_TCP_PORT;
export BLIKK_REDIS_HOST=$REDIS_PORT_6379_TCP_ADDR;

# Pull the latest git repository
git pull;
sbt compile;
sbt stage;

# Print the environment for debugging
env;

exec "$@";