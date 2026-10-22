package fr.antigaspi.chat.model;

/**
 * Message de la conversation : une question de l'utilisateur ou une réponse finale de l'assistant.
 */
public record ChatEntry(ChatRole role, String text) {
}
