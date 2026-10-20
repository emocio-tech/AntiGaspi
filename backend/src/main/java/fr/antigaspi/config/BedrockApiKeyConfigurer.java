package fr.antigaspi.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Transmet la clé API Bedrock lue dans la configuration Spring (donc dans {@code .env}) au SDK AWS, qui ne la lit que
 * dans la variable d'environnement {@code AWS_BEARER_TOKEN_BEDROCK} ou la propriété système
 * {@value #SDK_SYSTEM_PROPERTY}. S'exécute avant la création du client Bedrock.
 */
@Component
public class BedrockApiKeyConfigurer implements BeanFactoryPostProcessor, EnvironmentAware {

    static final String API_KEY_PROPERTY = "antigaspi.llm.bedrock-api-key";
    static final String SDK_SYSTEM_PROPERTY = "aws.bearerTokenBedrock";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        String apiKey = environment.getProperty(API_KEY_PROPERTY);
        if (StringUtils.hasText(apiKey) && System.getProperty(SDK_SYSTEM_PROPERTY) == null) {
            System.setProperty(SDK_SYSTEM_PROPERTY, apiKey.strip());
        }
    }
}
