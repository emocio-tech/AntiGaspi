package fr.antigaspi.chat.model;

import java.util.List;

import fr.antigaspi.chat.llm.ToolCalledEvent;

/**
 * Réponse de l'assistant à un message.
 *
 * @param text
 *            réponse finale
 * @param tools
 *            tools appelés par le LLM pour répondre à ce message seulement, dans l'ordre
 */
public record ChatReply(String text, List<ToolCalledEvent> tools) {
}
