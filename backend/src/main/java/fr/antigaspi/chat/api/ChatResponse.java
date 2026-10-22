package fr.antigaspi.chat.api;

import java.util.List;

/**
 * Réponse de l'assistant telle que le front la reçoit.
 *
 * @param reply
 *            réponse finale
 * @param tools
 *            tools appelés pour ce message, dans l'ordre
 */
public record ChatResponse(String reply, List<ToolCallResponse> tools) {

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
