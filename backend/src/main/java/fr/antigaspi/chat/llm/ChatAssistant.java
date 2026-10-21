package fr.antigaspi.chat.llm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.antigaspi.ingredient.tool.IngredientTools;
import fr.antigaspi.utensil.tool.UtensilTools;

/**
 * Assistant de chat anti-gaspillage : le LLM appelle les tools de l'application quand il en a besoin.
 * <p>
 * La conversation est gardée en mémoire (une seule conversation, en RAM). La boucle d'appel des tools est faite par le
 * {@link ChatClient} ({@link ToolCallingAdvisor}) ; la mémoire passe avant elle et ne garde que les questions et les
 * réponses finales.
 */
@Component
public class ChatAssistant {

    /** Mono-utilisateur : une seule conversation. */
    private static final String CONVERSATION_ID = "antigaspi";

    private final ChatClient chatClient;

    public ChatAssistant(ChatClient.Builder chatClientBuilder, IngredientTools ingredientTools,
            UtensilTools utensilTools, @Value("classpath:prompts/chat-system.md") Resource systemPrompt) {
        this.chatClient = chatClientBuilder.defaultSystem(read(systemPrompt))
                .defaultTools(ingredientTools, utensilTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build(),
                        ToolCallingAdvisor.builder().build())
                .build();
    }

    /**
     * @throws ChatException
     *             si le LLM échoue
     */
    public String reply(String message) {
        try {
            return chatClient.prompt().user(message)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID)).call().content();
        } catch (RuntimeException e) {
            throw new ChatException("Échec de la réponse de l'assistant : " + e.getMessage(), e);
        }
    }

    private static String read(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt système introuvable : " + resource.getDescription(), e);
        }
    }
}
