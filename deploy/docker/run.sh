#! /usr/bin/env bash

if [ -n "$REDIS_PORT_6379_TCP_PORT" ]; then
  export BLIKK_REDIS_PORT=$REDIS_PORT_6379_TCP_PORT;
fi

if [ -n "$REDIS_PORT_6379_TCP_ADDR" ]; then
  export BLIKK_REDIS_HOST=$REDIS_PORT_6379_TCP_ADDR;
fi  

# Pull the latest git repository
git pull;
sbt compile;
sbt stage;

# Print the environment for debugging
env;

exec "$@";