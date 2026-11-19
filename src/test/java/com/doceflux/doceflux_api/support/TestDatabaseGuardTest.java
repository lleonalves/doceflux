package com.doceflux.doceflux_api.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseGuardTest {

    @Test
    void rejectsMainDatabase() {
        assertThatThrownBy(() -> TestDatabaseGuard.verify("jdbc:postgresql://localhost:5433/doceflux"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingUrl() {
        assertThatThrownBy(() -> TestDatabaseGuard.verify(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsTestDatabase() {
        assertThatCode(() -> TestDatabaseGuard.verify("jdbc:postgresql://localhost:5433/doceflux_test"))
                .doesNotThrowAnyException();
    }
}
