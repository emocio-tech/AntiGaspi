package fr.antigaspi.profile.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import fr.antigaspi.profile.model.UserProfile;
import fr.antigaspi.profile.repository.ProfileRepository;

/**
 * Consultation du profil utilisateur (préférences alimentaires).
 */
@Service
public class ProfileService {

    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfile getProfile() {
        return repository.load();
    }

    /**
     * Remplace les préférences par la liste donnée, après avoir retiré les espaces superflus.
     *
     * @throws IllegalArgumentException
     *             si la liste est absente, si une préférence est vide ou si deux préférences sont identiques (sans tenir
     *             compte de la casse)
     */
    public UserProfile updatePreferences(List<String> preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("La liste des préférences est absente");
        }
        List<String> cleaned = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String preference : preferences) {
            if (preference == null || preference.isBlank()) {
                throw new IllegalArgumentException("Une préférence ne peut pas être vide");
            }
            String clean = preference.strip();
            if (!seen.add(clean.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("La préférence « %s » est en double".formatted(clean));
            }
            cleaned.add(clean);
        }
        UserProfile profile = new UserProfile(List.copyOf(cleaned));
        repository.save(profile);
        return profile;
    }
}
