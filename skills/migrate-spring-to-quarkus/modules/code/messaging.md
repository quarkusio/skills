# Module: Messaging

Migrate Spring messaging code (Kafka, RabbitMQ, JMS) to the Quarkus messaging model
appropriate to the source broker and protocol.

Load [references/annotation-map.md](../../references/annotation-map.md) and
[references/dependency-map.md](../../references/dependency-map.md) before starting.

**Strategy rule:** There is no `quarkus-spring-messaging` compatibility extension —
`@KafkaListener`, `@RabbitListener`, and `@JmsListener` cannot be retained regardless
of whether the overall migration strategy is `spring-compat` or `full-quarkus`.
Each broker maps to its own Quarkus connector:
- **Kafka / RabbitMQ / AMQP 1.0** — `quarkus-messaging-kafka`, `quarkus-messaging-rabbitmq`,
  or `quarkus-messaging-amqp`; listeners and producers use SmallRye Reactive Messaging
  (`@Incoming` / `@Outgoing` / `Emitter`).
- **JMS (ActiveMQ Classic / Artemis)** — `quarkus-artemis-jms` (Quarkiverse, provides the
  `ConnectionFactory`) plus `smallrye-reactive-messaging-jms` (in the Quarkus BOM, provides
  the `smallrye-jms` connector); listeners and producers also use SmallRye Reactive Messaging
  (`@Incoming` / `@Outgoing` / `Emitter`). See [JMS / Artemis](#jms--artemis).

**Unmigrated code rule:** If a Spring Messaging feature cannot be confidently mapped to
Quarkus, do not delete it. Leave it with a `// TODO: Migration required` comment and
include it in the migration report.

## What to do

- [ ] Scan all Java sources and all configuration files (`application.properties`,
      `application.yml`, and all profile variants such as `application-dev.properties`,
      `application-prod.yml`) for Spring messaging patterns
- [ ] For each listener — inspect broker type, consumer group, ack mode, concurrency,
      retry/DLQ/error-handler config, serialization, transaction boundary, and batch
      mode before converting (see [Listener migration](#listeners-incoming))
- [ ] For each producer — inspect message keys, partitions, headers,
      delivery guarantees, error handling, and transaction use before converting
      (see [Producer migration](#producers-channel-emitter))
- [ ] For each `@SendTo` — inspect reply destination, return type, headers, and
      whether the destination is dynamic before converting
      (see [`@SendTo` → `@Outgoing`](#sendto--outgoing))
- [ ] Rename outgoing channel names to avoid incoming/outgoing name conflicts (`-out` suffix);
      preserve the original broker destination/topic/queue name in the connector config
- [ ] Migrate messaging configuration from all active Spring configuration sources,
      including profile-specific files (see [Configuration migration](#configuration-migration))
- [ ] Remove Spring messaging dependencies; add the correct Quarkus extension
      (see [references/dependency-map.md](../../references/dependency-map.md))
- [ ] Remove Spring messaging imports; add MicroProfile Reactive Messaging imports
- [ ] Run the [Validation checklist](#validation-checklist)

## Broker distinction

These are three distinct connectors with different semantics — do not treat them as interchangeable.

| Spring technology | `pom.xml` extension | `mp.messaging.*.connector=` |
|---|---|---|
| Spring Kafka (`spring-kafka`) | `quarkus-messaging-kafka` | `smallrye-kafka` |
| Spring AMQP / RabbitMQ (`spring-boot-starter-amqp`) | `quarkus-messaging-rabbitmq` | `smallrye-rabbitmq` |
| Pure AMQP 1.0 (Azure Service Bus, Artemis AMQP mode) | `quarkus-messaging-amqp` | `smallrye-amqp` |
| Spring JMS / ActiveMQ / Artemis | `quarkus-artemis-jms` + `smallrye-reactive-messaging-jms`¹ | `smallrye-jms` |

¹ `quarkus-artemis-jms` is a Quarkiverse extension (not in the Quarkus platform); add it
explicitly. `smallrye-reactive-messaging-jms` is in the Quarkus BOM — no version needed.

**RabbitMQ vs AMQP 1.0:** `spring-boot-starter-amqp` uses RabbitMQ (AMQP 0-9-1) — map
to `quarkus-messaging-rabbitmq` / `smallrye-rabbitmq`. The `quarkus-messaging-amqp` /
`smallrye-amqp` pair is for AMQP 1.0 only. Confirm which protocol the source application
uses before choosing.

## Listeners (`@Incoming`)

Before converting, inspect each listener for:
- **Blocking work** — inspect the method body and call graph: does it perform database
  access, file I/O, synchronous HTTP, or any other blocking operation? If yes, add
  `@Blocking` — see [Blocking listeners](#blocking-listeners) below.
- **Consumer group** — preserve in `mp.messaging.incoming.<channel>.group.id`
- **Acknowledgment/commit semantics** — inspect Spring ack mode, auto-commit configuration, explicit acknowledgments, and failure behavior; select the corresponding Quarkus acknowledgment and broker commit strategy. Do not perform a one-to-one `AckMode` mapping.
- **Concurrency** — Kafka: use `mp.messaging.incoming.<channel>.partitions`; RabbitMQ: `mp.messaging.incoming.<channel>.worker-concurrency`
- **Retry / backoff** — preserve in connector retry config or via `@Retry` (MicroProfile Fault Tolerance)
- **Dead-letter** — Kafka: `mp.messaging.incoming.<channel>.dead-letter-queue.topic`; RabbitMQ: `mp.messaging.incoming.<channel>.dead-letter-exchange`
- **Error handler** — map `@KafkaListenerErrorHandler` to `@Incoming` with `nack()` and dead-letter routing
- **Serialization** — migrate `spring.kafka.consumer.value-deserializer` to connector-specific deserializer config
- **Transaction boundary** — see [Transaction semantics](#transaction-semantics)
- **Batch mode** — `@KafkaListener(batch=true)` requires a non-trivial migration; see
  [Batch listener migration](#batch-listener-migration) below before converting

All three Spring listener annotations map to `@Incoming`. The channel name comes from
the Spring annotation's `topics` / `queues` / `destination` attribute. The connector
in `application.properties` determines the broker — the annotation is the same for all three.

### Blocking listeners

`@Incoming` methods run on an I/O thread by default. Spring listener methods always ran
on dedicated thread-pool threads, so the original code may call blocking APIs freely.

**Inspect the listener body and every method it calls** for:
- Repository/DAO calls (`EntityManager`, JPA, JDBC, Panache)
- `@Transactional` service methods
- Synchronous HTTP/REST client calls
- File or socket I/O

If any blocking call is found, add `@Blocking` to the `@Incoming` method:

```java
@Incoming("orders")
@Blocking
@Transactional   // @Transactional alone does NOT move off the I/O thread — @Blocking required
public void processOrder(Order order) {
    repository.persist(order);
}
```

Use `@Blocking("my-pool")` to route to a named executor if the original Spring listener
used a custom `TaskExecutor`. If the method can be fully rewritten with reactive APIs
(returns `Uni<Void>`), remove `@Blocking` — but do not mix blocking calls into a `Uni` chain.

## Producers (`@Channel Emitter`)

Before converting, inspect each producer for:
- **Message keys** — `kafkaTemplate.send(topic, key, value)`: preserve key via `OutgoingKafkaRecordMetadata`
- **Partitions** — `kafkaTemplate.send(topic, partition, key, value)`: use `OutgoingKafkaRecordMetadata.partition`
- **Headers** — `ProducerRecord` headers: use `OutgoingKafkaRecordMetadata` or `OutgoingAmqpMetadata`
- **Callbacks / futures** — `ListenableFuture` result: use `emitter.send(Message.of(...).withAck(...).withNack(...))`
- **Delivery guarantees** — acks config: preserve in `mp.messaging.outgoing.<channel>.acks`
- **Error handling** — producer errors surface via `nack()`; document any retry behavior
- **Transactions** — see [Transaction semantics](#transaction-semantics)

`KafkaTemplate` / `RabbitTemplate` / `JmsTemplate` map to `@Channel`-injected `Emitter<T>`.
Use the `-out` suffix convention for the channel name and preserve the original broker
topic/queue name in the connector config. Per-message keys, partitions, or headers require
wrapping the payload in `Message.of(payload).addMetadata(OutgoingKafkaRecordMetadata...)`.

RabbitMQ and JMS producers follow the same `Emitter` pattern, but **RabbitMQ producers
publish to an exchange, not a queue** — inspect the original Spring configuration to
determine whether it publishes to a named exchange, uses the default exchange (routing by
queue name), or relies on a topic/fanout exchange. Set `exchange.name` and
`default-routing-key` on the outgoing channel accordingly (see [RabbitMQ configuration](#rabbitmq)
below). Connector-specific metadata classes (`OutgoingRabbitMQMetadata`) are used when
routing keys or message properties must be set per-message.

## `@SendTo` → `@Outgoing`

Before converting, inspect each `@SendTo` usage for:
- **Return type** — must be a serializable payload; adapt if the original returns a `Message<T>`
- **Headers / keys** — if the original copies headers from the incoming message, reproduce that via `Message<T>` wrapping
- **Destination type** — fixed string, SpEL expression, or class-level default (see below)

### Fixed destination

The straightforward case: a literal string destination maps directly to `@Outgoing`.

```java
// BEFORE
@KafkaListener(topics = "orders")
@SendTo("processed-orders")
public ProcessedOrder handle(Order order) {
    return process(order);
}

// AFTER
// "processed-orders-out" is the channel name only;
// set mp.messaging.outgoing.processed-orders-out.topic=processed-orders
// to keep the original broker topic name unchanged.
@Incoming("orders")
@Outgoing("processed-orders-out")
public ProcessedOrder handle(Order order) {
    return process(order);
}
```

### Dynamic destination (SpEL expression)

`@SendTo("#{...}")` has no direct SmallRye equivalent, but the routing intent is
usually expressible in application code. Analyse what the expression computes and
choose the appropriate approach:

| Original routing intent | Quarkus approach |
|---|---|
| Route based on a fixed set of possible destinations (e.g. `#{order.type}` resolves to one of a known set) | Declare one `@Outgoing` channel per destination; branch in application code and inject separate `@Channel Emitter`s |
| Forward to a destination derived from a message header or payload field | Wrap the return value as `Message<T>` and set broker-specific metadata (e.g. `OutgoingKafkaRecordMetadata` with the target topic); declare that channel as `@Outgoing` |
| Fan-out to multiple destinations conditionally | Inject multiple `@Channel Emitter`s; call `emitter.send()` for each applicable channel from within the method body |
| Truly arbitrary runtime destination (string computed at runtime from external state) | Cannot be expressed with static `@Outgoing`; implement as an explicit `Emitter` injected per channel, or use a single pass-through channel with a processor that routes internally |

In all cases: preserve the original routing behavior in the migrated code. Do not
reduce a multi-destination pattern to a single channel without verifying the business
intent. Document the approach taken in the migration report.

```java
// BEFORE: route based on order type (two possible destinations)
@KafkaListener(topics = "orders")
@SendTo("#{order.priority == 'HIGH' ? 'priority-orders' : 'standard-orders'}")
public Order route(Order order) { return order; }

// AFTER: explicit branching with two Emitters
@ApplicationScoped
public class OrderRouter {
    @Channel("priority-orders-out")   Emitter<Order> priorityEmitter;
    @Channel("standard-orders-out")   Emitter<Order> standardEmitter;

    @Incoming("orders")
    public void route(Order order) {
        if ("HIGH".equals(order.getPriority())) {
            priorityEmitter.send(order);
        } else {
            standardEmitter.send(order);
        }
    }
}
```

### Class-level `@SendTo`

Spring allows `@SendTo` at the class level as a default reply destination for all
listener methods in that class. SmallRye has no equivalent class-level annotation.
Move `@Outgoing` to each individual method.

## Channel naming: incoming/outgoing conflict rule

SmallRye Reactive Messaging does not allow the same channel name for both `@Incoming`
and `@Outgoing`. Using the same name produces:

```
SRMSG00073: Invalid configuration, the following channel names cannot be used
for both incoming and outgoing: [orders]
```

Use different channel names. Recommended convention: add `-out` suffix to the outgoing
channel name. **The underlying broker topic/queue is preserved — only the internal
channel name changes.**

```properties
# WRONG — same channel name on both sides
mp.messaging.incoming.orders.connector=smallrye-kafka
mp.messaging.outgoing.orders.connector=smallrye-kafka     # error

# CORRECT — different channel name, same underlying topic
mp.messaging.incoming.orders.connector=smallrye-kafka
mp.messaging.incoming.orders.topic=orders
mp.messaging.outgoing.orders-out.connector=smallrye-kafka
mp.messaging.outgoing.orders-out.topic=orders             # same broker topic — unchanged
```

This rule applies to all connectors. Scan all `application*.properties` and
`application*.yml` files, including profile-specific variants.

## Configuration migration

Inspect all active Spring configuration sources, including profile-specific files
(`application-dev.properties`, `application-prod.yml`, etc.), and migrate messaging
properties consistently.

**Serialization:** preserve the original wire format. Inspect Spring serializers and
deserializers before selecting Quarkus serde. Do not replace `JsonSerializer` /
`JsonDeserializer`, Schema Registry serde, or custom serializers with generic Jackson
serde without verifying wire compatibility — Spring `JsonSerializer` adds type-header
metadata by default (`spring.json.add.type.headers=true`); `ObjectMapperSerializer` does
not. Quarkus autodetects serde from the method signature for primitives, `String`,
`byte[]`, and typed custom implementations; omit the property when autodetection applies.
`List<T>` is **not** autodetected — set `value.deserializer` explicitly.

### Kafka

Key property mappings (`spring.kafka.*` → `mp.messaging.*`):

```properties
# extension: quarkus-messaging-kafka
kafka.bootstrap.servers=localhost:9092

mp.messaging.incoming.orders.connector=smallrye-kafka
mp.messaging.incoming.orders.topic=orders                   # spring.kafka.consumer.topics / @KafkaListener topics
mp.messaging.incoming.orders.group.id=order-service         # spring.kafka.consumer.group-id
mp.messaging.incoming.orders.auto.offset.reset=earliest     # spring.kafka.consumer.auto-offset-reset
# value.deserializer: omit when autodetected; set explicitly for custom/Schema Registry/List<T>

mp.messaging.outgoing.orders-out.connector=smallrye-kafka
mp.messaging.outgoing.orders-out.topic=orders               # preserve original topic name
# value.serializer: omit when autodetected; set explicitly otherwise
```

### RabbitMQ

`spring-boot-starter-amqp` uses RabbitMQ (AMQP 0-9-1). Use `smallrye-rabbitmq` and
`quarkus-messaging-rabbitmq` — not the AMQP 1.0 connector.

**Incoming vs outgoing asymmetry** — the addressing model differs by direction:

- **Incoming** — consumer binds to a **queue**: use `queue.name`
- **Outgoing** — producer publishes to an **exchange**: use `exchange.name` + `exchange.type` + `default-routing-key`. There is no `queue.name` on outgoing channels.

Inspect the original for exchange name, routing key, exchange type (`direct` / `topic` / `fanout` / `headers`; SmallRye default is `topic`), and whether the default exchange is used (empty name, routes by queue name).

```properties
# extension: quarkus-messaging-rabbitmq
rabbitmq-host=localhost  # spring.rabbitmq.host
rabbitmq-port=5672
rabbitmq-username=guest

# Incoming: queue
mp.messaging.incoming.orders.connector=smallrye-rabbitmq
mp.messaging.incoming.orders.queue.name=orders

# Outgoing: exchange (NOT queue.name)
mp.messaging.outgoing.orders-out.connector=smallrye-rabbitmq
mp.messaging.outgoing.orders-out.exchange.name=orders-exchange
mp.messaging.outgoing.orders-out.exchange.type=direct
mp.messaging.outgoing.orders-out.default-routing-key=orders
# Default exchange (route by queue name): set exchange.name= (empty)
```

### JMS / Artemis

JMS delivery semantics differ from Kafka and RabbitMQ — verify the broker type
(ActiveMQ Classic vs Artemis) before applying this mapping. Extensions:
`quarkus-artemis-jms` (Quarkiverse, provides `ConnectionFactory`) +
`smallrye-reactive-messaging-jms` (BOM, provides the `smallrye-jms` connector).

```properties
# extensions: quarkus-artemis-jms + smallrye-reactive-messaging-jms
quarkus.artemis.url=tcp://localhost:61616   # spring.activemq.broker-url
quarkus.artemis.username=admin

mp.messaging.incoming.orders.connector=smallrye-jms
mp.messaging.incoming.orders.destination=orders        # destination name (queue or topic)
mp.messaging.incoming.orders.destination-type=queue    # queue | topic
mp.messaging.incoming.orders.session-mode=AUTO_ACKNOWLEDGE

mp.messaging.outgoing.orders-out.connector=smallrye-jms
mp.messaging.outgoing.orders-out.destination=orders
mp.messaging.outgoing.orders-out.destination-type=queue
```

## Transaction semantics

Inspect whether the source uses database/JTA transactions, Kafka-native transactions, or
both. `@Transactional` + `emitter.send()` does **not** provide atomic DB/message behavior
— the message can be emitted even if the database transaction rolls back (dual-write
problem). Preserve Kafka-native transaction semantics using `KafkaTransactions.withTransaction(...)`
where applicable; for cross-system atomicity, use the transactional outbox pattern.
Document any behavioral difference in the migration report.

## Error handling, retry, and dead-letter

Inspect the original configuration before migrating:

- **Spring Retry** (`@Retryable` on listener, `RetryTemplate`): migrate to
  MicroProfile Fault Tolerance `@Retry` on the business method called from `@Incoming`
- **Backoff**: `@Retry(delay=..., jitter=...)` or connector-level
  `mp.messaging.incoming.<channel>.retry.attempts`
- **Dead-letter queue** (Kafka): `mp.messaging.incoming.<channel>.dead-letter-queue.topic`
- **Dead-letter exchange** (RabbitMQ): `mp.messaging.incoming.<channel>.dead-letter-exchange`
- **Error handlers** (`KafkaListenerErrorHandler`, `RabbitListenerErrorHandler`): migrate
  to `nack(failure)` in the `@Incoming` method; SmallRye routes nacked messages to the
  configured dead-letter destination
- **Recovery handlers**: document in the report; implement as a separate `@Incoming`
  consumer on the dead-letter channel

Preserve equivalent behavior where supported. Where an exact mapping does not exist,
document the behavioral difference in the migration report.

## Advanced patterns

### Batch listener migration

Do not mechanically map `@KafkaListener(batch=true)` to `@Incoming` with `List<T>`.

Inspect before choosing a target type:

| Aspect | What to look for |
|---|---|
| **Acknowledgment** | `BatchAcknowledgment` for per-record ack, or auto-ack the whole batch? |
| **Error handling** | `BatchErrorHandler`, `SeekToCurrentBatchErrorHandler`, or `ContainerStoppingBatchErrorHandler`? |
| **Record metadata** | Does it read offset, timestamp, headers, or partition from `ConsumerRecord`? |
| **Partial-batch failure** | Retry individual failed records, or skip and continue? |
| **Serde** | `List<T>` deserialization is **not** autodetected by Quarkus — always set `value.deserializer` explicitly |

Choose the SmallRye target type to match the ack and metadata requirements:

| Original pattern | SmallRye target type |
|---|---|
| `void consume(List<T> payloads)` — auto-ack whole batch | `@Incoming void consume(List<T> payloads)` — success commits latest offsets per partition; exception nacks entire batch |
| `void consume(List<ConsumerRecord<K,V>>)` — needs per-record metadata | `@Incoming void consume(ConsumerRecords<K,V> records)` |
| Per-record ack required | `@Incoming CompletionStage<Void> consume(Message<ConsumerRecords<K,V>> batch)` |
| Custom `BatchErrorHandler` — per-record retry | No direct equivalent — implement per-record `nack()` with DLQ, or redesign as single-record `@Incoming` with `max.poll.records` tuning; document the behavioral change |

Set batch mode explicitly if autodetection is ambiguous:

```properties
mp.messaging.incoming.orders.batch=true
mp.messaging.incoming.orders.value.deserializer=io.quarkus.kafka.client.serialization.ObjectMapperDeserializer
```

Document in the migration report: batch ack granularity, partial-failure behavior, and
`max.poll.records` expectations.

## Import updates

Remove obsolete Spring messaging imports and add the imports required by the selected
Quarkus messaging API (`org.eclipse.microprofile.reactive.messaging.*`,
`io.smallrye.reactive.messaging.annotations.Blocking` where needed,
`jakarta.enterprise.context.ApplicationScoped`).

## Validation checklist

After completing all transformations, verify:

- [ ] No Spring messaging annotations remain (`@KafkaListener`, `@RabbitListener`, `@JmsListener`, `@SendTo`, `@EnableKafka`, etc.)
- [ ] No Spring messaging imports remain (`org.springframework.kafka.*`, `org.springframework.amqp.*`, `org.springframework.jms.*`)
- [ ] No Spring messaging dependencies remain in `pom.xml` / `build.gradle`
- [ ] Every `@Incoming` channel has valid connector configuration in all active `application*.properties` / `application*.yml` files
- [ ] Every `@Outgoing` / `@Channel` has valid connector configuration
- [ ] No incoming and outgoing channel share the same name (`SRMSG00073`)
- [ ] Original broker destination/topic/queue names are preserved in connector config
- [ ] Serialization/deserialization configuration is present and correct for each channel
- [ ] Consumer group, ack mode, and retry behavior are preserved or the deviation is documented
- [ ] Dead-letter destinations are configured where the original had them
- [ ] Transaction boundaries are preserved or the deviation is documented
<!-- Useful in future
- [ ] Messaging-related tests pass where available
- [ ] Compile: `./mvnw clean compile -DskipTests` passes -->

## Watch out

- **RabbitMQ ≠ AMQP 1.0**: `spring-boot-starter-amqp` is RabbitMQ; use `smallrye-rabbitmq` / `quarkus-messaging-rabbitmq`, not the AMQP connector.
- **JMS semantics differ from Kafka/RabbitMQ**: do not apply Kafka or RabbitMQ patterns to JMS/Artemis; verify broker type and delivery guarantees separately.
- **Channel name conflicts**: the `-out` suffix renames the internal channel only — the broker topic/queue in the connector config must stay the same.
- **`@SendTo` class-level**: Spring allows it as a default reply destination; SmallRye does not — move `@Outgoing` to each method.
- **`@KafkaListener` containerFactory**: SmallRye has no equivalent; concurrency is set via `mp.messaging.incoming.<channel>.partitions` or connector threading config.
- **`@KafkaListener` groupId attribute**: must move to `mp.messaging.incoming.<channel>.group.id` in properties.
- **Profile-specific config**: Spring messaging properties may be in `application-dev.properties`, `application-prod.yml`, etc. — migrate all of them.
- **Batch listeners**: `@KafkaListener(batch=true)` requires explicit batch-semantics analysis; do not mechanically map it to `List<T>`. See [Batch listener migration](#batch-listener-migration).
