package fr.antigaspi.utensil.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fr.antigaspi.utensil.model.Utensil;
import fr.antigaspi.utensil.repository.InMemoryUtensilRepository;
import fr.antigaspi.utensil.repository.UtensilRepository;
import fr.antigaspi.utensil.service.UtensilService;

/**
 * API REST des ustensiles avec le vrai {@link UtensilService} et des ustensiles en mémoire : ni fichier ni réseau.
 */
@WebMvcTest(UtensilController.class)
@Import(UtensilControllerTest.UstensilesDeTest.class)
class UtensilControllerTest {

    /** Ustensiles servis au test en cours. */
    private static volatile UtensilRepository ustensiles;

    @TestConfiguration
    static class UstensilesDeTest {

        @Bean
        UtensilService utensilService() {
            return new UtensilService(new UtensilRepository() {

                @Override
                public java.util.List<Utensil> findAll() {
                    return ustensiles.findAll();
                }

                @Override
                public Utensil save(String name) {
                    return ustensiles.save(name);
                }

                @Override
                public void deleteById(long id) {
                    ustensiles.deleteById(id);
                }
            });
        }
    }

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void ustensilesParDefaut() {
        ustensiles = new InMemoryUtensilRepository(new Utensil(1L, "Poêle"), new Utensil(2L, "four"),
                new Utensil(3L, "Casserole"));
    }

    private static String json(String name) {
        return "{\"name\":" + name + "}";
    }

    @Test
    void list_renvoieLesUstensilesTriesParNom() throws Exception {
        mvc.perform(get("/api/utensils")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Casserole")).andExpect(jsonPath("$[1].name").value("four"))
                .andExpect(jsonPath("$[2].name").value("Poêle")).andExpect(jsonPath("$[2].id").value(1));
    }

    @Test
    void add_creeLUstensile_etRenvoie201() throws Exception {
        mvc.perform(post("/api/utensils").contentType(MediaType.APPLICATION_JSON).content(json("\" Mixeur \"")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Mixeur"));

        mvc.perform(get("/api/utensils")).andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void add_nomVide_renvoie400AvecLeMessageDuService() throws Exception {
        mvc.perform(post("/api/utensils").contentType(MediaType.APPLICATION_JSON).content(json("\"  \"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le nom de l'ustensile est vide"));
    }

    @Test
    void add_doublon_renvoie400AvecLeMessageDuService() throws Exception {
        mvc.perform(post("/api/utensils").contentType(MediaType.APPLICATION_JSON).content(json("\"FOUR\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("L'ustensile « four » est déjà dans la liste"));
    }

    @Test
    void remove_supprimeLUstensile_etRenvoie204() throws Exception {
        mvc.perform(delete("/api/utensils/2")).andExpect(status().isNoContent());

        mvc.perform(get("/api/utensils")).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("Poêle"));
    }

    @Test
    void remove_identifiantInconnu_renvoie404AvecUnMessage() throws Exception {
        mvc.perform(delete("/api/utensils/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun ustensile avec l'identifiant 99"));
    }

    @Test
    void remove_identifiantNonNumerique_renvoie400() throws Exception {
        mvc.perform(delete("/api/utensils/abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
