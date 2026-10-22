package fr.antigaspi.chat.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.antigaspi.chat.llm.ChatAssistant;
import fr.antigaspi.chat.llm.ChatException;
import fr.antigaspi.chat.llm.ToolCalledEvent;
import fr.antigaspi.chat.model.ChatEntry;
import fr.antigaspi.chat.model.ChatReply;
import fr.antigaspi.chat.model.ChatRole;

/**
 * API REST du chat : {@link ChatAssistant} est simulé, donc aucun appel au LLM.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatAssistant assistant;

    private static String json(String message) {
        return "{\"message\":" + message + "}";
    }

    @Test
    void post_renvoieLaReponseEtLesToolsAppeles() throws Exception {
        when(assistant.replyWithTools("Qu'est-ce qui périme ?")).thenReturn(new ChatReply("Les courgettes.",
                List.of(new ToolCalledEvent("listerIngredients", "{\"joursMax\":3}"))));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(json("\"Qu'est-ce qui périme ?\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reply").value("Les courgettes."))
                .andExpect(jsonPath("$.tools.length()").value(1))
                .andExpect(jsonPath("$.tools[0].name").value("listerIngredients"))
                .andExpect(jsonPath("$.tools[0].arguments").value("{\"joursMax\":3}"));
    }

    @Test
    void post_sansToolAppele_renvoieUneListeVide() throws Exception {
        when(assistant.replyWithTools("Bonjour")).thenReturn(new ChatReply("Bonjour !", List.of()));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(json("\"Bonjour\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tools").isEmpty());
    }

    @Test
    void post_messageRefuse_renvoie400AvecLeMessage() throws Exception {
        when(assistant.replyWithTools(" ")).thenThrow(new IllegalArgumentException("Le message est vide"));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(json("\" \"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Le message est vide"));
    }

    @Test
    void post_sansCorps_renvoie400() throws Exception {
        mvc.perform(post("/api/chat")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void post_echecDuLlm_renvoie502AvecUnMessage() throws Exception {
        when(assistant.replyWithTools("Bonjour"))
                .thenThrow(new ChatException("Échec de la réponse de l'assistant : LLM indisponible", null));

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(json("\"Bonjour\"")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Échec de la réponse de l'assistant : LLM indisponible"));
    }

    @Test
    void get_renvoieLHistoriqueAvecLesRolesEnMinuscules() throws Exception {
        when(assistant.history()).thenReturn(List.of(new ChatEntry(ChatRole.USER, "Bonjour"),
                new ChatEntry(ChatRole.ASSISTANT, "Bonjour !")));

        mvc.perform(get("/api/chat")).andExpect(status().isOk()).andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].text").value("Bonjour"))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"));
    }

    @Test
    void get_aucunEchange_renvoieUneListeVide() throws Exception {
        when(assistant.history()).thenReturn(List.of());

        mvc.perform(get("/api/chat")).andExpect(status().isOk()).andExpect(jsonPath("$.messages").isEmpty());
    }

    @Test
    void delete_efface_laConversation_etRenvoie204() throws Exception {
        mvc.perform(delete("/api/chat")).andExpect(status().isNoContent());

        verify(assistant).reset();
    }
}
