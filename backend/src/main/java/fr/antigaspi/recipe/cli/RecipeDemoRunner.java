package fr.antigaspi.recipe.cli;

import java.io.PrintStream;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.RecipeIssue;
import fr.antigaspi.recipe.model.RecipeIssueType;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.recipe.service.RecipeProperties;
import fr.antigaspi.recipe.service.RecipeService;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Démo en terminal (profil Spring {@code demo}) : affiche les aliments les plus urgents puis {@value #NOMBRE_RECETTES}
 * recettes générées successivement par le LLM au format JSON standardisé, avec un bilan des échecs.
 */
@Component
@Profile("demo")
public class RecipeDemoRunner implements ApplicationRunner {

    /** Nombre de générations enchaînées, pour repérer les problèmes récurrents du LLM. */
    private static final int NOMBRE_RECETTES = 10;

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
        out.println("=== Aliments prioritaires envoyés au LLM (le reste de l'inventaire est fourni en complément) ===");
        ingredientService.search(null, null, properties.maxIngredients()).forEach(view -> out.println(line(view)));

        Map<String, Integer> echecs = new LinkedHashMap<>();
        Map<RecipeIssueType, Integer> anomalies = new EnumMap<>(RecipeIssueType.class);
        int reussies = 0;
        for (int i = 1; i <= NOMBRE_RECETTES; i++) {
            out.println();
            out.println("=== Recette " + i + "/" + NOMBRE_RECETTES + " ===");
            try {
                GeneratedRecipe generated = recipeService.generate(RecipeRequest.mostUrgent());
                out.println(JSON.writeValueAsString(generated.recipe()));
                for (RecipeIssue issue : generated.issues()) {
                    out.println("⚠ " + issue.type().label() + " : " + issue.message());
                    anomalies.merge(issue.type(), 1, Integer::sum);
                }
                reussies++;
            } catch (RecipeGenerationException | IllegalArgumentException e) {
                out.println(e.getMessage());
                Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(e);
                if (rootCause != e) {
                    out.println("Cause : " + rootCause);
                }
                echecs.merge(e.getMessage(), 1, Integer::sum);
            }
        }

        out.println();
        out.println("=== Bilan ===");
        out.println("Bilan : %d réussie(s), %d échec(s) sur %d".formatted(reussies, NOMBRE_RECETTES - reussies,
                NOMBRE_RECETTES));
        echecs.forEach((message, nombre) -> out.println("  " + nombre + " × " + message));
        out.println("Anomalies : " + anomalies.values().stream().mapToInt(Integer::intValue).sum());
        anomalies.forEach((type, nombre) -> out.println("  " + nombre + " × " + type.label()));
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
