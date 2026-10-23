package fr.antigaspi.recipe.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès Spring Data à la table {@code cooked_recipe}. */
interface CookedRecipeJpaRepository extends JpaRepository<CookedRecipeEntity, Long> {

    /** Toutes les lignes, de la plus récente à la plus ancienne. */
    List<CookedRecipeEntity> findAllByOrderByCookedAtDescIdDesc();

    /** Les lignes de la page demandée, de la plus récente à la plus ancienne. */
    List<CookedRecipeEntity> findAllByOrderByCookedAtDescIdDesc(Pageable pageable);
}
