package fr.antigaspi.ingredient.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.model.Urgency;
import fr.antigaspi.ingredient.repository.IngredientRepository;

/**
 * Consultation de l'inventaire, trié par urgence de péremption.
 */
@Service
public class IngredientService {

    /** Date la plus proche d'abord ; à date égale, date réelle avant date estimée, puis ordre alphabétique. */
    private static final Comparator<IngredientView> BY_URGENCY = Comparator
            .comparing(IngredientView::effectiveExpiryDate).thenComparing(IngredientView::estimated)
            .thenComparing(view -> view.ingredient().name(), String.CASE_INSENSITIVE_ORDER);

    private final IngredientRepository repository;
    private final Clock clock;

    public IngredientService(IngredientRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Tous les aliments, du plus urgent au moins urgent.
     */
    public List<IngredientView> listByUrgency() {
        return sortedViews(repository.findAll());
    }

    /**
     * Les aliments correspondant aux filtres, du plus urgent au moins urgent. Un filtre {@code null} n'est pas
     * appliqué.
     *
     * @param category
     *            uniquement cette catégorie
     * @param withinDays
     *            uniquement les aliments périmés ou à consommer sous ce nombre de jours
     * @param limit
     *            nombre maximal d'aliments, appliqué après les autres filtres
     *
     * @throws IllegalArgumentException
     *             si {@code withinDays} est négatif ou si {@code limit} n'est pas positif
     */
    public List<IngredientView> search(Category category, Integer withinDays, Integer limit) {
        if (withinDays != null && withinDays < 0) {
            throw new IllegalArgumentException("Le nombre de jours ne peut pas être négatif : " + withinDays);
        }
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("La limite doit être positive : " + limit);
        }
        return listByUrgency().stream().filter(view -> category == null || view.ingredient().category() == category)
                .filter(view -> withinDays == null || view.daysLeft() <= withinDays)
                .limit(limit == null ? Long.MAX_VALUE : limit).toList();
    }

    private List<IngredientView> sortedViews(List<Ingredient> ingredients) {
        LocalDate today = LocalDate.now(clock);
        return ingredients.stream().map(ingredient -> toView(ingredient, today)).sorted(BY_URGENCY).toList();
    }

    private IngredientView toView(Ingredient ingredient, LocalDate today) {
        boolean estimated = ingredient.expiryDate() == null;
        LocalDate effectiveExpiryDate = estimated ? estimateExpiryDate(ingredient) : ingredient.expiryDate();
        long daysLeft = ChronoUnit.DAYS.between(today, effectiveExpiryDate);
        return new IngredientView(ingredient, effectiveExpiryDate, daysLeft, Urgency.of(daysLeft), estimated);
    }

    /** Estimation prudente : date d'ajout + durée de conservation minimale de la catégorie. */
    private LocalDate estimateExpiryDate(Ingredient ingredient) {
        LocalDate addedOn = LocalDate.ofInstant(ingredient.addedAt(), clock.getZone());
        return addedOn.plusDays(ingredient.category().minShelfDays());
    }
}
