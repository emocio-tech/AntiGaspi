package fr.antigaspi.chat.llm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import fr.antigaspi.chat.model.ChatEntry;
import fr.antigaspi.chat.model.ChatReply;
import fr.antigaspi.chat.model.ChatRole;
import fr.antigaspi.common.ChatTools;
import fr.antigaspi.common.Resources;
import fr.antigaspi.recipe.model.GeneratedRecipe;
import fr.antigaspi.recipe.tool.RecipeGeneratedEvent;

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

    private final ChatMemory memory = MessageWindowChatMemory.builder().build();

    /**
     * Tools appelés pendant la réponse en cours sur ce thread (le {@link ToolCallingAdvisor} les exécute sur le thread
     * qui répond), {@code null} hors de {@link #replyWithTools(String)}.
     */
    private final ThreadLocal<List<ToolCalledEvent>> currentCalls = new ThreadLocal<>();

    /** Recettes générées pendant la réponse en cours sur ce thread, {@code null} hors de {@link #replyWithTools}. */
    private final ThreadLocal<List<GeneratedRecipe>> currentRecipes = new ThreadLocal<>();

    private static final Logger log = LoggerFactory.getLogger(ChatAssistant.class);

    private final ChatClient chatClient;

    private final ApplicationEventPublisher eventPublisher;

    public ChatAssistant(ChatClient.Builder chatClientBuilder, List<ChatTools> tools,
            ApplicationEventPublisher eventPublisher,
            @Value("classpath:prompts/chat-system.md") Resource systemPrompt) {
        this.eventPublisher = eventPublisher;
        ToolCallback[] observedTools = Arrays.stream(ToolCallbacks.from(tools.toArray()))
                .map(tool -> (ToolCallback) new ObservedToolCallback(tool, this::notifyToolCall))
                .toArray(ToolCallback[]::new);
        this.chatClient = chatClientBuilder.defaultSystem(Resources.readText(systemPrompt))
                .defaultTools((Object[]) observedTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build(), ToolCallingAdvisor.builder().build(),
                        new SimpleLoggerAdvisor())
                .build();
    }

    /** Journalise l'appel et publie un {@link ToolCalledEvent}, affiché par exemple par la démo de chat. */
    private void notifyToolCall(ToolCalledEvent event) {
        List<ToolCalledEvent> calls = currentCalls.get();
        if (calls != null) {
            calls.add(event);
        }
        log.info("Appel du tool {} {}", event.name(), event.arguments());
        eventPublisher.publishEvent(event);
    }

    /**
     * @throws ChatException
     *             si le LLM échoue
     */
    public synchronized String reply(String message) {
        try {
            return chatClient.prompt().user(message)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID)).call().content();
        } catch (RuntimeException e) {
            throw new ChatException("Échec de la réponse de l'assistant : " + e.getMessage(), e);
        }
    }

    /**
     * Comme {@link #reply(String)}, avec les tools que le LLM a appelés pour ce message.
     *
     * @throws IllegalArgumentException
     *             si le message est vide
     * @throws ChatException
     *             si le LLM échoue
     */
    public synchronized ChatReply replyWithTools(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Le message est vide");
        }
        List<ToolCalledEvent> calls = new ArrayList<>();
        List<GeneratedRecipe> recipes = new ArrayList<>();
        currentCalls.set(calls);
        currentRecipes.set(recipes);
        try {
            String text = reply(message);
            return new ChatReply(text, List.copyOf(calls), List.copyOf(recipes));
        } finally {
            currentCalls.remove();
            currentRecipes.remove();
        }
    }

    /** Garde la recette générée par {@code genererRecette} pour la réponse en cours ; ignorée hors d'une réponse. */
    @EventListener
    public void onRecipeGenerated(RecipeGeneratedEvent event) {
        List<GeneratedRecipe> recipes = currentRecipes.get();
        if (recipes != null) {
            recipes.add(event.generated());
        }
    }

    /** Les questions et réponses de la conversation, dans l'ordre. */
    public synchronized List<ChatEntry> history() {
        return memory.get(CONVERSATION_ID).stream()
                .filter(message -> message.getMessageType() == MessageType.USER
                        || message.getMessageType() == MessageType.ASSISTANT)
                .map(message -> new ChatEntry(
                        message.getMessageType() == MessageType.USER ? ChatRole.USER : ChatRole.ASSISTANT,
                        message.getText()))
                .toList();
    }

    /** Efface la conversation : l'assistant repart de zéro. */
    public synchronized void reset() {
        memory.clear(CONVERSATION_ID);
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
            return new ToolCalledEvent(delegate.getToolDefinition().name(), readable(toolInput));
        }

        /**
         * Arguments réécrits en JSON compact, accents en clair : certains proxys les échappent ({@code \u00e9}). Texte
         * brut s'il ne s'agit pas de JSON.
         */
        private static String readable(String toolInput) {
            try {
                return JsonMapper.shared().writeValueAsString(JsonMapper.shared().readTree(toolInput));
            } catch (JacksonException e) {
                return toolInput;
            }
        }
    }

}
