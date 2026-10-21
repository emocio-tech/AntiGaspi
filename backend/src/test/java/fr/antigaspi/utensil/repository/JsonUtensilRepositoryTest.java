package fr.antigaspi.utensil.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import fr.antigaspi.utensil.model.Utensil;

class JsonUtensilRepositoryTest {

    /** Données initiales vides : à n'utiliser que si le fichier existe déjà. */
    private static final Resource NO_INITIAL = new ByteArrayResource(new byte[0]);

    private static JsonUtensilRepository repository(String json) {
        return new JsonUtensilRepository(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void chargeLesUstensiles() {
        JsonUtensilRepository repository = repository("""
                [{"id": 1, "name": "Four"}, {"id": 2, "name": "Poêle"}]
                """);

        assertThat(repository.findAll()).containsExactly(new Utensil(1L, "Four"), new Utensil(2L, "Poêle"));
    }

    @Test
    void fichierInvalide_echoueAuChargement() {
        assertThatThrownBy(() -> repository("pas du json")).isInstanceOf(IllegalStateException.class);
    }

    private static Path file(Path dir, String json) throws IOException {
        return Files.writeString(dir.resolve("utensils.json"), json);
    }

    @Test
    void save_genereUnIdEtPersisteDansLeFichier(@TempDir Path dir) throws IOException {
        Path file = file(dir, """
                [{"id": 1, "name": "Four"}, {"id": 4, "name": "Poêle"}]
                """);

        Utensil saved = new JsonUtensilRepository(file, NO_INITIAL).save("Mixeur");

        assertThat(saved).isEqualTo(new Utensil(5L, "Mixeur"));
        assertThat(new JsonUtensilRepository(file, NO_INITIAL).findAll()).containsExactly(new Utensil(1L, "Four"),
                new Utensil(4L, "Poêle"), new Utensil(5L, "Mixeur"));
    }

    @Test
    void save_listeVide_commenceAuId1(@TempDir Path dir) throws IOException {
        Path file = file(dir, "[]");

        assertThat(new JsonUtensilRepository(file, NO_INITIAL).save("Four")).isEqualTo(new Utensil(1L, "Four"));
    }

    @Test
    void deleteById_persisteDansLeFichier(@TempDir Path dir) throws IOException {
        Path file = file(dir, """
                [{"id": 1, "name": "Four"}, {"id": 2, "name": "Poêle"}]
                """);

        new JsonUtensilRepository(file, NO_INITIAL).deleteById(1L);

        assertThat(new JsonUtensilRepository(file, NO_INITIAL).findAll()).containsExactly(new Utensil(2L, "Poêle"));
    }

    @Test
    void fichierAbsent_partDesDonneesInitialesEtCreeLeFichierALaPremiereModification(@TempDir Path dir) {
        Path file = dir.resolve("sous-dossier/absent.json");
        Resource initial = new ByteArrayResource("""
                [{"id": 1, "name": "Four"}]
                """.getBytes(StandardCharsets.UTF_8));

        JsonUtensilRepository repository = new JsonUtensilRepository(file, initial);

        assertThat(repository.findAll()).containsExactly(new Utensil(1L, "Four"));
        assertThat(file).doesNotExist();

        repository.save("Mixeur");

        assertThat(new JsonUtensilRepository(file, NO_INITIAL).findAll()).containsExactly(new Utensil(1L, "Four"),
                new Utensil(2L, "Mixeur"));
    }

    @Test
    void fichierPresent_prioritaireSurLesDonneesInitiales(@TempDir Path dir) throws IOException {
        Path file = file(dir, """
                [{"id": 7, "name": "Wok"}]
                """);

        assertThat(new JsonUtensilRepository(file, NO_INITIAL).findAll()).containsExactly(new Utensil(7L, "Wok"));
    }

    @Test
    void fichierDeDonneesDuProjet_seChargeAvecDesIdsUniques() {
        List<Utensil> utensils = new JsonUtensilRepository(new ClassPathResource("data/utensils.json")).findAll();

        assertThat(utensils).isNotEmpty();
        assertThat(utensils).extracting(Utensil::id).doesNotHaveDuplicates();
        assertThat(utensils).extracting(Utensil::name).allSatisfy(name -> assertThat(name).isNotBlank());
    }
}
