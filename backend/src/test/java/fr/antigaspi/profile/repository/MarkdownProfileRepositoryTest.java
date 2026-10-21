package fr.antigaspi.profile.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.antigaspi.profile.model.UserProfile;

class MarkdownProfileRepositoryTest {

    @TempDir
    Path dir;

    private Path write(String fileName, String content) throws IOException {
        return Files.writeString(dir.resolve(fileName), content);
    }

    @Test
    void litLaSectionPreferencesEtIgnoreLesAutres() throws IOException {
        Path profile = write("profil.md", """
                # Profil
                ## Préférences
                - N'aime pas les plats épicés
                - Végétarien le midi
                ## Équipement
                - Four
                ## Historique des retours
                - 2026-10-19 · Curry de légumes · 👎 · « trop épicé »
                """);

        UserProfile result = new MarkdownProfileRepository(profile).load();

        assertThat(result.preferences()).containsExactly("N'aime pas les plats épicés", "Végétarien le midi");
    }

    @Test
    void sectionAbsente_listeVide() throws IOException {
        Path profile = write("profil.md", """
                ## Historique des retours
                - RAS
                """);

        assertThat(new MarkdownProfileRepository(profile).load().preferences()).isEmpty();
    }

    @Test
    void titreToleresSansAccentNiMajusculeEtPucesEtoile() throws IOException {
        Path profile = write("profil.md", """
                ##  PREFERENCES
                *   Sans gluten   \s
                texte libre ignoré
                -
                """);

        assertThat(new MarkdownProfileRepository(profile).load().preferences()).containsExactly("Sans gluten");
    }

    @Test
    void fichierAbsent_utiliseLeGabaritDuMemeDossier() throws IOException {
        write(MarkdownProfileRepository.TEMPLATE_FILE_NAME, """
                ## Préférences
                - Sans porc
                """);

        UserProfile result = new MarkdownProfileRepository(dir.resolve("profil.md")).load();

        assertThat(result.preferences()).containsExactly("Sans porc");
    }

    @Test
    void niFichierNiGabarit_profilVide() {
        assertThat(new MarkdownProfileRepository(dir.resolve("profil.md")).load()).isEqualTo(UserProfile.empty());
    }

    @Test
    void gabaritDuProjet_contientDesPreferences() {
        // Les tests Maven s'exécutent depuis backend/ : le gabarit est dans ../data
        UserProfile result = new MarkdownProfileRepository(Path.of("../data/absent.md")).load();

        assertThat(result.preferences()).isNotEmpty();
    }
}
