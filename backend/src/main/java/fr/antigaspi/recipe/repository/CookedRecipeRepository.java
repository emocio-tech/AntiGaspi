package fr.antigaspi.recipe.repository;

import fr.antigaspi.recipe.model.CookedRecipe;

/** Enregistrement des recettes cuisinées, implémenté par {@link JpaCookedRecipeRepository}. */
public interface CookedRecipeRepository {

    /** Insère la recette cuisinée (identifiant nul) ; renvoie la recette enregistrée avec son identifiant. */
    CookedRecipe save(CookedRecipe recipe);
}
