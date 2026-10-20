package fr.antigaspi.utensil;

import java.util.List;

/**
 * Accès aux ustensiles disponibles. Implémenté aujourd'hui par un fichier JSON statique, plus tard par PostgreSQL.
 */
public interface UtensilRepository {

    List<Utensil> findAll();
}
