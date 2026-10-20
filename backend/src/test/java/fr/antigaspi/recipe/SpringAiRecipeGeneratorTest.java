package fr.antigaspi.recipe;

import static fr.antigaspi.recipe.RecipeFixtures.view;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ByteArrayResource;

import fr.antigaspi.ingredient.Unit;
import fr.antigaspi.profile.UserProfile;

class SpringAiRecipeGeneratorTest {

    private static final String RECIPE_JSON = """
            {
              "title": "Gratin de courgettes",
              "servings": 2,
              "totalMinutes": 40,
              "usedIngredients": [{"ingredientId": 3, "name": "Courgettes", "quantity": 0.5, "unit": "kg"}],
              "missingIngredients": [{"ingredientId": null, "name": "Sel", "quantity": 1, "unit": "pincée"}],
              "steps": ["Couper les courgettes", "Enfourner 30 minutes"],
              "requiredEquipment": ["Four"]
            }
            """;

    private static final RecipeContext CONTEXT = new RecipeContext(
            List.of(view(3, "Courgettes", "2.5", Unit.KG, 2, false)), new UserProfile(List.of(), List.of("Four")), 2,
            null);

    private final AtomicReference<Prompt> sentPrompt = new AtomicReference<>();

    private SpringAiRecipeGenerator generatorAnswering(String content) {
        ChatModel chatModel = prompt -> {
            sentPrompt.set(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
        };
        return generator(chatModel);
    }

    private static SpringAiRecipeGenerator generator(ChatModel chatModel) {
        RecipePromptBuilder promptBuilder = new RecipePromptBuilder(
                new ByteArrayResource("PROMPT SYSTEME".getBytes(StandardCharsets.UTF_8)));
        return new SpringAiRecipeGenerator(ChatClient.builder(chatModel), promptBuilder);
    }

    @Test
    void convertitLaReponseJsonEnRecette() {
        Recipe recipe = generatorAnswering(RECIPE_JSON).generate(CONTEXT);

        assertThat(recipe.title()).isEqualTo("Gratin de courgettes");
        assertThat(recipe.servings()).isEqualTo(2);
        assertThat(recipe.totalMinutes()).isEqualTo(40);
        assertThat(recipe.usedIngredients())
                .containsExactly(new RecipeIngredient(3L, "Courgettes", new BigDecimal("0.5"), "kg"));
        assertThat(recipe.missingIngredients()).extracting(RecipeIngredient::ingredientId).containsOnlyNulls();
        assertThat(recipe.steps()).hasSize(2);
        assertThat(recipe.requiredEquipment()).containsExactly("Four");
    }

    @Test
    void envoieLePromptSystemeEtLePromptUtilisateur() {
        generatorAnswering(RECIPE_JSON).generate(CONTEXT);

        Prompt prompt = sentPrompt.get();
        assertThat(prompt.getInstructions()).anySatisfy(message -> {
            assertThat(message.getMessageType()).isEqualTo(MessageType.SYSTEM);
            assertThat(message.getText()).contains("PROMPT SYSTEME");
        });
        assertThat(prompt.getInstructions()).anySatisfy(message -> {
            assertThat(message.getMessageType()).isEqualTo(MessageType.USER);
            assertThat(message.getText()).contains("[3] Courgettes");
        });
    }

    @Test
    void reponseNonJson_leveRecipeGenerationException() {
        assertThatThrownBy(() -> generatorAnswering("Désolé, je ne peux pas.").generate(CONTEXT))
                .isInstanceOf(RecipeGenerationException.class);
    }

    @Test
    void echecDeLAppelLlm_leveRecipeGenerationExceptionAvecLaCause() {
        IllegalStateException failure = new IllegalStateException("Bedrock indisponible");
        SpringAiRecipeGenerator generator = generator(prompt -> {
            throw failure;
        });

        assertThatThrownBy(() -> generator.generate(CONTEXT)).isInstanceOf(RecipeGenerationException.class)
                .hasRootCause(failure);
    }
}
