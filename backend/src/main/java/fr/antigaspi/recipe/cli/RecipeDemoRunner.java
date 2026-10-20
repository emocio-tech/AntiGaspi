package fr.antigaspi.recipe.cli;

import java.io.PrintStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;

import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.recipe.llm.RecipeGenerationException;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.recipe.service.RecipeProperties;
import fr.antigaspi.recipe.service.RecipeService;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Démo en terminal (profil Spring {@code demo}) : affiche les aliments les plus urgents puis la recette générée par le
 * LLM au format JSON standardisé.
 */
@Component
@Profile("demo")
public class RecipeDemoRunner implements ApplicationRunner {

    private static final JsonMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private final IngredientService ingredientService;
    private final RecipeService recipeService;
    private final RecipeProperties properties;
    private final PrintStream out;

    @Autowired
    public RecipeDemoRunner(IngredientService ingredientService, RecipeService recipeService,
            RecipeProperties properties) {
        this(ingredientService, recipeService, properties, System.out);
    }

    RecipeDemoRunner(IngredientService ingredientService, RecipeService recipeService, RecipeProperties properties,
            PrintStream out) {
        this.ingredientService = ingredientService;
        this.recipeService = recipeService;
        this.properties = properties;
        this.out = out;
    }

    @Override
    public void run(ApplicationArguments args) {
        out.println();
        out.println("=== Aliments les plus urgents envoyés au LLM ===");
        ingredientService.findMostUrgent(properties.maxIngredients()).forEach(view -> out.println(line(view)));

        out.println();
        out.println("=== Génération de la recette… ===");
        try {
            Recipe recipe = recipeService.generate(RecipeRequest.mostUrgent());
            out.println(JSON.writeValueAsString(recipe));
        } catch (RecipeGenerationException | IllegalArgumentException e) {
            out.println(e.getMessage());
            Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(e);
            if (rootCause != e) {
                out.println("Cause : " + rootCause);
            }
        }
        out.println();
    }

    private static String line(IngredientView view) {
        Ingredient ingredient = view.ingredient();
        String days = view.daysLeft() < 0 ? "périmé depuis " + -view.daysLeft() + " jour(s)"
                : view.daysLeft() == 0 ? "à consommer aujourd'hui" : "reste " + view.daysLeft() + " jour(s)";
        return "%s [%d] %s : %s %s, %s%s".formatted(view.urgency().emoji(), ingredient.id(), ingredient.name(),
                ingredient.quantity().stripTrailingZeros().toPlainString(), ingredient.unit().symbol(), days,
                view.estimated() ? " (date estimée)" : "");
    }
}
