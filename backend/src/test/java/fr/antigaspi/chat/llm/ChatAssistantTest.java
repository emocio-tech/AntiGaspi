package fr.antigaspi.chat.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.core.io.ByteArrayResource;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.ingredient.tool.IngredientTools;
import fr.antigaspi.profile.service.ProfileService;
import fr.antigaspi.recipe.llm.RecipeGuidelines;
import fr.antigaspi.recipe.service.RecipeChecker;
import fr.antigaspi.recipe.service.RecipeProperties;
import fr.antigaspi.recipe.service.RecipeService;
import fr.antigaspi.recipe.tool.RecipeTools;
import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.repository.InMemoryUtensilRepository;
import fr.antigaspi.utensil.service.UtensilService;
import fr.antigaspi.utensil.tool.UtensilTools;

class ChatAssistantTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    private static final IngredientService INGREDIENTS = new IngredientService(() -> List.of(new Ingredient(3L,
            "Courgettes", Category.LEGUMES, new BigDecimal("2.5"), Unit.KG, LocalDate.of(2026, 10, 22), NOW)),
            Clock.fixed(NOW, ZoneOffset.UTC));

    private static final String GUIDELINES = """
            Méthode, dans cet ordre : REGLES
            Format de la recette : FORMAT
            Avant de répondre : FIN""";

    private static final RecipeTools RECIPE_TOOLS = new RecipeTools(
            new RecipeGuidelines(new ByteArrayResource(GUIDELINES.getBytes(StandardCharsets.UTF_8))),
            new RecipeService(INGREDIENTS, new UtensilService(new InMemoryUtensilRepository()),
                    mock(ProfileService.class), context -> null, new RecipeChecker(), new RecipeProperties(8, 2)));

    private static final UtensilTools UTENSIL_TOOLS = new UtensilTools(
            new UtensilService(new InMemoryUtensilRepository(new Utensil(1L, "Four"))));

    /** Prompts reçus par le faux modèle, dans l'ordre. */
    private final List<Prompt> prompts = new ArrayList<>();

    /** Événements publiés par l'assistant. */
    private final List<Object> events = new ArrayList<>();

    /** Faux modèle qui renvoie les réponses prévues, une par appel. */
    private ChatAssistant assistantAnswering(AssistantMessage... answers) {
        Deque<AssistantMessage> script = new ArrayDeque<>(List.of(answers));
        return assistant(prompt -> {
            prompts.add(prompt);
            return new ChatResponse(List.of(new Generation(script.removeFirst())));
        });
    }

    /** Faux modèle compatible tools, comme le vrai modèle : sans cela, le ChatClient n'envoie pas les tools. */
    private ChatAssistant assistant(Function<Prompt, ChatResponse> answer) {
        ChatModel chatModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return answer.apply(prompt);
            }

            @Override
            public ChatOptions getOptions() {
                return ToolCallingChatOptions.builder().build();
            }
        };
        return new ChatAssistant(ChatClient.builder(chatModel), new IngredientTools(INGREDIENTS), RECIPE_TOOLS,
                UTENSIL_TOOLS, events::add,
                new ByteArrayResource("PROMPT SYSTEME CHAT".getBytes(StandardCharsets.UTF_8)));
    }

    private static AssistantMessage toolCall(String arguments) {
        return AssistantMessage.builder().content("")
                .toolCalls(List.of(new ToolCall("appel-1", "function", "listerIngredients", arguments))).build();
    }

    private static List<String> texts(Prompt prompt, MessageType type) {
        return prompt.getInstructions().stream().filter(message -> message.getMessageType() == type)
                .map(Message::getText).toList();
    }

    @Test
    void appelleLeToolPuisRenvoieLaReponseFinale() {
        ChatAssistant assistant = assistantAnswering(toolCall("{}"),
                new AssistantMessage("Tu as 2.5 kg de courgettes à finir."));

        String reply = assistant.reply("Qu'est-ce que j'ai dans mon frigo ?");

        assertThat(reply).isEqualTo("Tu as 2.5 kg de courgettes à finir.");
        assertThat(prompts).hasSize(2);
        List<ToolResponseMessage> toolResponses = prompts.get(1).getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance).map(ToolResponseMessage.class::cast).toList();
        assertThat(toolResponses).singleElement()
                .satisfies(message -> assertThat(message.getResponses()).singleElement().satisfies(response -> {
                    assertThat(response.name()).isEqualTo("listerIngredients");
                    assertThat(response.responseData()).contains("Courgettes");
                }));
    }

    @Test
    void appelDUnTool_publieUnEvenementAvecSonNomEtSesArguments() {
        assistantAnswering(toolCall("{\"joursMax\":3}"), new AssistantMessage("Les courgettes."))
                .reply("Qu'est-ce qui périme bientôt ?");

        assertThat(events).containsExactly(new ToolCalledEvent("listerIngredients", "{\"joursMax\":3}"));
    }

    @Test
    void sansAppelDeTool_aucunEvenement() {
        assistantAnswering(new AssistantMessage("Bonjour !")).reply("Bonjour");

        assertThat(events).isEmpty();
    }

    @Test
    void envoieLePromptSystemeEtDeclareLesTools() {
        assistantAnswering(new AssistantMessage("Bonjour !")).reply("Bonjour");

        Prompt prompt = prompts.getFirst();
        assertThat(texts(prompt, MessageType.SYSTEM)).singleElement().asString().contains("PROMPT SYSTEME CHAT");
        assertThat(texts(prompt, MessageType.USER)).containsExactly("Bonjour");
        assertThat(prompt.getOptions()).isInstanceOfSatisfying(ToolCallingChatOptions.class,
                options -> assertThat(options.getToolCallbacks())
                        .extracting(callback -> callback.getToolDefinition().name())
                        .containsExactlyInAnyOrder("listerIngredients", "consignesRecette", "verifierRecette",
                                "listUtensils", "addUtensil", "removeUtensil"));
    }

    @Test
    void garderLaMemoireDeLaConversation() {
        ChatAssistant assistant = assistantAnswering(new AssistantMessage("Des courgettes."),
                new AssistantMessage("Ce sont des légumes."));

        assistant.reply("Qu'est-ce qui périme bientôt ?");
        assistant.reply("Et c'est quelle catégorie ?");

        Prompt second = prompts.get(1);
        assertThat(texts(second, MessageType.USER)).containsExactly("Qu'est-ce qui périme bientôt ?",
                "Et c'est quelle catégorie ?");
        assertThat(texts(second, MessageType.ASSISTANT)).containsExactly("Des courgettes.");
    }

    @Test
    void echecDuModele_leveChatExceptionAvecLaCause() {
        IllegalStateException failure = new IllegalStateException("LLM indisponible");
        ChatAssistant assistant = assistant(prompt -> {
            throw failure;
        });

        assertThatThrownBy(() -> assistant.reply("Bonjour")).isInstanceOf(ChatException.class).hasRootCause(failure);
    }
}
