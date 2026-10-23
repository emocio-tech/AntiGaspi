package fr.antigaspi.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture des ressources de l'application : fichiers JSON des données d'amorçage (lus par la migration
 * {@code SeedDataMigration}) et prompts texte.
 */
public final class Resources {

    private Resources() {
    }

    /**
     * @throws IllegalStateException
     *             si le fichier est absent ou invalide
     */
    public static <T> List<T> readList(Resource resource, TypeReference<List<T>> type) {
        try (InputStream in = resource.getInputStream()) {
            return JsonMapper.shared().readValue(in, type);
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("Impossible de charger " + resource.getDescription(), e);
        }
    }

    /**
     * Contenu texte UTF-8, sans espaces de début et de fin.
     *
     * @throws UncheckedIOException
     *             si la ressource est absente
     */
    public static String readText(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Ressource introuvable : " + resource.getDescription(), e);
        }
    }
}
