package fr.antigaspi.profile.api;

import java.util.List;

/**
 * Corps de {@code PUT /api/profile}.
 *
 * @param preferences
 *            nouvelle liste complète des préférences alimentaires
 */
public record ProfileRequest(List<String> preferences) {
}
