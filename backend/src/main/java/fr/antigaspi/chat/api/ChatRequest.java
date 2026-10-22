package fr.antigaspi.chat.api;

/**
 * Corps de {@code POST /api/chat}.
 *
 * @param message
 *            message de l'utilisateur
 */
public record ChatRequest(String message) {
}
