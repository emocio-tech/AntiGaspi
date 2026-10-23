package fr.antigaspi.chat.model;

import java.util.List;

import fr.antigaspi.chat.llm.ToolCalledEvent;
import fr.antigaspi.recipe.model.GeneratedRecipe;

/**
 * Réponse de l'assistant à un message.
 *
 * @param text
 *            réponse finale
 * @param tools
 *            tools appelés par le LLM pour répondre à ce message seulement, dans l'ordre
 * @param recipes
 *            recettes générées pour ce message seulement, dans l'ordre ; vide sinon
 */
public record ChatReply(String text, List<ToolCalledEvent> tools, List<GeneratedRecipe> recipes) {
}
