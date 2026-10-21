package fr.antigaspi.profile.model;

import java.util.List;

/**
 * Profil de l'utilisateur lu depuis le fichier Markdown.
 *
 * @param preferences
 *            préférences alimentaires (section « Préférences »)
 */
public record UserProfile(List<String> preferences) {

    public static UserProfile empty() {
        return new UserProfile(List.of());
    }
}
