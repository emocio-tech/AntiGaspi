package fr.antigaspi.profile.api;

import java.util.List;

/**
 * Profil tel que le front le reçoit.
 *
 * @param preferences
 *            préférences alimentaires
 */
public record ProfileResponse(List<String> preferences) {
}
