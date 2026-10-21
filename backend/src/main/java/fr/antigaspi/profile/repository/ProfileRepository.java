package fr.antigaspi.profile.repository;

import fr.antigaspi.profile.model.UserProfile;

/**
 * Accès au profil utilisateur. Implémenté aujourd'hui par un fichier Markdown.
 */
public interface ProfileRepository {

    /**
     * Le profil courant, vide s'il n'existe pas.
     */
    UserProfile load();
}
