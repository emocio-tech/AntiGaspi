package fr.antigaspi.utensil;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import fr.antigaspi.common.JsonResources;
import tools.jackson.core.type.TypeReference;

/**
 * Ustensiles disponibles chargés depuis un fichier JSON statique au démarrage. Sera remplacé par une table en base.
 */
@Repository
public class JsonUtensilRepository implements UtensilRepository {

    private final List<Utensil> utensils;

    public JsonUtensilRepository(@Value("classpath:data/utensils.json") Resource resource) {
        this.utensils = JsonResources.readList(resource, new TypeReference<List<Utensil>>() {
        });
    }

    @Override
    public List<Utensil> findAll() {
        return utensils;
    }
}
