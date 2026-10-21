package fr.antigaspi.chat.llm;

/**
 * Échec d'un échange avec l'assistant de chat (appel au LLM ou à un tool).
 */
public class ChatException extends RuntimeException {

    public ChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
