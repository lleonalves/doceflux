package com.doceflux.doceflux_api.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Aborta qualquer contexto de teste cujo datasource não seja o banco doceflux_test. */
public class TestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String REQUIRED_SUFFIX = "/doceflux_test";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        verify(context.getEnvironment().getProperty("spring.datasource.url"));
    }

    static void verify(String url) {
        if (url == null || !url.endsWith(REQUIRED_SUFFIX)) {
            throw new IllegalStateException(
                    "Testes só podem usar o banco doceflux_test, mas spring.datasource.url = " + url);
        }
    }
}
