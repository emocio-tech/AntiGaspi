package fr.antigaspi.chat.api;

import java.util.List;

/**
 * Conversation en cours telle que le front la reçoit.
 *
 * @param messages
 *            questions et réponses, dans l'ordre
 */
public record HistoryResponse(List<Entry> messages) {

    /**
     * Message de la conversation.
     *
     * @param role
     *            {@code user} ou {@code assistant}
     * @param text
     *            contenu du message
     */
    public record Entry(String role, String text) {
    }
}
