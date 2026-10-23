package fr.antigaspi.recipe.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.profile.service.ProfileService;
import fr.antigaspi.recipe.llm.RecipeGenerationException;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeContext;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.model.RecipeIssue;
import fr.antigaspi.recipe.model.RecipeIssueType;
import fr.antigaspi.recipe.repository.InMemoryCookedRecipeRepository;
import fr.antigaspi.recipe.service.CookingService;
import fr.antigaspi.recipe.service.CookingService;
import fr.antigaspi.recipe.service.RecipeChecker;
import fr.antigaspi.recipe.service.RecipeProperties;
import fr.antigaspi.recipe.service.RecipeService;
import fr.antigaspi.recipe.tool.RecipeTools.Proposition;
import fr.antigaspi.utensil.repository.InMemoryUtensilRepository;
import fr.antigaspi.utensil.service.UtensilService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RecipeToolsTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private static final List<Ingredient> INVENTORY = List.of(
            new Ingredient(2L, "Blancs de poulet", Category.VIANDES_POISSONS, new BigDecimal("400"), Unit.G, TODAY,
                    NOW),
            new Ingredient(3L, "Courgettes", Category.LEGUMES, new BigDecimal("3"), Unit.PIECE, TODAY.plusDays(2),
                    NOW));

    /** Contexte reçu par le faux générateur. */
    private final AtomicReference<RecipeContext> receivedContext = new AtomicReference<>();

    /** Réponse du faux générateur ; une exception est levée telle quelle. */
    private final AtomicReference<Object> llmAnswer = new AtomicReference<>(recipe(3L));

    private final IngredientService ingredientService = new IngredientService(
            new InMemoryIngredientRepository(INVENTORY.toArray(Ingredient[]::new)), Clock.fixed(NOW, ZoneOffset.UTC));

    /** Événements publiés par le tool. */
    private final List<Object> events = new java.util.ArrayList<>();

    private final RecipeTools tools = new RecipeTools(
            new RecipeService(ingredientService, new UtensilService(new InMemoryUtensilRepository()),
                    mock(ProfileService.class), new CookingService(ingredientService,
                            new InMemoryCookedRecipeRepository(), Clock.fixed(NOW, ZoneOffset.UTC)),
                    context -> {
                        receivedContext.set(context);
                        if (llmAnswer.get() instanceof RuntimeException e) {
                            throw e;
                        }
                        return (Recipe) llmAnswer.get();
                    }, new RecipeChecker(), new RecipeProperties(8, 2, 5)),
            events::add);

    private static Recipe recipe(Long courgettesId) {
        return new Recipe("Courgettes sautées", 2, 15, List.of("Poêle"),
                List.of(new RecipeIngredient(courgettesId, "Courgettes", new BigDecimal("2"), "pièce")),
                List.of("Faire sauter les courgettes."));
    }

    private List<Long> priorityIds() {
        return receivedContext.get().priorityIngredients().stream().map(IngredientView::ingredient).map(Ingredient::id)
                .toList();
    }

    @Test
    void sansParametre_recetteDesPlusUrgentsControlee() {
        llmAnswer.set(recipe(99L));

        Proposition proposition = tools.genererRecette(null, null, null, null);

        assertThat(priorityIds()).containsExactly(2L, 3L);
        assertThat(receivedContext.get().servings()).isEqualTo(2);
        assertThat(proposition.erreur()).isNull();
        assertThat(proposition.recette().ingredients().getFirst().ingredientId()).isNull();
        assertThat(proposition.problemes()).extracting(RecipeIssue::type)
                .containsExactly(RecipeIssueType.IDENTIFIANT_INCONNU);
    }

    @Test
    void parametres_transmisALaGeneration() {
        Proposition proposition = tools.genererRecette(List.of(3L), 4, 20, "végétarien");

        assertThat(priorityIds()).containsExactly(3L);
        assertThat(receivedContext.get().servings()).isEqualTo(4);
        assertThat(receivedContext.get().maxMinutes()).isEqualTo(20);
        assertThat(receivedContext.get().instructions()).isEqualTo("végétarien");
        assertThat(proposition.recette()).isEqualTo(recipe(3L));
        assertThat(proposition.problemes()).isEmpty();
    }

    @Test
    void demandeRefusee_erreurRenvoyeeAuLlmSansException() {
        Proposition portions = tools.genererRecette(null, 0, null, null);
        Proposition alimentInconnu = tools.genererRecette(List.of(99L), null, null, null);

        assertThat(portions.recette()).isNull();
        assertThat(portions.erreur()).contains("portions");
        assertThat(alimentInconnu.recette()).isNull();
        assertThat(alimentInconnu.erreur()).contains("Aucun aliment");
    }

    @Test
    void echecDuLlm_erreurRenvoyeeAuLlmSansException() {
        llmAnswer.set(new RecipeGenerationException("LLM indisponible"));

        Proposition proposition = tools.genererRecette(null, null, null, null);

        assertThat(proposition.recette()).isNull();
        assertThat(proposition.problemes()).isEmpty();
        assertThat(proposition.erreur()).isEqualTo("LLM indisponible");
    }

    @Test
    void contratExposeAuLlm_unSeulToolAuxParametresFacultatifs() {
        ToolCallback[] callbacks = ToolCallbacks.from(tools);

        assertThat(callbacks).extracting(c -> c.getToolDefinition().name()).containsExactly("genererRecette");
        JsonNode schema = JsonMapper.shared().readTree(callbacks[0].getToolDefinition().inputSchema());
        assertThat(schema.get("properties").propertyNames()).containsExactlyInAnyOrder("alimentIds", "personnes",
                "tempsMax", "demande");
        JsonNode required = schema.get("required");
        assertThat(required == null || required.isEmpty()).as("paramètres facultatifs").isTrue();
    }

    @Test
    void appelParLeLlmEnJson_parametresDeserialisesEtRecetteRenvoyee() {
        ToolCallback callback = Arrays.stream(ToolCallbacks.from(tools)).findFirst().orElseThrow();

        String result = callback.call("{\"alimentIds\": [3], \"demande\": \"v\\u00e9g\\u00e9tarien\"}");

        assertThat(priorityIds()).containsExactly(3L);
        assertThat(receivedContext.get().instructions()).isEqualTo("végétarien");
        assertThat(result).contains("Courgettes sautées");
    }

    @Test
    void recetteGeneree_evenementPublieAvecLaRecetteControlee() {
        llmAnswer.set(recipe(99L));

        Proposition proposition = tools.genererRecette(null, null, null, null);

        assertThat(events).singleElement().isInstanceOfSatisfying(RecipeGeneratedEvent.class, event -> {
            assertThat(event.generated().recipe()).isEqualTo(proposition.recette());
            assertThat(event.generated().issues()).isEqualTo(proposition.problemes());
        });
    }

    @Test
    void generationEnErreur_aucunEvenement() {
        tools.genererRecette(null, 0, null, null);
        llmAnswer.set(new RecipeGenerationException("LLM indisponible"));
        tools.genererRecette(null, null, null, null);

        assertThat(events).isEmpty();
    }
}
