package fr.antigaspi.utensil.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.repository.InMemoryUtensilRepository;
import fr.antigaspi.utensil.service.UtensilService;

class UtensilToolsTest {

    private final UtensilTools tools = new UtensilTools(
            new UtensilService(new InMemoryUtensilRepository(new Utensil(1L, "Four"))));

    @Test
    void addUtensil_confirmeAvecLIdentifiantAttribue() {
        assertThat(tools.addUtensil("Mixeur")).contains("Mixeur", "2");
    }

    @Test
    void addUtensil_erreurDuService_estRenvoyeeAuLlmSousFormeDeMessage() {
        assertThat(tools.addUtensil("four")).contains("Four");
        assertThat(tools.listUtensils()).hasSize(1);
    }

    @Test
    void removeUtensil_idInconnu_estRenvoyeAuLlmSousFormeDeMessage() {
        assertThat(tools.removeUtensil(99L)).contains("99");
        assertThat(tools.listUtensils()).hasSize(1);
    }
}
