package fr.antigaspi.recipe.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeContext;

/**
 * Générateur de recettes s'appuyant sur Spring AI : le fournisseur (Bedrock, Anthropic, OpenAI…) dépend uniquement de
 * la configuration. La réponse JSON est convertie en {@link Recipe} par la sortie structurée de Spring AI.
 */
@Component
public class SpringAiRecipeGenerator implements RecipeGenerator {

    private static final Logger log = LoggerFactory.getLogger(SpringAiRecipeGenerator.class);

    private final ChatClient chatClient;
    private final RecipePromptBuilder promptBuilder;

    public SpringAiRecipeGenerator(ChatClient.Builder chatClientBuilder, RecipePromptBuilder promptBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.promptBuilder = promptBuilder;
    }

    @Override
    public Recipe generate(RecipeContext context) {
        long start = System.currentTimeMillis();
        ResponseEntity<ChatResponse, Recipe> response;
        try {
            response = chatClient.prompt().system(promptBuilder.systemPrompt()).user(promptBuilder.userPrompt(context))
                    .call().responseEntity(Recipe.class);
        } catch (RuntimeException e) {
            throw new RecipeGenerationException("Échec de la génération de recette : " + e.getMessage(), e);
        }
        logUsage(response.response(), System.currentTimeMillis() - start);
        if (response.entity() == null) {
            throw new RecipeGenerationException("Le LLM n'a renvoyé aucune recette exploitable");
        }
        return response.entity();
    }

    /** Durée et tokens consommés, pour suivre le coût du fournisseur. */
    private static void logUsage(ChatResponse chatResponse, long durationMs) {
        Usage usage = chatResponse == null ? null : chatResponse.getMetadata().getUsage();
        if (usage == null) {
            log.info("Recette générée en {} ms", durationMs);
            return;
        }
        log.info("Recette générée en {} ms (tokens : {} en entrée, {} en sortie)", durationMs, usage.getPromptTokens(),
                usage.getCompletionTokens());
    }
}
