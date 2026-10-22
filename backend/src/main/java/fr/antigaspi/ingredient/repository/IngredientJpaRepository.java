package fr.antigaspi.ingredient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès Spring Data à la table {@code ingredient}.
 */
interface IngredientJpaRepository extends JpaRepository<IngredientEntity, Long> {
}
