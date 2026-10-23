package fr.antigaspi.ingredient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientUpdate;
import fr.antigaspi.ingredient.model.NewIngredient;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;

class IngredientEditServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-10-10T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private static final Ingredient YAOURTS = new Ingredient(1L, "Yaourts", Category.PRODUITS_LAITIERS,
            new BigDecimal("4"), Unit.PIECE, TODAY.plusDays(10), EARLIER);
    private static final Ingredient COURGETTES = new Ingredient(2L, "Courgettes", Category.LEGUMES,
            new BigDecimal("2.5"), Unit.KG, TODAY.plusDays(2), EARLIER);
    private static final Ingredient LAIT = new Ingredient(3L, "Lait", Category.PRODUITS_LAITIERS, BigDecimal.ONE,
            Unit.L, null, EARLIER);

    private final InMemoryIngredientRepository repository = new InMemoryIngredientRepository(YAOURTS, COURGETTES, LAIT);
    private final IngredientEditService service = new IngredientEditService(repository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private static NewIngredient nouveau(String name, BigDecimal quantity) {
        return new NewIngredient(name, Category.FRUITS, quantity, Unit.PIECE, TODAY.plusDays(5));
    }

    private static IngredientUpdate modification(long id, String name, BigDecimal quantity, LocalDate expiryDate,
            boolean clearExpiryDate) {
        return new IngredientUpdate(id, name, null, quantity, null, expiryDate, clearExpiryDate);
    }

    // ---- ajout ----

    @Test
    void add_plusieursAliments_attribueLesIdentifiantsEtLaDateDAjout() {
        List<Ingredient> added = service.add(List.of(nouveau("Pommes", new BigDecimal("6")),
                new NewIngredient("Riz", Category.FECULENTS, new BigDecimal("500"), Unit.G, null)));

        assertThat(added).extracting(Ingredient::id).containsExactly(4L, 5L);
        assertThat(added).extracting(Ingredient::name).containsExactly("Pommes", "Riz");
        assertThat(added).extracting(Ingredient::addedAt).containsOnly(NOW);
        assertThat(added.get(0).expiryDate()).isEqualTo(TODAY.plusDays(5));
        assertThat(added.get(1).expiryDate()).isNull();
        assertThat(repository.findAll()).hasSize(5).containsAll(added);
    }

    @Test
    void add_nomAvecEspaces_estNettoye() {
        List<Ingredient> added = service.add(List.of(nouveau("  Pommes  ", BigDecimal.ONE)));

        assertThat(added.get(0).name()).isEqualTo("Pommes");
    }

    @Test
    void add_unNomDejaPresent_estAutorise() {
        service.add(List.of(nouveau("Yaourts", BigDecimal.ONE)));

        assertThat(repository.findAll()).extracting(Ingredient::name).filteredOn("Yaourts"::equals).hasSize(2);
    }

    @Test
    void add_listeVideOuAbsente_estRefusee() {
        assertThatThrownBy(() -> service.add(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void add_alimentInvalide_refuseToutSansRienEcrire() {
        NewIngredient valide = nouveau("Pommes", BigDecimal.ONE);

        assertThatThrownBy(() -> service.add(List.of(valide, nouveau("  ", BigDecimal.ONE))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(List.of(valide, nouveau(null, BigDecimal.ONE))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(List.of(valide, nouveau("Poires", BigDecimal.ZERO))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(List.of(valide, nouveau("Poires", new BigDecimal("-1")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(List.of(valide, nouveau("Poires", null))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                () -> service.add(List.of(valide, new NewIngredient("Poires", null, BigDecimal.ONE, Unit.PIECE, null))))
                        .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service
                .add(List.of(valide, new NewIngredient("Poires", Category.FRUITS, BigDecimal.ONE, null, null))))
                        .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES, LAIT);
    }

    @Test
    void add_messageDErreur_designeLAlimentEnCause() {
        assertThatThrownBy(() -> service.add(List.of(nouveau("Pommes", BigDecimal.ONE), nouveau("Poires", null))))
                .hasMessageContaining("Poires");
    }

    // ---- suppression ----

    @Test
    void remove_plusieursAliments_lesSupprimeEtLesRenvoie() {
        List<Ingredient> removed = service.remove(List.of(3L, 1L));

        assertThat(removed).containsExactly(LAIT, YAOURTS);
        assertThat(repository.findAll()).containsExactly(COURGETTES);
    }

    @Test
    void remove_listeVideOuAbsente_estRefusee() {
        assertThatThrownBy(() -> service.remove(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.remove(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void remove_unIdentifiantInconnu_refuseToutSansRienSupprimer() {
        assertThatThrownBy(() -> service.remove(List.of(1L, 99L))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES, LAIT);
    }

    @Test
    void remove_unIdentifiantEnDouble_refuseToutSansRienSupprimer() {
        assertThatThrownBy(() -> service.remove(List.of(1L, 1L))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1");

        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES, LAIT);
    }

    // ---- modification ----

    @Test
    void update_seulsLesChampsRenseignesChangent() {
        List<Ingredient> updated = service.update(List.of(modification(2L, null, new BigDecimal("1"), null, false)));

        assertThat(updated).containsExactly(new Ingredient(2L, "Courgettes", Category.LEGUMES, new BigDecimal("1"),
                Unit.KG, TODAY.plusDays(2), EARLIER));
        assertThat(repository.findAll()).contains(updated.get(0));
    }

    @Test
    void update_tousLesChamps_etLaDateDAjoutResteInchangee() {
        List<Ingredient> updated = service.update(List.of(new IngredientUpdate(1L, " Skyr ", Category.OEUFS,
                new BigDecimal("2"), Unit.G, TODAY.plusDays(30), false)));

        assertThat(updated).containsExactly(
                new Ingredient(1L, "Skyr", Category.OEUFS, new BigDecimal("2"), Unit.G, TODAY.plusDays(30), EARLIER));
    }

    @Test
    void update_plusieursAliments_lesModifieDansLOrdreRecu() {
        List<Ingredient> updated = service.update(List.of(modification(3L, "Lait entier", null, null, false),
                modification(1L, null, null, TODAY.plusDays(3), false)));

        assertThat(updated).extracting(Ingredient::id).containsExactly(3L, 1L);
        assertThat(updated.get(0).name()).isEqualTo("Lait entier");
        assertThat(updated.get(1).expiryDate()).isEqualTo(TODAY.plusDays(3));
    }

    @Test
    void update_effacerLaDate_retireLaDateDePeremption() {
        List<Ingredient> updated = service.update(List.of(modification(1L, null, null, null, true)));

        assertThat(updated.get(0).expiryDate()).isNull();
        assertThat(updated.get(0).name()).isEqualTo("Yaourts");
    }

    @Test
    void update_dateDonneeEtEffacee_estRefuse() {
        assertThatThrownBy(() -> service.update(List.of(modification(1L, null, null, TODAY.plusDays(3), true))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_listeVideOuAbsente_estRefusee() {
        assertThatThrownBy(() -> service.update(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_unIdentifiantInconnuOuEnDouble_refuseToutSansRienModifier() {
        IngredientUpdate valide = modification(1L, "Skyr", null, null, false);

        assertThatThrownBy(() -> service.update(List.of(valide, modification(99L, "X", null, null, false))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("99");
        assertThatThrownBy(() -> service.update(List.of(valide, modification(1L, "Y", null, null, false))))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES, LAIT);
    }

    @Test
    void update_champInvalide_refuseToutSansRienModifier() {
        IngredientUpdate valide = modification(1L, "Skyr", null, null, false);

        assertThatThrownBy(() -> service.update(List.of(valide, modification(2L, "  ", null, null, false))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(List.of(valide, modification(2L, null, BigDecimal.ZERO, null, false))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                () -> service.update(List.of(valide, modification(2L, null, new BigDecimal("-2"), null, false))))
                        .isInstanceOf(IllegalArgumentException.class);

        assertThat(repository.findAll()).containsExactly(YAOURTS, COURGETTES, LAIT);
    }
}
