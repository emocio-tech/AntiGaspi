package fr.antigaspi.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.profile.repository.ProfileRepository;

class ProfileServiceTest {

    /** Dépôt en mémoire qui garde la dernière sauvegarde. */
    private static class InMemoryProfileRepository implements ProfileRepository {

        final List<UserProfile> saved = new ArrayList<>();
        UserProfile current = UserProfile.empty();

        @Override
        public UserProfile load() {
            return current;
        }

        @Override
        public void save(UserProfile profile) {
            saved.add(profile);
            current = profile;
        }
    }

    private final InMemoryProfileRepository repository = new InMemoryProfileRepository();
    private final ProfileService service = new ProfileService(repository);

    @Test
    void updatePreferences_retireLesEspacesSuperflus_etEnregistre() {
        UserProfile result = service.updatePreferences(List.of("  Sans gluten ", "Pas de coriandre"));

        assertThat(result.preferences()).containsExactly("Sans gluten", "Pas de coriandre");
        assertThat(repository.current).isEqualTo(result);
    }

    @Test
    void updatePreferences_listeVide_viderLesPreferences() {
        repository.current = new UserProfile(List.of("Sans porc"));

        UserProfile result = service.updatePreferences(List.of());

        assertThat(result.preferences()).isEmpty();
        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void updatePreferences_preferenceVide_estRefusee_sansRienEnregistrer() {
        assertThatThrownBy(() -> service.updatePreferences(Arrays.asList("Sans porc", "   ")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Une préférence ne peut pas être vide");

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void updatePreferences_preferenceNull_estRefusee() {
        assertThatThrownBy(() -> service.updatePreferences(Arrays.asList("Sans porc", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Une préférence ne peut pas être vide");
    }

    @Test
    void updatePreferences_doublonSansTenirCompteDeLaCasse_estRefuse_sansRienEnregistrer() {
        assertThatThrownBy(() -> service.updatePreferences(List.of("Sans gluten", " sans GLUTEN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La préférence « sans GLUTEN » est en double");

        assertThat(repository.saved).isEmpty();
    }

    @Test
    void updatePreferences_listeAbsente_estRefusee() {
        assertThatThrownBy(() -> service.updatePreferences(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La liste des préférences est absente");
    }
}
