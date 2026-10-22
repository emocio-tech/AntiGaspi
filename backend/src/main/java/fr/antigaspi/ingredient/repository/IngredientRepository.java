package fr.antigaspi.ingredient.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import fr.antigaspi.ingredient.model.Ingredient;

/**
 * Accès aux aliments de l'inventaire, stockés en base (PostgreSQL, H2 en test) et implémenté par
 * {@link JpaIngredientRepository}.
 */
public interface IngredientRepository {

    List<Ingredient> findAll();

    Optional<Ingredient> findById(long id);

    /** Insère l'aliment si son identifiant est nul, sinon le met à jour ; renvoie l'aliment enregistré. */
    Ingredient save(Ingredient ingredient);

    void deleteAllById(Collection<Long> ids);
}
