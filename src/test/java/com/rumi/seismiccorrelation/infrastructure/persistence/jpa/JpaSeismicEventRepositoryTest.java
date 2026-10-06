package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaSeismicEventRepository.class)
class JpaSeismicEventRepositoryTest {

    @Autowired
    private JpaSeismicEventRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void storesAnIgpEventUnderANewUuidWithTheErColumns() {
        SeismicEvent stored = repository.save(igpEvent("IGP-2026-0412", 5.8));
        entityManager.flush();
        entityManager.clear();

        SeismicEvent found = repository.findById(UUID.fromString(stored.getId())).orElseThrow();
        assertThat(found.getOccurredAt()).isEqualTo(Instant.parse("2026-10-06T15:29:41Z"));
        assertThat(found.getMagnitude()).isEqualTo(5.8);
        assertThat(found.getMagnitudeScale()).isEqualTo("Mw");
        assertThat(found.getLatitude()).isEqualTo(-12.05);
        assertThat(found.getLongitude()).isEqualTo(-77.12);
        assertThat(found.getEpicenterDescription()).isEqualTo("Callao, 35 km SW");
        assertThat(found.getSource()).isEqualTo("IGP");
        assertThat(found.getDepth()).isNaN();
    }

    @Test
    void keepsAnIdThatIsAlreadyAUuid() {
        String id = "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9";

        SeismicEvent stored = repository.save(new SeismicEvent(
                id, 5.8, "Mw", Instant.parse("2026-10-06T15:29:41Z"), -12.05, -77.12, 40.0, null));

        assertThat(stored.getId()).isEqualTo(id);
    }

    @Test
    void findsAStoredEventWithTheSameOccurrence() {
        SeismicEvent stored = repository.save(igpEvent("IGP-2026-0412", 5.8));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findSameOccurrence(igpEvent("IGP-other-code", 5.8)))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(stored.getId()));
        assertThat(repository.findSameOccurrence(igpEvent("IGP-2026-0412", 5.9))).isEmpty();
    }

    @Test
    void theSchemaRejectsADuplicatedOccurrence() {
        repository.save(igpEvent("IGP-2026-0412", 5.8));
        entityManager.flush();

        assertThatThrownBy(() -> {
            repository.save(igpEvent("IGP-2026-0412", 5.8));
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);
    }

    @Test
    void findsTheEventThatOccurredLast() {
        repository.save(new SeismicEvent("IGP-1", 4.1, Instant.parse("2026-10-01T08:00:00Z"), -13.0, -76.0, 20.0));
        repository.save(new SeismicEvent("IGP-2", 5.8, Instant.parse("2026-10-06T15:29:41Z"), -12.05, -77.12, 38.0));
        repository.save(new SeismicEvent("IGP-3", 3.9, Instant.parse("2026-10-03T22:10:00Z"), -14.0, -75.0, 60.0));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findLatest()).hasValueSatisfying(
                latest -> assertThat(latest.getOccurredAt()).isEqualTo(Instant.parse("2026-10-06T15:29:41Z")));
    }

    @Test
    void findsNoLatestEventWhenNothingIsStored() {
        assertThat(repository.findLatest()).isEmpty();
    }

    private static SeismicEvent igpEvent(String code, double magnitude) {
        return new SeismicEvent(
                code, magnitude, "Mw", Instant.parse("2026-10-06T15:29:41Z"),
                -12.05, -77.12, 40.0, "Callao, 35 km SW");
    }
}
