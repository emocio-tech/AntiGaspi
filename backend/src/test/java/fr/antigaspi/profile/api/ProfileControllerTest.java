package fr.antigaspi.profile.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.profile.repository.ProfileRepository;
import fr.antigaspi.profile.service.ProfileService;

/**
 * API REST du profil avec le vrai {@link ProfileService} et un profil en mémoire : ni fichier ni réseau.
 */
@WebMvcTest(ProfileController.class)
@Import(ProfileControllerTest.ProfilDeTest.class)
class ProfileControllerTest {

    /** Profil servi au test en cours. */
    private static volatile UserProfile profil;

    @TestConfiguration
    static class ProfilDeTest {

        @Bean
        ProfileService profileService() {
            return new ProfileService(new ProfileRepository() {

                @Override
                public UserProfile load() {
                    return profil;
                }

                @Override
                public void save(UserProfile profile) {
                    profil = profile;
                }
            });
        }
    }

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void profilParDefaut() {
        profil = new UserProfile(List.of("Végétarien le midi", "Sans coriandre"));
    }

    private static String json(String preferences) {
        return "{\"preferences\":" + preferences + "}";
    }

    @Test
    void get_renvoieLesPreferences() throws Exception {
        mvc.perform(get("/api/profile")).andExpect(status().isOk()).andExpect(jsonPath("$.preferences.length()").value(2))
                .andExpect(jsonPath("$.preferences[0]").value("Végétarien le midi"))
                .andExpect(jsonPath("$.preferences[1]").value("Sans coriandre"));
    }

    @Test
    void get_profilVide_renvoieUneListeVide() throws Exception {
        profil = UserProfile.empty();

        mvc.perform(get("/api/profile")).andExpect(status().isOk()).andExpect(jsonPath("$.preferences").isEmpty());
    }

    @Test
    void put_remplaceLesPreferences_etRenvoieLeProfilEnregistre() throws Exception {
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON)
                .content(json("[\" Sans gluten \",\"Pas de lait\"]"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.length()").value(2))
                .andExpect(jsonPath("$.preferences[0]").value("Sans gluten"));

        mvc.perform(get("/api/profile")).andExpect(jsonPath("$.preferences[1]").value("Pas de lait"));
    }

    @Test
    void put_preferenceVide_renvoie400AvecLeMessageDuService() throws Exception {
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content(json("[\"Sans porc\",\" \"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Une préférence ne peut pas être vide"));
    }

    @Test
    void put_doublon_renvoie400AvecLeMessageDuService() throws Exception {
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON)
                .content(json("[\"Sans porc\",\"sans PORC\"]"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La préférence « sans PORC » est en double"));
    }

    @Test
    void put_listeAbsente_renvoie400() throws Exception {
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void put_corpsIllisible_renvoie400AvecUnMessage() throws Exception {
        mvc.perform(put("/api/profile").contentType(MediaType.APPLICATION_JSON).content("pas du json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }
}
