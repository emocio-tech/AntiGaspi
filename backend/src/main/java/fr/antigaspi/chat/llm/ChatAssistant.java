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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private volatile Consumer<String> toolCallListener = call -> {
    };

    @Autowired
    public ChatAssistant(ChatClient.Builder chatClientBuilder, IngredientTools ingredientTools,
            RecipeTools recipeTools, @Value("classpath:prompts/chat-system.md") Resource systemPrompt) {
        this(chatClientBuilder, systemPrompt, ingredientTools, recipeTools);
    }

    /** Assistant limité à l'inventaire, sans les tools de recette. */
    ChatAssistant(ChatClient.Builder chatClientBuilder, IngredientTools ingredientTools, Resource systemPrompt) {
        this(chatClientBuilder, systemPrompt, ingredientTools);
    }

    private ChatAssistant(ChatClient.Builder chatClientBuilder, Resource systemPrompt, Object... tools) {
        ToolCallback[] observedTools = Arrays.stream(ToolCallbacks.from(tools))
                .map(tool -> (ToolCallback) new ObservedToolCallback(tool, this::notifyToolCall))
                .toArray(ToolCallback[]::new);
        this.chatClient = chatClientBuilder.defaultSystem(read(systemPrompt)).defaultToolCallbacks(observedTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build(),
                        ToolCallingAdvisor.builder().build(), new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * Enregistre l'écouteur prévenu à chaque appel d'un tool par le LLM, avec le nom du tool et ses arguments JSON.
     */
    public void onToolCall(Consumer<String> listener) {
        this.toolCallListener = listener;
    }

    private void notifyToolCall(String call) {
        log.info("Appel du tool {}", call);
        toolCallListener.accept(call);
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
    private record ObservedToolCallback(ToolCallback delegate, Consumer<String> observer) implements ToolCallback {

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

        private String describe(String toolInput) {
            return delegate.getToolDefinition().name() + " " + toolInput;
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
