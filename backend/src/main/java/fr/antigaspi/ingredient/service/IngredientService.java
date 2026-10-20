package fr.antigaspi.ingredient.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

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
     * Les {@code limit} aliments les plus urgents.
     */
    public List<IngredientView> findMostUrgent(int limit) {
        return listByUrgency().stream().limit(limit).toList();
    }

    /**
     * Les aliments dont l'identifiant est dans {@code ids}, du plus urgent au moins urgent. Les identifiants inconnus
     * sont ignorés.
     */
    public List<IngredientView> findByIds(Collection<Long> ids) {
        return sortedViews(repository.findAll().stream().filter(ingredient -> ids.contains(ingredient.id())).toList());
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
