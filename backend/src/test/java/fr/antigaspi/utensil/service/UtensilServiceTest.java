package fr.antigaspi.utensil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.repository.InMemoryUtensilRepository;

class UtensilServiceTest {

    @Test
    void listAll_trieParNomSansTenirCompteDeLaCasse() {
        UtensilService service = new UtensilService(new InMemoryUtensilRepository(new Utensil(1L, "Poêle"),
                new Utensil(2L, "four"), new Utensil(3L, "Casserole")));

        assertThat(service.listAll()).extracting(Utensil::name).containsExactly("Casserole", "four", "Poêle");
    }

    @Test
    void listAll_aucunUstensile() {
        assertThat(new UtensilService(new InMemoryUtensilRepository()).listAll()).isEmpty();
    }

    @Test
    void add_enregistreLUstensileSansLesEspacesAutour() {
        UtensilService service = new UtensilService(new InMemoryUtensilRepository(new Utensil(1L, "Four")));

        Utensil added = service.add("  Mixeur ");

        assertThat(added.name()).isEqualTo("Mixeur");
        assertThat(service.listAll()).extracting(Utensil::name).containsExactly("Four", "Mixeur");
    }

    @Test
    void add_nomVide_estRefuse() {
        UtensilService service = new UtensilService(new InMemoryUtensilRepository());

        assertThatThrownBy(() -> service.add("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.listAll()).isEmpty();
    }

    @Test
    void add_doublonSansTenirCompteDeLaCasse_estRefuse() {
        UtensilService service = new UtensilService(new InMemoryUtensilRepository(new Utensil(1L, "Poêle")));

        assertThatThrownBy(() -> service.add("poêle")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Poêle");
        assertThat(service.listAll()).hasSize(1);
    }

    @Test
    void remove_supprimeEtRenvoieLUstensile() {
        UtensilService service = new UtensilService(
                new InMemoryUtensilRepository(new Utensil(1L, "Four"), new Utensil(2L, "Poêle")));

        Utensil removed = service.remove(2L);

        assertThat(removed).isEqualTo(new Utensil(2L, "Poêle"));
        assertThat(service.listAll()).extracting(Utensil::name).containsExactly("Four");
    }

    @Test
    void remove_idInconnu_estRefuse() {
        UtensilService service = new UtensilService(new InMemoryUtensilRepository(new Utensil(1L, "Four")));

        assertThatThrownBy(() -> service.remove(99L)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
        assertThat(service.listAll()).hasSize(1);
    }
}
