package fr.antigaspi.utensil.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import fr.antigaspi.utensil.model.Utensil;

class JsonUtensilRepositoryTest {

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

    @Test
    void fichierDeDonneesDuProjet_seChargeAvecDesIdsUniques() {
        List<Utensil> utensils = new JsonUtensilRepository(new ClassPathResource("data/utensils.json")).findAll();

        assertThat(utensils).isNotEmpty();
        assertThat(utensils).extracting(Utensil::id).doesNotHaveDuplicates();
        assertThat(utensils).extracting(Utensil::name).allSatisfy(name -> assertThat(name).isNotBlank());
    }
}
