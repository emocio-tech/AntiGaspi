package fr.antigaspi.recipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;

/** Accès Spring Data à la table {@code cooked_recipe}. */
interface CookedRecipeJpaRepository extends JpaRepository<CookedRecipeEntity, Long> {
}
