package fr.antigaspi.profile.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import fr.antigaspi.common.ChatTools;
import fr.antigaspi.profile.service.ProfileService;

/**
 * Tool Spring AI donnant au LLM les préférences alimentaires du profil utilisateur.
 */
@Component
public class ProfileTools implements ChatTools {

    private final ProfileService profileService;

    public ProfileTools(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Tool(name = "consulterPreferences", description = """
            Liste les préférences alimentaires de l'utilisateur (régime, allergies, aliments refusés, goûts), à \
            respecter strictement. À appeler avant de proposer une recette ; liste vide si aucune préférence.""")
    public List<String> consulterPreferences() {
        return profileService.getProfile().preferences();
    }
}
