package fr.antigaspi.profile.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import fr.antigaspi.profile.model.UserProfile;

/**
 * Profil utilisateur lu depuis un fichier Markdown. Si le fichier est absent, le gabarit {@value #TEMPLATE_FILE_NAME}
 * situé dans le même dossier est utilisé.
 * <p>
 * Le fichier est relu à chaque appel pour prendre en compte les modifications faites à la main. Seules les lignes de
 * liste ({@code -} ou {@code *}) de la section {@code ## Préférences} sont lues ; les ustensiles sont gérés à part
 * ({@code fr.antigaspi.utensil}).
 */
@Repository
public class MarkdownProfileRepository implements ProfileRepository {

    static final String TEMPLATE_FILE_NAME = "profil.example.md";

    private static final Logger log = LoggerFactory.getLogger(MarkdownProfileRepository.class);

    private final Path profilePath;

    /**
     * Constructeur Spring : le chemin est reçu en texte, car convertir directement en {@link Path} lit un chemin
     * relatif comme une ressource du serveur web et fait échouer le démarrage.
     */
    @Autowired
    public MarkdownProfileRepository(@Value("${antigaspi.profile.path}") String profilePath) {
        this(Path.of(profilePath));
    }

    public MarkdownProfileRepository(Path profilePath) {
        this.profilePath = profilePath;
    }

    @Override
    public UserProfile load() {
        Optional<Path> source = resolveSource();
        if (source.isEmpty()) {
            log.warn("Profil introuvable ({} ni gabarit {}) : profil vide utilisé", profilePath, TEMPLATE_FILE_NAME);
            return UserProfile.empty();
        }
        try {
            return parse(Files.readAllLines(source.get()));
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du profil impossible : " + source.get(), e);
        }
    }

    private Optional<Path> resolveSource() {
        if (Files.isRegularFile(profilePath)) {
            return Optional.of(profilePath);
        }
        Path template = profilePath.resolveSibling(TEMPLATE_FILE_NAME);
        return Files.isRegularFile(template) ? Optional.of(template) : Optional.empty();
    }

    private static UserProfile parse(List<String> lines) {
        List<String> preferences = new ArrayList<>();
        List<String> current = null;
        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.startsWith("## ")) {
                current = switch (normalize(line.substring(3))) {
                case "preferences" -> preferences;
                default -> null;
                };
            } else if (current != null && (line.startsWith("- ") || line.startsWith("* "))) {
                String item = line.substring(2).strip();
                if (!item.isEmpty()) {
                    current.add(item);
                }
            }
        }
        return new UserProfile(List.copyOf(preferences));
    }

    /** Titre de section en minuscules, sans accents ni espaces superflus : « Préférences » -> « preferences ». */
    private static String normalize(String title) {
        return Normalizer.normalize(title.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
