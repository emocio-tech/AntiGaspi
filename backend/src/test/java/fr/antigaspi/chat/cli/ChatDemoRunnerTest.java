package fr.antigaspi.chat.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import fr.antigaspi.chat.llm.ChatAssistant;
import fr.antigaspi.chat.llm.ChatException;

class ChatDemoRunnerTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ChatAssistant assistant = mock(ChatAssistant.class);

    private String run(String input) {
        ChatDemoRunner runner = new ChatDemoRunner(assistant, new BufferedReader(new StringReader(input)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
        runner.run(null);
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void afficheLaReponseDeLAssistant() {
        when(assistant.reply("Qu'est-ce qui périme ?")).thenReturn("Les courgettes.");

        String printed = run("Qu'est-ce qui périme ?\nquitter\n");

        assertThat(printed).contains("Vous > ", "AntiGaspi > Les courgettes.");
    }

    @Test
    void quitter_arreteSansAppelerLAssistant() {
        run("quitter\nQuestion jamais posée\n");

        verifyNoInteractions(assistant);
    }

    @Test
    void exit_arreteAussi() {
        run("EXIT\nQuestion jamais posée\n");

        verifyNoInteractions(assistant);
    }

    @Test
    void finDeLEntree_sortieSansErreur() {
        when(assistant.reply("Bonjour")).thenReturn("Salut !");

        String printed = run("Bonjour");

        verify(assistant).reply("Bonjour");
        assertThat(printed).contains("AntiGaspi > Salut !");
    }

    @Test
    void ligneVide_ignoree() {
        when(assistant.reply("Bonjour")).thenReturn("Salut !");

        run("\n   \nBonjour\n");

        verify(assistant).reply("Bonjour");
        verifyNoMoreInteractions(assistant);
    }

    @Test
    void echecDeLAssistant_afficheLeMessageEtContinue() {
        when(assistant.reply("Première")).thenThrow(new ChatException("LLM indisponible", null));
        when(assistant.reply("Seconde")).thenReturn("Me revoilà.");

        String printed = run("Première\nSeconde\n");

        assertThat(printed).contains("LLM indisponible", "AntiGaspi > Me revoilà.");
    }
}
