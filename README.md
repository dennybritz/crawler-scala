blikk-crawler
=============

[ ![Codeship Status for dennybritz/blikk-crawler](https://codeship.io/projects/7cac7940-1e2c-0132-8f7f-66915511a081/status)](https://codeship.io/projects/35496)

### RabbitMQ exchanges and queues

**Exchanges**

- `blikk-data`
  - direct, non-durable
  - Used for all data items. The routing key for all messages is the application ID.
  - declared by the crawler service on startup

- `blikk-frontier-exchange`
  - topic, non-durable
  - Used to publish immediate HTTP requestes
  - declared by the frontier on startup

**Queues**


- `blikk-frontier-queue`
  - durable, non-exclusive, non-auto-delete
  - Bound to `blikk-frontier-exchange` to process the frontier queue
  - declared by the frontier on startup

- `blikk-frontier-queue-scheduled`
  - durable, non-exclusive, non-auto-delete, dead-letter-exchange=`blikk-frontier-exchange`
  - Bound to `blikk-frontier-exchange` to process the frontier queue
  - clients enqueue messages using the default exchange and `blikk-frontier-exchange` routing key
  - declared by the frontier on startup

- `[appID]`
  - non-durable, non-exclusive, non-auto-delete
  - bound to `blikk-data` with a routing key of [appID]. Each app instances uses the same queue.
  - declared by the `RabbitPublisher` in the application on startup