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

    /**
     * Enregistre le profil. Les implémentations en lecture seule gardent ce comportement par défaut.
     *
     * @throws UnsupportedOperationException
     *             si le dépôt ne sait pas écrire
     */
    default void save(UserProfile profile) {
        throw new UnsupportedOperationException("Ce dépôt de profil est en lecture seule");
    }
}
