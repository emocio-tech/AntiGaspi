package fr.antigaspi.profile.service;

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
}
