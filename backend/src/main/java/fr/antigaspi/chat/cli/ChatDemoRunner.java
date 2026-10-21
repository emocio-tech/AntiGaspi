package fr.antigaspi.chat.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fr.antigaspi.chat.llm.ChatAssistant;
import fr.antigaspi.chat.llm.ChatException;

/**
 * Démo de chat en terminal (profil Spring {@code chat}) : chaque ligne saisie est envoyée à l'assistant, qui consulte
 * l'inventaire par ses tools quand il en a besoin. {@code quitter} ou {@code exit} termine la conversation.
 */
@Component
@Profile("chat")
public class ChatDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChatDemoRunner.class);

    private static final Set<String> COMMANDES_SORTIE = Set.of("quitter", "exit");

    private final ChatAssistant assistant;
    private final BufferedReader in;
    private final PrintStream out;

    @Autowired
    public ChatDemoRunner(ChatAssistant assistant) {
        this(assistant, new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), System.out);
        assistant.onToolCall(call -> out.println("  ⚙ " + call));
    }

    ChatDemoRunner(ChatAssistant assistant, BufferedReader in, PrintStream out) {
        this.assistant = assistant;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run(ApplicationArguments args) {
        out.println();
        out.println("=== Chat AntiGaspi : posez vos questions sur l'inventaire (« quitter » pour sortir) ===");
        String line;
        while ((line = prompt()) != null) {
            String message = line.strip();
            if (COMMANDES_SORTIE.contains(message.toLowerCase())) {
                break;
            }
            if (message.isEmpty()) {
                continue;
            }
            log.info("Vous > {}", message);
            try {
                String reponse = assistant.reply(message);
                log.info("AntiGaspi > {}", reponse);
                out.println("AntiGaspi > " + reponse);
            } catch (ChatException e) {
                log.warn("Erreur du chat : {}", e.getMessage());
                out.println(e.getMessage());
            }
        }
        out.println();
    }

    /** Affiche l'invite et lit la ligne suivante, {@code null} en fin d'entrée. */
    private String prompt() {
        out.print("Vous > ");
        out.flush();
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
