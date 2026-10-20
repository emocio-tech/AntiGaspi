package fr.antigaspi.config;

import java.util.Locale;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

/**
 * Traduit la variable {@code LLM_PROVIDER} du {@code .env} (valeurs du projet : {@code openai}, {@code bedrock}) en
 * propriété Spring AI {@code spring.ai.model.chat}, avec une erreur claire si la valeur est inconnue. S'exécute après
 * le chargement du {@code .env} ; un argument de ligne de commande reste prioritaire.
 */
public class LlmProviderEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROVIDER_VARIABLE = "LLM_PROVIDER";
    static final String SPRING_AI_PROPERTY = "spring.ai.model.chat";

    private static final String COMMAND_LINE_SOURCE = "commandLineArgs";
    private static final String PROPERTY_SOURCE_NAME = "antigaspiLlmProvider";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String provider = environment.getProperty(PROVIDER_VARIABLE);
        if (!StringUtils.hasText(provider)) {
            return;
        }
        MapPropertySource source = new MapPropertySource(PROPERTY_SOURCE_NAME,
                Map.of(SPRING_AI_PROPERTY, toSpringAiValue(provider)));
        MutablePropertySources sources = environment.getPropertySources();
        if (sources.contains(COMMAND_LINE_SOURCE)) {
            sources.addAfter(COMMAND_LINE_SOURCE, source);
        } else {
            sources.addFirst(source);
        }
    }

    private static String toSpringAiValue(String provider) {
        return switch (provider.strip().toLowerCase(Locale.ROOT)) {
        case "openai" -> "openai";
        case "bedrock", "bedrock-converse" -> "bedrock-converse";
        default -> throw new IllegalStateException(
                "LLM_PROVIDER inconnu : '" + provider + "' (valeurs possibles : openai, bedrock)");
        };
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
