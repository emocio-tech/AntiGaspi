package fr.antigaspi.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class BedrockApiKeyConfigurerTest {

    private static final String SDK_PROPERTY = BedrockApiKeyConfigurer.SDK_SYSTEM_PROPERTY;

    @BeforeEach
    @AfterEach
    void clearSystemProperty() {
        System.clearProperty(SDK_PROPERTY);
    }

    private static void configure(MockEnvironment environment) {
        BedrockApiKeyConfigurer configurer = new BedrockApiKeyConfigurer();
        configurer.setEnvironment(environment);
        configurer.postProcessBeanFactory(null);
    }

    @Test
    void cleConfiguree_estTransmiseAuSdkAws() {
        configure(new MockEnvironment().withProperty(BedrockApiKeyConfigurer.API_KEY_PROPERTY, "ABSK-test"));

        assertThat(System.getProperty(SDK_PROPERTY)).isEqualTo("ABSK-test");
    }

    @Test
    void cleAbsenteOuVide_rienNEstDefini() {
        configure(new MockEnvironment());
        assertThat(System.getProperty(SDK_PROPERTY)).isNull();

        configure(new MockEnvironment().withProperty(BedrockApiKeyConfigurer.API_KEY_PROPERTY, "  "));
        assertThat(System.getProperty(SDK_PROPERTY)).isNull();
    }

    @Test
    void proprieteSystemeDejaDefinie_nEstPasEcrasee() {
        System.setProperty(SDK_PROPERTY, "deja-la");

        configure(new MockEnvironment().withProperty(BedrockApiKeyConfigurer.API_KEY_PROPERTY, "ABSK-test"));

        assertThat(System.getProperty(SDK_PROPERTY)).isEqualTo("deja-la");
    }
}
