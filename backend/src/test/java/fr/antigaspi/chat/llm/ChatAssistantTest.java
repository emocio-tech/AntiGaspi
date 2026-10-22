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

import fr.antigaspi.chat.model.ChatEntry;
import fr.antigaspi.chat.model.ChatReply;
import fr.antigaspi.chat.model.ChatRole;
import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.ingredient.tool.IngredientTools;
import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.profile.service.ProfileService;
import fr.antigaspi.profile.tool.ProfileTools;
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

    private static final RecipeTools RECIPE_TOOLS = new RecipeTools(
            new RecipeService(INGREDIENTS, new UtensilService(new InMemoryUtensilRepository()),
                    mock(ProfileService.class), context -> null, new RecipeChecker(), new RecipeProperties(8, 2)));

    private static final UtensilTools UTENSIL_TOOLS = new UtensilTools(
            new UtensilService(new InMemoryUtensilRepository(new Utensil(1L, "Four"))));

    private static final ProfileTools PROFILE_TOOLS = new ProfileTools(
            new ProfileService(() -> new UserProfile(List.of("Végétarien"))));

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
        return new ChatAssistant(ChatClient.builder(chatModel),
                List.of(new IngredientTools(INGREDIENTS), RECIPE_TOOLS, UTENSIL_TOOLS, PROFILE_TOOLS), events::add,
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
    void argumentsAvecAccentsEchappes_evenementLisible() {
        // Certains proxys renvoient les arguments avec les accents échappés (\u00e9 pour é)
        assistantAnswering(toolCall("{\"categorie\": \"L\\u00c9GUMES\", \"joursMax\": 3}"),
                new AssistantMessage("Les courgettes.")).reply("Quels légumes ?");

        assertThat(events).containsExactly(
                new ToolCalledEvent("listerIngredients", "{\"categorie\":\"LÉGUMES\",\"joursMax\":3}"));
    }

    @Test
    void argumentsQuiNeSontPasDuJson_evenementAvecLeTexteBrut() {
        assistantAnswering(toolCall("pas du json"), new AssistantMessage("Les courgettes.")).reply("Quels légumes ?");

        assertThat(events).containsExactly(new ToolCalledEvent("listerIngredients", "pas du json"));
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
                        .containsExactlyInAnyOrder("listerIngredients", "genererRecette", "listUtensils", "addUtensil",
                                "removeUtensil", "consulterPreferences"));
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

    @Test
    void replyWithTools_renvoieLeTexteEtLesToolsAppelesPourCeMessageSeulement() {
        ChatAssistant assistant = assistantAnswering(toolCall("{\"joursMax\":3}"),
                new AssistantMessage("Les courgettes."), new AssistantMessage("Bonjour !"));

        ChatReply first = assistant.replyWithTools("Qu'est-ce qui périme bientôt ?");
        ChatReply second = assistant.replyWithTools("Bonjour");

        assertThat(first.text()).isEqualTo("Les courgettes.");
        assertThat(first.tools()).containsExactly(new ToolCalledEvent("listerIngredients", "{\"joursMax\":3}"));
        assertThat(second.text()).isEqualTo("Bonjour !");
        assertThat(second.tools()).isEmpty();
    }

    @Test
    void replyWithTools_messageVide_estRefuse_sansAppelerLeModele() {
        ChatAssistant assistant = assistantAnswering(new AssistantMessage("Bonjour !"));

        assertThatThrownBy(() -> assistant.replyWithTools("   ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le message est vide");
        assertThatThrownBy(() -> assistant.replyWithTools(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Le message est vide");
        assertThat(prompts).isEmpty();
    }

    @Test
    void replyWithTools_echecDuModele_leveChatException() {
        ChatAssistant assistant = assistant(prompt -> {
            throw new IllegalStateException("LLM indisponible");
        });

        assertThatThrownBy(() -> assistant.replyWithTools("Bonjour")).isInstanceOf(ChatException.class);
    }

    @Test
    void reset_faitOublierLesEchangesPrecedents() {
        ChatAssistant assistant = assistantAnswering(new AssistantMessage("Des courgettes."),
                new AssistantMessage("Bonjour !"));
        assistant.reply("Qu'est-ce qui périme bientôt ?");

        assistant.reset();
        assistant.reply("Bonjour");

        assertThat(texts(prompts.get(1), MessageType.USER)).containsExactly("Bonjour");
        assertThat(texts(prompts.get(1), MessageType.ASSISTANT)).isEmpty();
    }

    @Test
    void history_listeQuestionsEtReponsesDansLOrdre_sansLesMessagesDeTools() {
        ChatAssistant assistant = assistantAnswering(toolCall("{}"), new AssistantMessage("Des courgettes."),
                new AssistantMessage("Ce sont des légumes."));
        assertThat(assistant.history()).isEmpty();

        assistant.reply("Qu'est-ce qui périme bientôt ?");
        assistant.reply("Et c'est quelle catégorie ?");

        assertThat(assistant.history()).containsExactly(new ChatEntry(ChatRole.USER, "Qu'est-ce qui périme bientôt ?"),
                new ChatEntry(ChatRole.ASSISTANT, "Des courgettes."),
                new ChatEntry(ChatRole.USER, "Et c'est quelle catégorie ?"),
                new ChatEntry(ChatRole.ASSISTANT, "Ce sont des légumes."));
    }

    @Test
    void history_apresReset_estVide() {
        ChatAssistant assistant = assistantAnswering(new AssistantMessage("Bonjour !"));
        assistant.reply("Bonjour");

        assistant.reset();

        assertThat(assistant.history()).isEmpty();
    }
}
