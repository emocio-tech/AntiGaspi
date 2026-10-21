package fr.antigaspi.chat.llm;

/**
 * Événement Spring publié à chaque appel d'un tool par le LLM.
 *
 * @param name
 *            nom du tool
 * @param arguments
 *            arguments JSON envoyés par le LLM
 */
public record ToolCalledEvent(String name, String arguments) {
}
