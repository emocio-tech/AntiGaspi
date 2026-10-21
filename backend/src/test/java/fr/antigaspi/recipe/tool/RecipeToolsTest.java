package fr.antigaspi.recipe.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.model.RecipeIssueType;
import fr.antigaspi.profile.service.ProfileService;
import fr.antigaspi.recipe.service.RecipeChecker;
import fr.antigaspi.recipe.service.RecipeProperties;
import fr.antigaspi.recipe.service.RecipeService;
import fr.antigaspi.utensil.service.UtensilService;
import fr.antigaspi.recipe.llm.RecipeGuidelines;
import fr.antigaspi.recipe.tool.RecipeTools.Consigne;
import fr.antigaspi.recipe.tool.RecipeTools.Verification;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RecipeToolsTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private static final String SOURCE = """
            Intro.

            Méthode, dans cet ordre : REGLES-A
            Contraintes : REGLES-B

            Format de la recette : FORMAT-A

            Avant de répondre : FIN""";

    private final RecipeTools tools = new RecipeTools(
            new RecipeGuidelines(new ByteArrayResource(SOURCE.getBytes(StandardCharsets.UTF_8))),
            new RecipeService(
                    new IngredientService(() -> List.of(new Ingredient(3L, "Courgettes", Category.LEGUMES,
                            new BigDecimal("2.5"), Unit.KG, TODAY.plusDays(2), NOW)), Clock.fixed(NOW, ZoneOffset.UTC)),
                    new UtensilService(List::of), mock(ProfileService.class), context -> null, new RecipeChecker(),
                    new RecipeProperties(8, 2)));

    private static ToolCallback callback(RecipeTools tools, String name) {
        return Arrays.stream(ToolCallbacks.from(tools)).filter(c -> c.getToolDefinition().name().equals(name))
                .findFirst().orElseThrow();
    }

    private static Recipe recipe(Long courgettesId) {
        return new Recipe("Courgettes sautées", 2, 15, List.of("Poêle"),
                List.of(new RecipeIngredient(courgettesId, "Courgettes", new BigDecimal("2"), "pièce")),
                List.of("Faire sauter les courgettes."));
    }

    @Test
    void consignesRecette_renvoieLaSectionDemandee() {
        assertThat(tools.consignesRecette(Consigne.REGLES)).contains("REGLES-A", "REGLES-B").doesNotContain("FORMAT-A");
        assertThat(tools.consignesRecette(Consigne.FORMAT)).contains("FORMAT-A").doesNotContain("REGLES-A");
        assertThat(tools.consignesRecette(Consigne.TOUT)).contains("REGLES-A", "FORMAT-A");
    }

    @Test
    void consignesRecette_sansParametre_renvoieToutes() {
        assertThat(tools.consignesRecette(null)).contains("REGLES-A", "FORMAT-A");
    }

    @Test
    void verifierRecette_recetteConforme_aucunProbleme() {
        Verification verification = tools.verifierRecette(recipe(3L));

        assertThat(verification.problemes()).isEmpty();
        assertThat(verification.recette()).isEqualTo(recipe(3L));
    }

    @Test
    void verifierRecette_identifiantInconnu_renvoieLaRecetteCorrigeeEtLeProbleme() {
        Verification verification = tools.verifierRecette(recipe(99L));

        assertThat(verification.problemes()).extracting("type").containsExactly(RecipeIssueType.IDENTIFIANT_INCONNU);
        assertThat(verification.recette().ingredients().get(0).ingredientId()).isNull();
    }

    @Test
    void verifierRecette_precisePerimetreLimiteDeLaVerification() {
        Verification verification = tools.verifierRecette(recipe(3L));

        assertThat(verification.limites()).contains("quantités", "ustensiles", "sécurité alimentaire");
    }

    @Test
    void contratExposeAuLlm_deuxToolsEtParametres() {
        ToolCallback[] callbacks = ToolCallbacks.from(tools);
        assertThat(callbacks).extracting(c -> c.getToolDefinition().name())
                .containsExactlyInAnyOrder("consignesRecette", "verifierRecette");

        JsonNode consignes = schema(callback(tools, "consignesRecette"));
        assertThat(consignes.get("properties").get("consigne").get("enum").toString()).contains("REGLES", "FORMAT",
                "TOUT");
        JsonNode required = consignes.get("required");
        assertThat(required == null || required.isEmpty()).as("consigne facultative").isTrue();

        assertThat(schema(callback(tools, "verifierRecette")).get("required").toString()).contains("recette");
    }

    @Test
    void appelParLeLlmEnJson_verifierRecetteDeserialiseLaRecette() {
        String json = """
                {"recette":{"title":"Courgettes sautées","servings":2,"preparationMinutes":15,
                "utensils":["Poêle"],
                "ingredients":[{"ingredientId":99,"name":"Courgettes","quantity":2,"unit":"pièce"}],
                "steps":["Faire sauter les courgettes."]}}""";

        String result = callback(tools, "verifierRecette").call(json);

        assertThat(result).contains("IDENTIFIANT_INCONNU");
    }

    private static JsonNode schema(ToolCallback callback) {
        return JsonMapper.builder().build().readTree(callback.getToolDefinition().inputSchema());
    }
}
