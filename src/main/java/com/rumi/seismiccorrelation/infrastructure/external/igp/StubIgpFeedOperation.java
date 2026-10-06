package com.rumi.seismiccorrelation.infrastructure.external.igp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * IGP feed of the dev profile. It always reports the sample event loaded by DevDataSeeder,
 * so the service runs without network access. With rumi.igp.stub.fail=true it fails like an
 * unavailable IGP.
 */
@Component
@Profile("dev")
public class StubIgpFeedOperation implements IgpFeedOperation {

    private final boolean fail;

    public StubIgpFeedOperation(@Value("${rumi.igp.stub.fail:false}") boolean fail) {
        this.fail = fail;
    }

    @Override
    public IgpSeismicEventResponse fetchLatest() {
        if (fail) {
            throw new IllegalStateException("Stub IGP feed configured to fail");
        }
        return new IgpSeismicEventResponse(
                "IGP/CENSIS/RS 2026-0412",
                5.8,
                Instant.parse("2026-10-06T15:29:41Z"),
                -12.05,
                -77.12,
                38.0
        );
    }
}
