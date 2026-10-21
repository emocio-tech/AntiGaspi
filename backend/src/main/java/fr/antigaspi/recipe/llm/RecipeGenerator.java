package fr.antigaspi.recipe.llm;

import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.model.RecipeContext;

/**
 * Générateur de recettes. Isole le fournisseur de LLM : en changer ne touche que la configuration ou l'implémentation.
 */
public interface RecipeGenerator {

    /**
     * @throws RecipeGenerationException
     *             si le LLM échoue ou renvoie une réponse inexploitable
     */
    Recipe generate(RecipeContext context);
}
