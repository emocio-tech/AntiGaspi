package fr.antigaspi.recipe;

/**
 * Échec de la génération d'une recette (appel au LLM ou réponse inexploitable).
 */
public class RecipeGenerationException extends RuntimeException {

    public RecipeGenerationException(String message) {
        super(message);
    }

    public RecipeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
