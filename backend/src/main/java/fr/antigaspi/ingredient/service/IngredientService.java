package fr.antigaspi.ingredient.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientDraft;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.model.Urgency;
import fr.antigaspi.ingredient.repository.IngredientRepository;

/**
 * Consultation de l'inventaire, trié par urgence de péremption, et modification de ses aliments.
 */
@Service
public class IngredientService {

    /** Date la plus proche d'abord ; à date égale, date réelle avant date estimée, puis ordre alphabétique. */
    private static final Comparator<IngredientView> BY_URGENCY = Comparator
            .comparing(IngredientView::effectiveExpiryDate).thenComparing(IngredientView::estimated)
            .thenComparing(view -> view.ingredient().name(), String.CASE_INSENSITIVE_ORDER);

    private static final int MAX_NAME_LENGTH = 100;
    /** Plus grande quantité tenant dans la colonne numeric(10,3). */
    private static final BigDecimal MAX_QUANTITY = new BigDecimal("9999999.999");

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

    /**
     * Un aliment, avec son urgence de péremption.
     *
     * @throws IngredientNotFoundException
     *             si aucun aliment ne porte cet identifiant
     */
    public IngredientView get(long id) {
        return toView(find(id), LocalDate.now(clock));
    }

    /**
     * Ajoute un aliment à l'inventaire ; la date d'ajout est celle d'aujourd'hui.
     *
     * @throws IllegalArgumentException
     *             si le nom est vide ou trop long, la quantité non positive, la catégorie ou l'unité absente
     */
    @Transactional
    public IngredientView add(IngredientDraft draft) {
        BigDecimal quantity = validate(draft);
        Ingredient saved = repository.save(toIngredient(null, draft, quantity, clock.instant()));
        return toView(saved, LocalDate.now(clock));
    }

    /**
     * Remplace les champs d'un aliment ; son identifiant et sa date d'ajout sont conservés.
     *
     * @throws IllegalArgumentException
     *             si une valeur est refusée (voir {@link #add})
     * @throws IngredientNotFoundException
     *             si aucun aliment ne porte cet identifiant
     */
    @Transactional
    public IngredientView update(long id, IngredientDraft draft) {
        Ingredient existing = find(id);
        BigDecimal quantity = validate(draft);
        Ingredient saved = repository.save(toIngredient(id, draft, quantity, existing.addedAt()));
        return toView(saved, LocalDate.now(clock));
    }

    /**
     * Change uniquement la quantité d'un aliment.
     *
     * @throws IllegalArgumentException
     *             si la quantité n'est pas positive
     * @throws IngredientNotFoundException
     *             si aucun aliment ne porte cet identifiant
     */
    @Transactional
    public IngredientView updateQuantity(long id, BigDecimal quantity) {
        Ingredient existing = find(id);
        BigDecimal normalized = validateQuantity(quantity);
        Ingredient saved = repository.save(new Ingredient(existing.id(), existing.name(), existing.category(),
                normalized, existing.unit(), existing.expiryDate(), existing.addedAt()));
        return toView(saved, LocalDate.now(clock));
    }

    /**
     * Retire un aliment de l'inventaire.
     *
     * @throws IngredientNotFoundException
     *             si aucun aliment ne porte cet identifiant
     */
    @Transactional
    public void remove(long id) {
        find(id);
        repository.deleteAllById(List.of(id));
    }

    /**
     * Retire plusieurs aliments : tous ou aucun.
     *
     * @throws IllegalArgumentException
     *             si la liste est vide ou nulle
     * @throws IngredientNotFoundException
     *             si un identifiant est inconnu (rien n'est alors retiré)
     */
    @Transactional
    public void removeAll(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Aucun aliment à retirer");
        }
        Set<Long> known = repository.findAll().stream().map(Ingredient::id).collect(Collectors.toSet());
        List<Long> unknown = ids.stream().filter(id -> !known.contains(id)).distinct().toList();
        if (!unknown.isEmpty()) {
            throw new IngredientNotFoundException(unknown);
        }
        repository.deleteAllById(ids);
    }

    private Ingredient find(long id) {
        return repository.findById(id).orElseThrow(() -> new IngredientNotFoundException(id));
    }

    private static Ingredient toIngredient(Long id, IngredientDraft draft, BigDecimal quantity, Instant addedAt) {
        return new Ingredient(id, draft.name().strip(), draft.category(), quantity, draft.unit(), draft.expiryDate(),
                addedAt);
    }

    private static BigDecimal validate(IngredientDraft draft) {
        if (draft.name() == null || draft.name().isBlank()) {
            throw new IllegalArgumentException("Le nom de l'aliment est vide");
        }
        if (draft.name().strip().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Le nom de l'aliment dépasse " + MAX_NAME_LENGTH + " caractères");
        }
        BigDecimal quantity = validateQuantity(draft.quantity());
        if (draft.category() == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire");
        }
        if (draft.unit() == null) {
            throw new IllegalArgumentException("L'unité est obligatoire");
        }
        return quantity;
    }

    /** Valide la quantité et la renvoie telle qu'elle sera stockée : 3 décimales au plus, sans zéros inutiles. */
    private static BigDecimal validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive : "
                    + (quantity == null ? "aucune" : quantity.stripTrailingZeros().toPlainString()));
        }
        if (quantity.compareTo(MAX_QUANTITY) > 0) {
            throw new IllegalArgumentException("La quantité ne peut pas dépasser " + MAX_QUANTITY.toPlainString()
                    + " : " + quantity.stripTrailingZeros().toPlainString());
        }
        BigDecimal normalized = quantity.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
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
