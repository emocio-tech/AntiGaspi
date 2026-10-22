package fr.antigaspi.profile.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test
    void save_remplaceLesPreferences_etGardeLeResteDuFichier() throws IOException {
        Path profile = write("profil.md", """
                # Profil

                ## Préférences
                - N'aime pas les plats épicés
                - Végétarien le midi

                ## Historique des retours
                - 2026-10-19 · Curry de légumes · 👎
                """);

        new MarkdownProfileRepository(profile).save(new UserProfile(List.of("Sans gluten", "Pas de coriandre")));

        assertThat(Files.readString(profile)).isEqualTo("""
                # Profil

                ## Préférences
                - Sans gluten
                - Pas de coriandre

                ## Historique des retours
                - 2026-10-19 · Curry de légumes · 👎
                """);
    }

    @Test
    void save_fichierAbsent_partDuGabarit() throws IOException {
        Path template = write(MarkdownProfileRepository.TEMPLATE_FILE_NAME, """
                # Profil

                ## Préférences
                - n'est pas difficile

                ## Historique des retours
                """);
        Path profile = dir.resolve("profil.md");

        new MarkdownProfileRepository(profile).save(new UserProfile(List.of("Vegan")));

        assertThat(new MarkdownProfileRepository(profile).load().preferences()).containsExactly("Vegan");
        assertThat(Files.readString(profile)).contains("## Historique des retours");
        assertThat(Files.readString(template)).contains("n'est pas difficile");
    }

    @Test
    void save_niFichierNiGabarit_creeUnProfilMinimal() throws IOException {
        Path profile = dir.resolve("profil.md");

        new MarkdownProfileRepository(profile).save(new UserProfile(List.of("Vegan")));

        assertThat(Files.readString(profile)).startsWith("# Profil");
        assertThat(new MarkdownProfileRepository(profile).load().preferences()).containsExactly("Vegan");
    }

    @Test
    void save_sectionAbsente_laCreeSansPerdreLeReste() throws IOException {
        Path profile = write("profil.md", """
                # Profil

                ## Historique des retours
                - 2026-10-19 · Curry de légumes · 👎
                """);

        new MarkdownProfileRepository(profile).save(new UserProfile(List.of("Sans porc")));

        assertThat(new MarkdownProfileRepository(profile).load().preferences()).containsExactly("Sans porc");
        assertThat(Files.readString(profile)).contains("- 2026-10-19 · Curry de légumes · 👎");
    }

    @Test
    void save_listeVide_gardeLaSectionSansElement() throws IOException {
        Path profile = write("profil.md", """
                ## Préférences
                - Sans porc
                """);

        new MarkdownProfileRepository(profile).save(UserProfile.empty());

        assertThat(Files.readString(profile)).contains("## Préférences");
        assertThat(new MarkdownProfileRepository(profile).load().preferences()).isEmpty();
    }

    @Test
    void save_puisLoad_redonneLaMemeListe_avecAccentsEtGuillemets() {
        Path profile = dir.resolve("profil.md");
        List<String> preferences = List.of("N'aime pas les plats épicés", "« Végétarien » le midi", "Œufs : ok");

        new MarkdownProfileRepository(profile).save(new UserProfile(preferences));

        assertThat(new MarkdownProfileRepository(profile).load().preferences()).containsExactlyElementsOf(preferences);
    }
}
