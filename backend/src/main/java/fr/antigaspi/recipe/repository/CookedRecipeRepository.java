package fr.antigaspi.recipe.repository;

import java.util.List;
import java.util.Optional;

import fr.antigaspi.recipe.model.CookedRecipe;

/** Enregistrement des recettes cuisinées, implémenté par {@link JpaCookedRecipeRepository}. */
public interface CookedRecipeRepository {

    /** Insère la recette cuisinée (identifiant nul) ; renvoie la recette enregistrée avec son identifiant. */
    CookedRecipe save(CookedRecipe recipe);

    /** Recette cuisinée d'identifiant donné, vide si elle n'existe pas. */
    Optional<CookedRecipe> findById(long id);

    /** Toutes les recettes cuisinées, de la plus récente à la plus ancienne ({@code cookedAt}, puis identifiant). */
    List<CookedRecipe> findAllMostRecentFirst();

    /** Les {@code limit} recettes cuisinées les plus récentes, de la plus récente à la plus ancienne. */
    List<CookedRecipe> findMostRecent(int limit);
}
