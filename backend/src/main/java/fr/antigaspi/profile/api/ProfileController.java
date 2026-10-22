package fr.antigaspi.profile.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.antigaspi.profile.service.ProfileService;

/**
 * API REST du profil utilisateur, destinée au front.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse get() {
        return new ProfileResponse(profileService.getProfile().preferences());
    }

    @PutMapping
    public ProfileResponse update(@RequestBody ProfileRequest request) {
        return new ProfileResponse(profileService.updatePreferences(request.preferences()).preferences());
    }
}
