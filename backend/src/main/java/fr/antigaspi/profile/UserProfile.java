package fr.antigaspi.profile;

import java.util.List;

/**
 * Profil de l'utilisateur lu depuis le fichier Markdown.
 *
 * @param preferences
 *            préférences alimentaires (section « Préférences »)
 * @param equipment
 *            équipement de cuisine disponible (section « Équipement »)
 */
public record UserProfile(List<String> preferences, List<String> equipment) {

    public static UserProfile empty() {
        return new UserProfile(List.of(), List.of());
    }
}
