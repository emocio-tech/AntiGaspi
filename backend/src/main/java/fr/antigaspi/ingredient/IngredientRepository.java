package fr.antigaspi.ingredient;

import java.util.List;

/**
 * Accès aux aliments de l'inventaire. Implémenté aujourd'hui par un fichier JSON statique, plus tard par PostgreSQL.
 */
public interface IngredientRepository {

    List<Ingredient> findAll();
}
