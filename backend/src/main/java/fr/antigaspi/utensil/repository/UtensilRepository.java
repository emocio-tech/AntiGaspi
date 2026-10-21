package fr.antigaspi.utensil.repository;

import java.util.List;

import fr.antigaspi.utensil.model.Utensil;

/**
 * Accès aux ustensiles disponibles. Implémenté aujourd'hui par un fichier JSON, plus tard par PostgreSQL.
 */
public interface UtensilRepository {

    List<Utensil> findAll();

    /**
     * Enregistre un nouvel ustensile en lui attribuant un identifiant.
     */
    Utensil save(String name);

    void deleteById(long id);
}
