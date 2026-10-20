package fr.antigaspi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

class LlmProviderEnvironmentPostProcessorTest {

    private static final String SPRING_AI = LlmProviderEnvironmentPostProcessor.SPRING_AI_PROPERTY;

    private static MockEnvironment process(MockEnvironment environment) {
        new LlmProviderEnvironmentPostProcessor().postProcessEnvironment(environment, null);
        return environment;
    }

    private static MockEnvironment withProvider(String value) {
        return new MockEnvironment().withProperty(LlmProviderEnvironmentPostProcessor.PROVIDER_VARIABLE, value);
    }

    @Test
    void bedrock_devientBedrockConverse() {
        assertThat(process(withProvider("bedrock")).getProperty(SPRING_AI)).isEqualTo("bedrock-converse");
        assertThat(process(withProvider("bedrock-converse")).getProperty(SPRING_AI)).isEqualTo("bedrock-converse");
    }

    @Test
    void openai_insensibleALaCasseEtAuxEspaces() {
        assertThat(process(withProvider(" OpenAI ")).getProperty(SPRING_AI)).isEqualTo("openai");
    }

    @Test
    void variableAbsenteOuVide_laConfigurationParDefautEstConservee() {
        MockEnvironment absent = new MockEnvironment().withProperty(SPRING_AI, "openai");
        assertThat(process(absent).getProperty(SPRING_AI)).isEqualTo("openai");

        MockEnvironment empty = withProvider("").withProperty(SPRING_AI, "openai");
        assertThat(process(empty).getProperty(SPRING_AI)).isEqualTo("openai");
    }

    @Test
    void valeurInconnue_erreurExplicite() {
        assertThatThrownBy(() -> process(withProvider("anthropic"))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("anthropic").hasMessageContaining("openai").hasMessageContaining("bedrock");
    }

    @Test
    void argumentDeLigneDeCommande_resteprioritaire() {
        MockEnvironment environment = withProvider("bedrock");
        environment.getPropertySources()
                .addFirst(new MapPropertySource("commandLineArgs", Map.of(SPRING_AI, "openai")));

        assertThat(process(environment).getProperty(SPRING_AI)).isEqualTo("openai");
    }
}
