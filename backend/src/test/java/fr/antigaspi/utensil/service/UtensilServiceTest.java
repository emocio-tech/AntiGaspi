package fr.antigaspi.utensil.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import fr.antigaspi.utensil.model.Utensil;

class UtensilServiceTest {

    @Test
    void listAll_trieParNomSansTenirCompteDeLaCasse() {
        UtensilService service = new UtensilService(
                () -> List.of(new Utensil(1L, "Poêle"), new Utensil(2L, "four"), new Utensil(3L, "Casserole")));

        assertThat(service.listAll()).extracting(Utensil::name).containsExactly("Casserole", "four", "Poêle");
    }

    @Test
    void listAll_aucunUstensile() {
        assertThat(new UtensilService(List::of).listAll()).isEmpty();
    }
}
