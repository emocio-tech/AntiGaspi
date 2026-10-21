package fr.antigaspi.utensil.repository;

import java.util.List;

import fr.antigaspi.utensil.model.Utensil;

/**
 * Accès aux ustensiles disponibles. Implémenté aujourd'hui par un fichier JSON statique, plus tard par PostgreSQL.
 */
public interface UtensilRepository {

    List<Utensil> findAll();
}
