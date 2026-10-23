package fr.antigaspi.recipe.llm;

import static fr.antigaspi.recipe.RecipeFixtures.view;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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

import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeContext;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.utensil.model.Utensil;

class SpringAiRecipeGeneratorTest {

    /** Exemple du format JSON standardisé d'une recette. */
    private static final String RECIPE_JSON = """
            {
              "title": "Gratin de courgettes",
              "servings": 2,
              "preparationMinutes": 40,
              "utensils": ["Four", "Plat à gratin"],
              "ingredients": [
                {"ingredientId": 3, "name": "Courgettes", "quantity": 0.5, "unit": "kg"},
                {"ingredientId": null, "name": "Sel", "quantity": 1, "unit": "pincée"}
              ],
              "steps": ["Couper les courgettes", "Enfourner 30 minutes"]
            }
            """;

    private static final RecipeContext CONTEXT = new RecipeContext(
            List.of(view(3, "Courgettes", "2.5", Unit.KG, 2, false)), List.of(),
            List.of(new Utensil(1L, "Four"), new Utensil(2L, "Plat à gratin")), UserProfile.empty(), 2, null, null);

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
    void convertitLaReponseJsonStandardiseeEnRecette() {
        Recipe recipe = generatorAnswering(RECIPE_JSON).generate(CONTEXT);

        assertThat(recipe).isEqualTo(new Recipe("Gratin de courgettes", 2, 40, List.of("Four", "Plat à gratin"),
                List.of(new RecipeIngredient(3L, "Courgettes", new BigDecimal("0.5"), "kg"),
                        new RecipeIngredient(null, "Sel", new BigDecimal("1"), "pincée")),
                List.of("Couper les courgettes", "Enfourner 30 minutes")));
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
            assertThat(message.getText()).contains("[3] Courgettes", "- Plat à gratin");
        });
    }

    @Test
    void reponseNonJson_leveRecipeGenerationException() {
        assertThatThrownBy(() -> generatorAnswering("Désolé, je ne peux pas.").generate(CONTEXT))
                .isInstanceOf(RecipeGenerationException.class);
    }

    @Test
    void echecDeLAppelLlm_leveRecipeGenerationExceptionAvecLaCause() {
        IllegalStateException failure = new IllegalStateException("LLM indisponible");
        SpringAiRecipeGenerator generator = generator(prompt -> {
            throw failure;
        });

        assertThatThrownBy(() -> generator.generate(CONTEXT)).isInstanceOf(RecipeGenerationException.class)
                .hasRootCause(failure);
    }

    @Test
    void commentaireAvecAccolades_promptEnvoyeSansErreur() {
        CookedRecipe cooked = new CookedRecipe(1L, new Recipe("Soupe", 2, 30, List.of(), List.of(), List.of()),
                Instant.parse("2026-10-19T12:00:00Z"), Rating.DOWN, "trop {salé}");
        RecipeContext context = new RecipeContext(CONTEXT.priorityIngredients(), CONTEXT.otherIngredients(),
                CONTEXT.utensils(), CONTEXT.profile(), 2, null, null, null, null, List.of(cooked));

        generatorAnswering(RECIPE_JSON).generate(context);

        assertThat(sentPrompt.get().getInstructions()).anySatisfy(message -> {
            assertThat(message.getMessageType()).isEqualTo(MessageType.USER);
            assertThat(message.getText()).contains("salé");
        });
    }
}
