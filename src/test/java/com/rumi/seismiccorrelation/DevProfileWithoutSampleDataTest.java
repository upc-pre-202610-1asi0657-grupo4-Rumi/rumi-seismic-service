package com.rumi.seismiccorrelation;

import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.infrastructure.devdata.DevDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "rumi.dev.sample-data=false")
@ActiveProfiles("dev")
class DevProfileWithoutSampleDataTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SeismicEventRepository seismicEventRepository;

    @Test
    void startsWithAnEmptyDatabase() {
        assertThat(context.getBeansOfType(DevDataSeeder.class)).isEmpty();
        assertThat(seismicEventRepository.findLatest()).isEmpty();
    }
}
