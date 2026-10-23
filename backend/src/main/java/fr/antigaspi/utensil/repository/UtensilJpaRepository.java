package fr.antigaspi.utensil.repository;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès Spring Data à la table {@code utensil}.
 */
interface UtensilJpaRepository extends JpaRepository<UtensilEntity, Long> {
}
