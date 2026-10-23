package fr.antigaspi.chat.api;

import java.util.List;

import fr.antigaspi.recipe.api.RecipeResponse;

/**
 * Réponse de l'assistant telle que le front la reçoit.
 *
 * @param reply
 *            réponse finale
 * @param tools
 *            tools appelés pour ce message, dans l'ordre
 * @param recipes
 *            recettes générées pour ce message (même forme que {@code POST /api/recipes}), vide sinon
 */
public record ChatResponse(String reply, List<ToolCallResponse> tools, List<RecipeResponse> recipes) {

    /**
     * Appel d'un tool par le LLM.
     *
     * @param name
     *            nom du tool
     * @param arguments
     *            arguments JSON envoyés par le LLM
     */
    public record ToolCallResponse(String name, String arguments) {
    }
}
