package fr.antigaspi.ingredient.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.model.Urgency;

class IngredientServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static Ingredient ingredient(long id, String name, Category category, Integer expiresInDays) {
        return ingredient(id, name, category, expiresInDays, 0);
    }

    private static Ingredient ingredient(long id, String name, Category category, Integer expiresInDays,
            int addedDaysAgo) {
        LocalDate expiry = expiresInDays == null ? null : TODAY.plusDays(expiresInDays);
        return new Ingredient(id, name, category, BigDecimal.ONE, Unit.PIECE, expiry,
                NOW.minus(Duration.ofDays(addedDaysAgo)));
    }

    private static IngredientService service(Ingredient... ingredients) {
        return new IngredientService(() -> List.of(ingredients), CLOCK);
    }

    private static List<String> names(List<IngredientView> views) {
        return views.stream().map(v -> v.ingredient().name()).toList();
    }

    @Test
    void listByUrgency_trieParDateDePeremptionCroissante() {
        IngredientService service = service(ingredient(1, "Riz", Category.FECULENTS, 200),
                ingredient(2, "Yaourt", Category.PRODUITS_LAITIERS, -1),
                ingredient(3, "Courgette", Category.LEGUMES, 2), ingredient(4, "Lait", Category.PRODUITS_LAITIERS, 0),
                ingredient(5, "Jambon", Category.VIANDES_POISSONS, 5));

        assertThat(names(service.listByUrgency())).containsExactly("Yaourt", "Lait", "Courgette", "Jambon", "Riz");
    }

    @Test
    void listByUrgency_calculeJoursRestantsEtUrgence() {
        IngredientService service = service(ingredient(1, "Yaourt", Category.PRODUITS_LAITIERS, -1),
                ingredient(2, "Courgette", Category.LEGUMES, 2), ingredient(3, "Jambon", Category.VIANDES_POISSONS, 5),
                ingredient(4, "Riz", Category.FECULENTS, 200));

        List<IngredientView> views = service.listByUrgency();

        assertThat(views).extracting(IngredientView::daysLeft).containsExactly(-1L, 2L, 5L, 200L);
        assertThat(views).extracting(IngredientView::urgency).containsExactly(Urgency.EXPIRED_OR_TODAY, Urgency.SOON,
                Urgency.WEEK, Urgency.LATER);
        assertThat(views).extracting(IngredientView::estimated).containsOnly(false);
        assertThat(views.get(1).effectiveExpiryDate()).isEqualTo(TODAY.plusDays(2));
    }

    @Test
    void listByUrgency_estimeLaDateSelonLaCategorieQuandElleEstAbsente() {
        // Fruits : conservation minimale de 3 jours, ajoutés il y a 1 jour -> péremption estimée à J+2
        IngredientService service = service(ingredient(1, "Pommes", Category.FRUITS, null, 1));

        IngredientView view = service.listByUrgency().getFirst();

        assertThat(view.estimated()).isTrue();
        assertThat(view.effectiveExpiryDate()).isEqualTo(TODAY.plusDays(Category.FRUITS.minShelfDays() - 1));
        assertThat(view.daysLeft()).isEqualTo(2);
        assertThat(view.urgency()).isEqualTo(Urgency.SOON);
        assertThat(view.ingredient().expiryDate()).isNull();
    }

    @Test
    void listByUrgency_aDateEgaleLaDateReellePasseAvantLEstimeePuisTriParNom() {
        // Pommes sans date ajoutées aujourd'hui : estimée à J+3, comme les deux autres
        IngredientService service = service(ingredient(1, "Pommes", Category.FRUITS, null),
                ingredient(2, "Poires", Category.FRUITS, 3), ingredient(3, "Abricots", Category.FRUITS, 3));

        assertThat(names(service.listByUrgency())).containsExactly("Abricots", "Poires", "Pommes");
    }

    @Test
    void listByUrgency_inventaireVide() {
        assertThat(service().listByUrgency()).isEmpty();
    }

    @Test
    void findMostUrgent_renvoieLesNPremiers() {
        IngredientService service = service(ingredient(1, "Riz", Category.FECULENTS, 200),
                ingredient(2, "Yaourt", Category.PRODUITS_LAITIERS, -1),
                ingredient(3, "Courgette", Category.LEGUMES, 2));

        assertThat(names(service.findMostUrgent(2))).containsExactly("Yaourt", "Courgette");
        assertThat(names(service.findMostUrgent(10))).containsExactly("Yaourt", "Courgette", "Riz");
        assertThat(service.findMostUrgent(0)).isEmpty();
    }

    @Test
    void findByIds_filtreEtTrieEnIgnorantLesIdsInconnus() {
        IngredientService service = service(ingredient(1, "Riz", Category.FECULENTS, 200),
                ingredient(2, "Yaourt", Category.PRODUITS_LAITIERS, -1),
                ingredient(3, "Courgette", Category.LEGUMES, 2));

        assertThat(names(service.findByIds(Set.of(1L, 3L, 99L)))).containsExactly("Courgette", "Riz");
        assertThat(service.findByIds(Set.of())).isEmpty();
    }
}
