package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the three consumed event topics plus the shared dead-letter
 * topic explicitly via {@code NewTopic} beans (story Boundaries &
 * Constraints) rather than leaving them to broker auto-create -- Spring
 * Boot's auto-configured {@code KafkaAdmin} picks up every {@code NewTopic}
 * bean in the context and creates it on startup if missing.
 *
 * One partition / replica each: the demo's Kafka broker (docker-compose.yml)
 * is a single-node KRaft cluster, so a higher replication factor would fail
 * topic creation outright, and per-topic ordering (not parallelism) is all
 * this story needs.
 *
 * The dead-letter topic is singular and shared across all three source
 * topics (AD-11 says "a dead-letter topic", not one per source topic) --
 * see story Design Notes.
 */
@Configuration
public class KafkaTopicsConfig {

    public static final String TOPIC_OPENED = "incident-case.mass-incident.opened";
    public static final String TOPIC_UPDATED = "incident-case.mass-incident.updated";
    public static final String TOPIC_CLOSED = "incident-case.mass-incident.closed";
    public static final String TOPIC_DEAD_LETTER = "incident-case.mass-incident.dead-letter";

    @Bean
    public NewTopic massIncidentOpenedTopic() {
        return TopicBuilder.name(TOPIC_OPENED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic massIncidentUpdatedTopic() {
        return TopicBuilder.name(TOPIC_UPDATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic massIncidentClosedTopic() {
        return TopicBuilder.name(TOPIC_CLOSED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic massIncidentDeadLetterTopic() {
        return TopicBuilder.name(TOPIC_DEAD_LETTER).partitions(1).replicas(1).build();
    }
}
