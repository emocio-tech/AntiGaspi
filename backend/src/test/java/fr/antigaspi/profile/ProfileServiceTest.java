package fr.antigaspi.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileServiceTest {

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

        UserProfile result = new ProfileService(profile).getProfile();

        assertThat(result.preferences()).containsExactly("N'aime pas les plats épicés", "Végétarien le midi");
    }

    @Test
    void sectionAbsente_listeVide() throws IOException {
        Path profile = write("profil.md", """
                ## Historique des retours
                - RAS
                """);

        assertThat(new ProfileService(profile).getProfile().preferences()).isEmpty();
    }

    @Test
    void titreToleresSansAccentNiMajusculeEtPucesEtoile() throws IOException {
        Path profile = write("profil.md", """
                ##  PREFERENCES
                *   Sans gluten   \s
                texte libre ignoré
                -
                """);

        assertThat(new ProfileService(profile).getProfile().preferences()).containsExactly("Sans gluten");
    }

    @Test
    void fichierAbsent_utiliseLeGabaritDuMemeDossier() throws IOException {
        write(ProfileService.TEMPLATE_FILE_NAME, """
                ## Préférences
                - Sans porc
                """);

        UserProfile result = new ProfileService(dir.resolve("profil.md")).getProfile();

        assertThat(result.preferences()).containsExactly("Sans porc");
    }

    @Test
    void niFichierNiGabarit_profilVide() {
        assertThat(new ProfileService(dir.resolve("profil.md")).getProfile()).isEqualTo(UserProfile.empty());
    }

    @Test
    void gabaritDuProjet_contientDesPreferences() {
        // Les tests Maven s'exécutent depuis backend/ : le gabarit est dans ../data
        UserProfile result = new ProfileService(Path.of("../data/absent.md")).getProfile();

        assertThat(result.preferences()).isNotEmpty();
    }
}
