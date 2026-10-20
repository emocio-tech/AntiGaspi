package fr.antigaspi.recipe.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.recipe.RecipeFixtures;
import fr.antigaspi.recipe.llm.RecipeGenerationException;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeIngredient;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.recipe.service.RecipeProperties;
import fr.antigaspi.recipe.service.RecipeService;

class RecipeDemoRunnerTest {

    private static final Recipe RECIPE = new Recipe("Gratin de courgettes", 2, 40, List.of("Four"),
            List.of(new RecipeIngredient(3L, "Courgettes", new BigDecimal("0.5"), "kg")),
            List.of("Couper", "Enfourner"));

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final RecipeService recipeService = mock(RecipeService.class);
    private final IngredientService ingredientService = new IngredientService(
            () -> List.of(new Ingredient(3L, "Courgettes", Category.LEGUMES, new BigDecimal("2.5"), Unit.KG,
                    RecipeFixtures.TODAY.plusDays(2), RecipeFixtures.NOW)),
            Clock.fixed(RecipeFixtures.NOW, ZoneOffset.UTC));

    private String run() {
        RecipeDemoRunner runner = new RecipeDemoRunner(ingredientService, recipeService, new RecipeProperties(8, 2),
                new PrintStream(output, true, StandardCharsets.UTF_8));
        runner.run(null);
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void afficheLesAlimentsUrgentsPuisLaRecetteEnJsonIndente() {
        when(recipeService.generate(any(RecipeRequest.class))).thenReturn(RECIPE);

        String printed = run();

        assertThat(printed).contains("🟠 [3] Courgettes : 2.5 kg, reste 2 jour(s)");
        assertThat(printed).contains("\"title\" : \"Gratin de courgettes\"", "\"preparationMinutes\" : 40",
                "\"ingredientId\" : 3");
        assertThat(printed.indexOf("Courgettes : 2.5 kg")).isLessThan(printed.indexOf("\"title\""));
    }

    @Test
    void echecDuLlm_afficheUnMessageSansPlanter() {
        when(recipeService.generate(any(RecipeRequest.class)))
                .thenThrow(new RecipeGenerationException("Échec de la génération de recette : accès refusé"));

        String printed = run();

        assertThat(printed).contains("Échec de la génération de recette : accès refusé");
    }
}
