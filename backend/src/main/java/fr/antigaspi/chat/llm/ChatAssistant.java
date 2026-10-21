package fr.antigaspi.chat.llm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import fr.antigaspi.ingredient.tool.IngredientTools;
import fr.antigaspi.recipe.tool.RecipeTools;

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

    private static final Logger log = LoggerFactory.getLogger(ChatAssistant.class);

    private final ChatClient chatClient;

    private final ApplicationEventPublisher eventPublisher;

    public ChatAssistant(ChatClient.Builder chatClientBuilder, IngredientTools ingredientTools, RecipeTools recipeTools,
            ApplicationEventPublisher eventPublisher,
            @Value("classpath:prompts/chat-system.md") Resource systemPrompt) {
        this.eventPublisher = eventPublisher;
        ToolCallback[] observedTools = Arrays.stream(ToolCallbacks.from(ingredientTools, recipeTools))
                .map(tool -> (ToolCallback) new ObservedToolCallback(tool, this::notifyToolCall))
                .toArray(ToolCallback[]::new);
        this.chatClient = chatClientBuilder.defaultSystem(read(systemPrompt)).defaultTools((Object[]) observedTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build(),
                        ToolCallingAdvisor.builder().build(), new SimpleLoggerAdvisor())
                .build();
    }

    /** Journalise l'appel et publie un {@link ToolCalledEvent}, affiché par exemple par la démo de chat. */
    private void notifyToolCall(ToolCalledEvent event) {
        log.info("Appel du tool {} {}", event.name(), event.arguments());
        eventPublisher.publishEvent(event);
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

    /** Tool qui signale chaque appel avant de le déléguer au tool d'origine. */
    private record ObservedToolCallback(ToolCallback delegate, Consumer<ToolCalledEvent> observer)
            implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            observer.accept(describe(toolInput));
            return delegate.call(toolInput);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            observer.accept(describe(toolInput));
            return delegate.call(toolInput, toolContext);
        }

        private ToolCalledEvent describe(String toolInput) {
            return new ToolCalledEvent(delegate.getToolDefinition().name(), toolInput);
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
