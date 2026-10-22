package fr.antigaspi.recipe.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.antigaspi.ingredient.service.IngredientService;
import fr.antigaspi.recipe.model.Consumption;
import fr.antigaspi.recipe.model.CookedRecipe;
import fr.antigaspi.recipe.model.Rating;
import fr.antigaspi.recipe.model.Recipe;
import fr.antigaspi.recipe.repository.CookedRecipeRepository;

/** « J'ai cuisiné » : décompte des aliments utilisés et enregistrement de la recette cuisinée. */
@Service
public class CookingService {

    private static final int MAX_COMMENT_LENGTH = 1000;
    private static final int MAX_TITLE_LENGTH = 200;

    private final IngredientService ingredientService;
    private final CookedRecipeRepository repository;
    private final Clock clock;

    public CookingService(IngredientService ingredientService, CookedRecipeRepository repository, Clock clock) {
        this.ingredientService = ingredientService;
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Décompte les aliments utilisés puis enregistre la recette cuisinée, dans une même transaction : si
     * l'enregistrement échoue, l'inventaire n'est pas modifié. Les quantités d'un même aliment sont cumulées, les
     * lignes à 0 et les aliments déjà retirés sont ignorés.
     *
     * @param consumed
     *            aliments utilisés, éventuellement {@code null}
     *
     * @throws IllegalArgumentException
     *             si la recette est absente ou sans titre, le commentaire dépasse 1000 caractères ou une quantité est
     *             négative ou absente, ou si une ligne n'indique pas l'aliment, ou si le titre ou le JSON de la recette
     *             sont trop longs
     */
    @Transactional
    public CookedRecipe cook(Recipe recipe, Rating rating, String comment, List<Consumption> consumed) {
        if (recipe == null || recipe.title() == null || recipe.title().isBlank()) {
            throw new IllegalArgumentException("La recette est vide");
        }
        if (recipe.title().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Le titre de la recette dépasse " + MAX_TITLE_LENGTH + " caractères");
        }
        String cleanedComment = comment == null || comment.isBlank() ? null : comment.strip();
        if (cleanedComment != null && cleanedComment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Le commentaire dépasse " + MAX_COMMENT_LENGTH + " caractères");
        }
        Map<Long, BigDecimal> used = new LinkedHashMap<>();
        for (Consumption line : consumed == null ? List.<Consumption> of() : consumed) {
            if (line == null || line.ingredientId() == null) {
                throw new IllegalArgumentException("L'aliment utilisé est obligatoire");
            }
            if (line.quantity() == null || line.quantity().signum() < 0) {
                throw new IllegalArgumentException("La quantité utilisée doit être positive ou nulle : "
                        + (line.quantity() == null ? "aucune" : line.quantity().stripTrailingZeros().toPlainString()));
            }
            if (line.quantity().signum() > 0) {
                used.merge(line.ingredientId(), line.quantity(), BigDecimal::add);
            }
        }
        ingredientService.consume(used);
        return repository.save(new CookedRecipe(null, recipe, clock.instant(), rating, cleanedComment));
    }

    /** Historique des recettes cuisinées, de la plus récente à la plus ancienne. */
    public List<CookedRecipe> history() {
        return repository.findAllMostRecentFirst();
    }

    /**
     * Modifie l'avis et le commentaire d'une recette cuisinée ; la date et la recette sont inchangées.
     *
     * @param rating
     *            nouvel avis, {@code null} pour l'effacer
     * @param comment
     *            nouveau commentaire, {@code null} ou blanc pour l'effacer
     *
     * @throws CookedRecipeNotFoundException
     *             si l'identifiant est inconnu
     * @throws IllegalArgumentException
     *             si le commentaire dépasse 1000 caractères
     */
    @Transactional
    public CookedRecipe updateFeedback(long id, Rating rating, String comment) {
        String cleanedComment = comment == null || comment.isBlank() ? null : comment.strip();
        if (cleanedComment != null && cleanedComment.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Le commentaire dépasse " + MAX_COMMENT_LENGTH + " caractères");
        }
        CookedRecipe existing = repository.findById(id).orElseThrow(() -> new CookedRecipeNotFoundException(id));
        return repository
                .save(new CookedRecipe(existing.id(), existing.recipe(), existing.cookedAt(), rating, cleanedComment));
    }

    /** Les {@code limit} recettes cuisinées les plus récentes, de la plus récente à la plus ancienne. */
    public List<CookedRecipe> recent(int limit) {
        return repository.findMostRecent(limit);
    }
}
