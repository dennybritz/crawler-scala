#! /usr/bin/env bash

# Start a redis container
docker run --rm --name redis -p 16379:6379 dockerfile/redis

# Start a node
docker run --rm -t --name blikk-crawler-1 \
  --net=host \
  dennybritz/blikk-crawler \
  /bin/bash -c " \
  export BLIKK_REDIS_PORT=16379; \
  export BLIKK_API_HOST=localhost; \
  export BLIKK_API_PORT=8080; \
  export BLIKK_SEEDS_FILE=seeds.txt; \
  echo 'localhost:8080' > seeds.txt; \
  /blikk-crawler/crawler-backend/target/start"


# Start another node
docker run --rm -t --name blikk-crawler-2 \
  --net=host \
  dennybritz/blikk-crawler \
  /bin/bash -c " \
  export BLIKK_REDIS_PORT=16379; \
  export BLIKK_API_HOST=localhost; \
  export BLIKK_API_PORT=8081; \
  export BLIKK_SEEDS_FILE=seeds.txt; \
  echo 'localhost:8080' > seeds.txt; \
  /blikk-crawler/crawler-backend/target/start"