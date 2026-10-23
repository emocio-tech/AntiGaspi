package fr.antigaspi.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès Spring Data à la table {@code ingredient}, utilisé uniquement par {@link JpaIngredientRepository}.
 */
interface SpringDataIngredientJpa extends JpaRepository<IngredientEntity, Long> {
}
