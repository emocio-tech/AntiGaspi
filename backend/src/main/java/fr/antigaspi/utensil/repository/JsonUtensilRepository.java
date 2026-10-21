package fr.antigaspi.utensil.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import fr.antigaspi.common.JsonResources;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import fr.antigaspi.utensil.model.Utensil;

/**
 * Ustensiles disponibles chargés depuis un fichier JSON au démarrage ; les ajouts et suppressions le réécrivent. Sera
 * remplacé par une table en base.
 * <p>
 * Construit à partir d'une simple ressource (lecture seule, ex. fichier du classpath), les modifications ne sont
 * gardées qu'en mémoire.
 */
@Repository
public class JsonUtensilRepository implements UtensilRepository {

    private static final TypeReference<List<Utensil>> UTENSIL_LIST = new TypeReference<>() {
    };

    /** {@code null} si les modifications ne sont pas persistées. */
    private final Path file;

    private List<Utensil> utensils;

    public JsonUtensilRepository(Resource resource) {
        this(resource, null);
    }

    @Autowired
    public JsonUtensilRepository(@Value("${antigaspi.utensils.path}") Path file) {
        this(new FileSystemResource(file), file);
    }

    private JsonUtensilRepository(Resource resource, Path file) {
        this.file = file;
        this.utensils = List.copyOf(JsonResources.readList(resource, UTENSIL_LIST));
    }

    @Override
    public synchronized List<Utensil> findAll() {
        return utensils;
    }

    @Override
    public synchronized Utensil save(String name) {
        long id = utensils.stream().mapToLong(Utensil::id).max().orElse(0L) + 1;
        Utensil utensil = new Utensil(id, name);
        List<Utensil> updated = new ArrayList<>(utensils);
        updated.add(utensil);
        replaceWith(updated);
        return utensil;
    }

    @Override
    public synchronized void deleteById(long id) {
        replaceWith(utensils.stream().filter(utensil -> utensil.id() != id).toList());
    }

    /** Écrit d'abord le fichier : en cas d'échec, la liste en mémoire reste inchangée. */
    private void replaceWith(List<Utensil> updated) {
        if (file != null) {
            try {
                JsonMapper.shared().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), updated);
            } catch (JacksonException e) {
                throw new IllegalStateException("Impossible d'écrire " + file, e);
            }
        }
        utensils = List.copyOf(updated);
    }
}
