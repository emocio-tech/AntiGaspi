package fr.antigaspi.utensil.repository;

import java.util.List;

import fr.antigaspi.utensil.model.Utensil;

/**
 * Accès aux ustensiles disponibles, stockés en base (PostgreSQL, H2 en test) et implémenté par
 * {@link JpaUtensilRepository}.
 */
public interface UtensilRepository {

    List<Utensil> findAll();

    /**
     * Enregistre un nouvel ustensile en lui attribuant un identifiant.
     */
    Utensil save(String name);

    void deleteById(long id);
}
