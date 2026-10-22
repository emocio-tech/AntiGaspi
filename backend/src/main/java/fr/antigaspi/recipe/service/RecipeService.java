package fr.antigaspi.recipe.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.profile.service.ProfileService;
import fr.antigaspi.recipe.llm.RecipeGenerator;
import fr.antigaspi.recipe.model.DishType;
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeContext;
import fr.antigaspi.recipe.model.RecipeRequest;
import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.service.UtensilService;

/**
 * Génération d'une recette à partir de l'inventaire, des ustensiles disponibles et du profil utilisateur.
 */
@Service
public class RecipeService {

    /** Catégories qui ne peuvent pas être l'aliment principal d'un plat. */
    private static final Set<Category> NOT_MAIN_CATEGORIES = EnumSet.of(Category.CONDIMENTS, Category.BOISSONS,
            Category.EPICERIE_SECHE);

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
     *             si aucun aliment n'est disponible ou si les portions, le temps ou la variante sont invalides
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

        Integer variant = effectiveRequest.variant();
        if (variant != null && (variant < 0 || variant >= DishType.values().length)) {
            throw new IllegalArgumentException(
                    "La variante doit être comprise entre 0 et " + (DishType.values().length - 1) + " : " + variant);
        }

        List<IngredientView> inventory = ingredientService.listByUrgency();
        List<IngredientView> priority = selectPriority(inventory, effectiveRequest.ingredientIds());
        if (priority.isEmpty()) {
            throw new IllegalArgumentException("Aucun aliment disponible pour générer une recette");
        }
        List<IngredientView> others = inventory.stream().filter(view -> !priority.contains(view)).toList();
        List<Utensil> utensils = utensilService.listAll();
        IngredientView imposedMain = null;
        DishType imposedDishType = null;
        if (variant != null) {
            List<IngredientView> candidates = priority.stream()
                    .filter(view -> !NOT_MAIN_CATEGORIES.contains(view.ingredient().category())).toList();
            imposedMain = candidates.isEmpty() ? null : candidates.get(variant % candidates.size());
            imposedDishType = DishType.values()[variant];
        }
        Recipe recipe = recipeGenerator
                .generate(new RecipeContext(priority, others, utensils, profileService.getProfile(), servings,
                        maxMinutes, effectiveRequest.instructions(), imposedMain, imposedDishType));
        return recipeChecker.check(recipe, inventory.stream().map(IngredientView::ingredient).toList(), utensils);
    }

    /** Les aliments choisis, ou à défaut les plus urgents ; l'inventaire est déjà trié par urgence. */
    private List<IngredientView> selectPriority(List<IngredientView> inventory, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return inventory.stream().limit(properties.maxIngredients()).toList();
        }
        return inventory.stream().filter(view -> ids.contains(view.ingredient().id())).toList();
    }
}
