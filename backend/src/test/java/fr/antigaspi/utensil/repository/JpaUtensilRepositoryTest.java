package fr.antigaspi.utensil.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import fr.antigaspi.utensil.model.Utensil;

@DataJpaTest
@Import(JpaUtensilRepository.class)
class JpaUtensilRepositoryTest {

    @Autowired
    private UtensilRepository repository;

    @Test
    void save_attribueDesIdentifiantsDistinctsEtFindAllLesRelit() {
        Utensil wok = repository.save("Wok");
        Utensil four = repository.save("Four");

        assertThat(wok.id()).isNotNull();
        assertThat(four.id()).isNotNull().isNotEqualTo(wok.id());
        assertThat(repository.findAll()).extracting(Utensil::name).containsExactlyInAnyOrder("Wok", "Four");
    }

    @Test
    void deleteById_retireLUstensile_etIgnoreUnIdentifiantInconnu() {
        Utensil wok = repository.save("Wok");
        repository.save("Four");

        repository.deleteById(wok.id());
        repository.deleteById(999);

        assertThat(repository.findAll()).extracting(Utensil::name).containsExactly("Four");
    }
}
