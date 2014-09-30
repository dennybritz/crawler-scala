blikk-crawler
=============

[ ![Codeship Status for dennybritz/blikk-crawler](https://codeship.io/projects/7cac7940-1e2c-0132-8f7f-66915511a081/status)](https://codeship.io/projects/35496)

### RabbitMQ exchanges and queues

**Exchanges**

- `com.blikk.crawler.data-x`
  - direct, non-durable
  - Used for all data items. The routing key for all messages is the application ID.
  - declared by the crawler service on startup

- `com.blikk.crawler.frontier-x`
  - topic, non-durable
  - Used to publish immediate HTTP requestes
  - declared by the frontier on startup

**Queues**

- `com.blikk.crawler.frontier-q`
  - durable, non-exclusive, non-auto-delete
  - Bound to `com.blikk.crawler.frontier` to process the frontier
  - Ignores the routing key (= takes anything from the frontier)
  - declared by the frontier on startup

- `com.blikk.crawler.frontier-scheduled-q`
  - durable, non-exclusive, non-auto-delete, dead-letter-exchange=`com.blikk.crawler.frontier-x`
  - clients enqueue messages using the default exchange and `com.blikk.crawler.frontier-x` routing key
  - declared by the frontier on startup

- `[appId]`
  - non-durable, non-exclusive, non-auto-delete
  - bound to `com.blikk.crawler.data-x` with a routing key of `[appId]`. Each app instance uses the same queue.
  - declared by the `RabbitPublisher` in the application on startup
  - appIds are recommended to be in package-like dot notation