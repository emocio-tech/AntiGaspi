package fr.antigaspi.recipe.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.profile.service.ProfileService;
import fr.antigaspi.recipe.llm.RecipeGenerator;
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeContext;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.utensil.service.UtensilService;

/**
 * Génération d'une recette à partir de l'inventaire, des ustensiles disponibles et du profil utilisateur.
 */
@Service
public class RecipeService {

    private final IngredientService ingredientService;
    private final UtensilService utensilService;
    private final ProfileService profileService;
    private final RecipeGenerator recipeGenerator;
    private final RecipeChecker recipeChecker;
    private final RecipeProperties properties;

    public RecipeService(IngredientService ingredientService, UtensilService utensilService,
            ProfileService profileService, RecipeGenerator recipeGenerator, RecipeChecker recipeChecker,
            RecipeProperties properties) {
        this.ingredientService = ingredientService;
        this.utensilService = utensilService;
        this.profileService = profileService;
        this.recipeGenerator = recipeGenerator;
        this.recipeChecker = recipeChecker;
        this.properties = properties;
    }

    /**
     * @param request
     *            {@code null} équivaut à {@link RecipeRequest#mostUrgent()}
     *
     * @throws IllegalArgumentException
     *             si aucun aliment n'est disponible ou si les portions ou le temps sont invalides
     * @throws RecipeGenerationException
     *             si le LLM échoue
     */
    public GeneratedRecipe generate(RecipeRequest request) {
        RecipeRequest effectiveRequest = request == null ? RecipeRequest.mostUrgent() : request;
        int servings = effectiveRequest.servings() == null ? properties.defaultServings() : effectiveRequest.servings();
        if (servings <= 0) {
            throw new IllegalArgumentException("Le nombre de portions doit être positif : " + servings);
        }
        Integer maxMinutes = effectiveRequest.maxMinutes();
        if (maxMinutes != null && maxMinutes <= 0) {
            throw new IllegalArgumentException("Le temps maximal doit être positif : " + maxMinutes);
        }

        List<IngredientView> inventory = ingredientService.listByUrgency();
        List<IngredientView> priority = selectPriority(inventory, effectiveRequest.ingredientIds());
        if (priority.isEmpty()) {
            throw new IllegalArgumentException("Aucun aliment disponible pour générer une recette");
        }
        List<IngredientView> others = inventory.stream().filter(view -> !priority.contains(view)).toList();
        Recipe recipe = recipeGenerator.generate(new RecipeContext(priority, others, utensilService.listAll(),
                profileService.getProfile(), servings, maxMinutes));
        return recipeChecker.check(recipe, inventory.stream().map(IngredientView::ingredient).toList());
    }

    /**
     * Applique le garde-fou à une recette rédigée ailleurs (par exemple par le LLM du chat), en la comparant à
     * l'inventaire complet.
     */
    public GeneratedRecipe check(Recipe recipe) {
        return recipeChecker.check(recipe,
                ingredientService.listByUrgency().stream().map(IngredientView::ingredient).toList());
    }

    /** Les aliments choisis, ou à défaut les plus urgents ; l'inventaire est déjà trié par urgence. */
    private List<IngredientView> selectPriority(List<IngredientView> inventory, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return inventory.stream().limit(properties.maxIngredients()).toList();
        }
        return inventory.stream().filter(view -> ids.contains(view.ingredient().id())).toList();
    }
}
