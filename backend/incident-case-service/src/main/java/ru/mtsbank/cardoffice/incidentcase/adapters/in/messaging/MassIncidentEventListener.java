package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import ru.mtsbank.cardoffice.incidentcase.application.ApplyMassIncidentEventUseCase;

import tools.jackson.databind.ObjectMapper;

/**
 * The sole consumer of the three {@code incident-case.mass-incident.*}
 * topics (AD-5, AD-11) -- three {@code @KafkaListener} methods, one per
 * topic, each validating {@code version} and converting {@code data} to
 * its own typed record before calling {@link ApplyMassIncidentEventUseCase}
 * (story Tasks & Acceptance).
 *
 * Payloads are consumed as raw {@code String} (not deserialized straight
 * into {@link MassIncidentEventEnvelope} by the Kafka client) specifically
 * so a version-mismatched event can be republished to the dead-letter topic
 * byte-for-byte identical to what the producer sent (story Boundaries &
 * Constraints: "its full original envelope is republished verbatim") --
 * re-serializing a parsed Java object back to JSON could reorder fields or
 * drop envelope-level properties this record doesn't know about.
 *
 * No listener method here ever throws: every failure path (unparseable
 * payload, unsupported version, a failure applying an otherwise-valid
 * event) is caught, logged, and routed to the dead-letter topic, then the
 * method returns normally. That is what gives "the consumer acknowledges
 * it, no retry loop, no crash" (story Boundaries & Constraints) for free
 * from Spring Kafka's default container behavior -- no custom error
 * handler or manual {@code Acknowledgment} is needed.
 */
@Component
public class MassIncidentEventListener {

    private static final Logger log = LoggerFactory.getLogger(MassIncidentEventListener.class);

    /** The one {@code version} value this service currently understands (AD-11). */
    static final int SUPPORTED_VERSION = 1;

    private final ApplyMassIncidentEventUseCase applyMassIncidentEventUseCase;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public MassIncidentEventListener(
            ApplyMassIncidentEventUseCase applyMassIncidentEventUseCase,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.applyMassIncidentEventUseCase = applyMassIncidentEventUseCase;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopicsConfig.TOPIC_OPENED, groupId = "incident-case-service")
    public void onOpened(String payload) {
        withValidEnvelope(payload, envelope -> {
            MassIncidentOpenedData data = objectMapper.treeToValue(envelope.data(), MassIncidentOpenedData.class);
            applyMassIncidentEventUseCase.applyOpened(
                    data.id(), data.theme(), data.product(), data.start(), data.status(), data.affected());
        });
    }

    @KafkaListener(topics = KafkaTopicsConfig.TOPIC_UPDATED, groupId = "incident-case-service")
    public void onUpdated(String payload) {
        withValidEnvelope(payload, envelope -> {
            MassIncidentUpdatedData data = objectMapper.treeToValue(envelope.data(), MassIncidentUpdatedData.class);
            applyMassIncidentEventUseCase.applyUpdated(data.id(), data.status(), data.affected());
        });
    }

    @KafkaListener(topics = KafkaTopicsConfig.TOPIC_CLOSED, groupId = "incident-case-service")
    public void onClosed(String payload) {
        withValidEnvelope(payload, envelope -> {
            MassIncidentClosedData data = objectMapper.treeToValue(envelope.data(), MassIncidentClosedData.class);
            applyMassIncidentEventUseCase.applyClosed(data.id(), data.status());
        });
    }

    /**
     * Parses {@code payload} into the envelope, checks {@code version}, and
     * either hands the envelope to {@code handler} or republishes the raw
     * {@code payload} verbatim to the dead-letter topic. Never lets an
     * exception escape -- see class Javadoc.
     */
    private void withValidEnvelope(String payload, EnvelopeHandler handler) {
        MassIncidentEventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(payload, MassIncidentEventEnvelope.class);
        } catch (Exception exception) {
            log.error("Unparseable mass-incident event payload, routing to dead-letter", exception);
            sendToDeadLetter(payload);
            return;
        }

        if (envelope.version() != SUPPORTED_VERSION) {
            // Never applied, never retried on its own topic (story Boundaries & Constraints)
            // -- routed once to the dead-letter topic and acknowledged.
            log.warn(
                    "Unsupported mass-incident event version {} for eventId={} eventType={}, routing to dead-letter",
                    envelope.version(),
                    envelope.eventId(),
                    envelope.eventType());
            sendToDeadLetter(payload);
            return;
        }

        try {
            handler.handle(envelope);
        } catch (Exception exception) {
            // Same "route to a dead-letter topic rather than fail silently" rule (AD-11,
            // story Boundaries & Constraints) applies here too: the envelope parsed and its
            // version is supported, but data couldn't be converted/applied, so it is dead-
            // lettered just like the other two failure paths rather than just logged and dropped.
            log.error(
                    "Failed to apply mass-incident event eventId={} eventType={}, routing to dead-letter",
                    envelope.eventId(),
                    envelope.eventType(),
                    exception);
            sendToDeadLetter(payload);
        }
    }

    private void sendToDeadLetter(String payload) {
        kafkaTemplate.send(KafkaTopicsConfig.TOPIC_DEAD_LETTER, payload).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to publish to dead-letter topic {}", KafkaTopicsConfig.TOPIC_DEAD_LETTER, exception);
            }
        });
    }

    @FunctionalInterface
    private interface EnvelopeHandler {
        void handle(MassIncidentEventEnvelope envelope) throws Exception;
    }
}
