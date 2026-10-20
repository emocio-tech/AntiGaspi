package fr.antigaspi.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.Resource;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lecture des fichiers JSON de données statiques (en attendant la base de données).
 */
public final class JsonResources {

    private JsonResources() {
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
}
