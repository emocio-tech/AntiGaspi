package fr.antigaspi.ingredient.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import fr.antigaspi.ingredient.model.Category;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import fr.antigaspi.ingredient.model.Ingredient;
import fr.antigaspi.ingredient.model.IngredientDraft;
import fr.antigaspi.ingredient.model.IngredientPatch;
import fr.antigaspi.ingredient.model.IngredientView;
import fr.antigaspi.ingredient.model.Unit;
import fr.antigaspi.ingredient.model.Urgency;
import fr.antigaspi.ingredient.repository.InMemoryIngredientRepository;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

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
        return new IngredientService(new InMemoryIngredientRepository(ingredients), CLOCK);
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

    private static IngredientService stock() {
        return service(ingredient(1, "Riz", Category.FECULENTS, 200),
                ingredient(2, "Yaourt", Category.PRODUITS_LAITIERS, -1),
                ingredient(3, "Courgette", Category.LEGUMES, 2), ingredient(4, "Carottes", Category.LEGUMES, 9),
                // Fruits sans date, ajoutés aujourd'hui : estimés à J+3
                ingredient(5, "Pommes", Category.FRUITS, null));
    }

    @Test
    void search_sansFiltre_renvoieToutTrieParUrgence() {
        assertThat(names(stock().search(null, null, null))).containsExactly("Yaourt", "Courgette", "Pommes", "Carottes",
                "Riz");
    }

    @Test
    void search_parCategorie() {
        assertThat(names(stock().search(Category.LEGUMES, null, null))).containsExactly("Courgette", "Carottes");
        assertThat(stock().search(Category.SURGELES, null, null)).isEmpty();
    }

    @Test
    void search_sousNJours_inclutLesPerimesEtLesDatesEstimees() {
        assertThat(names(stock().search(null, 3, null))).containsExactly("Yaourt", "Courgette", "Pommes");
        assertThat(names(stock().search(null, 0, null))).containsExactly("Yaourt");
    }

    @Test
    void search_limiteAppliqueeApresFiltrage() {
        assertThat(names(stock().search(null, null, 2))).containsExactly("Yaourt", "Courgette");
        assertThat(names(stock().search(Category.LEGUMES, null, 1))).containsExactly("Courgette");
    }

    @Test
    void search_filtresCombines() {
        assertThat(names(stock().search(Category.LEGUMES, 7, 5))).containsExactly("Courgette");
    }

    @Test
    void search_parametresInvalides_leveIllegalArgumentException() {
        assertThatThrownBy(() -> stock().search(null, -1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock().search(null, null, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- Gestion de l'inventaire ---

    private static IngredientDraft draft(String name, LocalDate expiryDate) {
        return new IngredientDraft(name, Category.PRODUITS_LAITIERS, new BigDecimal("2"), Unit.L, expiryDate);
    }

    @Test
    void add_quantiteAuDelaDeLaColonne_refuseeSansRienEnregistrer() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository();
        IngredientService service = serviceOver(repository);
        IngredientDraft enorme = new IngredientDraft("Riz", Category.FECULENTS, new BigDecimal("10000000"), Unit.G,
                null);

        assertThatThrownBy(() -> service.add(enorme)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("La quantité ne peut pas dépasser 9999999.999 : 10000000");
        assertThatThrownBy(() -> service.updateQuantity(1, new BigDecimal("10000000")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void add_quantiteAPlusDeTroisDecimales_arrondieAuMillieme() {
        IngredientService service = serviceOver(new InMemoryIngredientRepository());

        IngredientView added = service
                .add(new IngredientDraft("Farine", Category.EPICERIE_SECHE, new BigDecimal("0.12345"), Unit.KG, null));

        assertThat(added.ingredient().quantity()).isEqualByComparingTo("0.123");
    }

    private static IngredientService serviceOver(InMemoryIngredientRepository repository) {
        return new IngredientService(repository, CLOCK);
    }

    private static Stream<Arguments> invalidDrafts() {
        BigDecimal one = BigDecimal.ONE;
        LocalDate date = TODAY.plusDays(3);
        return Stream.of(
                Arguments.of(new IngredientDraft(null, Category.FRUITS, one, Unit.KG, date),
                        "Le nom de l'aliment est vide"),
                Arguments.of(new IngredientDraft("   ", Category.FRUITS, one, Unit.KG, date),
                        "Le nom de l'aliment est vide"),
                Arguments.of(new IngredientDraft("a".repeat(101), Category.FRUITS, one, Unit.KG, date),
                        "Le nom de l'aliment dépasse 100 caractères"),
                Arguments.of(new IngredientDraft("Pommes", Category.FRUITS, BigDecimal.ZERO, Unit.KG, date),
                        "La quantité doit être positive : 0"),
                Arguments.of(new IngredientDraft("Pommes", Category.FRUITS, new BigDecimal("-2"), Unit.KG, date),
                        "La quantité doit être positive : -2"),
                Arguments.of(new IngredientDraft("Pommes", Category.FRUITS, null, Unit.KG, date),
                        "La quantité doit être positive : aucune"),
                Arguments.of(new IngredientDraft("Pommes", null, one, Unit.KG, date), "La catégorie est obligatoire"),
                Arguments.of(new IngredientDraft("Pommes", Category.FRUITS, one, null, date),
                        "L'unité est obligatoire"));
    }

    @Test
    void add_attribueUnIdentifiant_nettoieLeNom_etFixeLaDateDAjout() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(7, "Riz", Category.FECULENTS, 200));
        IngredientService service = serviceOver(repository);

        IngredientView added = service.add(draft("  Crème fraîche  ", TODAY.plusDays(2)));

        assertThat(added.ingredient().id()).isEqualTo(8L);
        assertThat(added.ingredient().name()).isEqualTo("Crème fraîche");
        assertThat(added.ingredient().addedAt()).isEqualTo(NOW);
        assertThat(added.urgency()).isEqualTo(Urgency.SOON);
        assertThat(service.listByUrgency()).extracting(v -> v.ingredient().id()).contains(8L);
        assertThat(service.listByUrgency().stream().filter(v -> v.ingredient().id() == 8L).findFirst().orElseThrow()
                .urgency()).isEqualTo(Urgency.SOON);
    }

    @Test
    void add_sansDate_laDatePeremptionEstEstimee() {
        IngredientView added = serviceOver(new InMemoryIngredientRepository()).add(draft("Lait", null));

        assertThat(added.estimated()).isTrue();
        assertThat(added.ingredient().expiryDate()).isNull();
    }

    @Test
    void add_datePasseeAcceptee() {
        IngredientView added = serviceOver(new InMemoryIngredientRepository()).add(draft("Lait", TODAY.minusDays(3)));

        assertThat(added.daysLeft()).isEqualTo(-3);
        assertThat(added.urgency()).isEqualTo(Urgency.EXPIRED_OR_TODAY);
    }

    @Test
    void add_homonymeAccepte() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Lait", Category.PRODUITS_LAITIERS, 5));
        IngredientService service = serviceOver(repository);

        service.add(draft("Lait", null));

        assertThat(names(service.listByUrgency())).containsExactlyInAnyOrder("Lait", "Lait");
    }

    @ParameterizedTest
    @MethodSource("invalidDrafts")
    void add_valeurRefusee_leveIllegalArgumentExceptionSansRienEnregistrer(IngredientDraft draft, String message) {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository();

        assertThatThrownBy(() -> serviceOver(repository).add(draft)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void update_remplaceLesChampsEnGardantIdentifiantEtDateDAjout() {
        Ingredient original = ingredient(4, "Lait", Category.PRODUITS_LAITIERS, 5, 2);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(original);

        IngredientView updated = serviceOver(repository).update(4L, new IngredientDraft(" Lait d'amande ",
                Category.BOISSONS, new BigDecimal("3"), Unit.L, TODAY.plusDays(9)));

        assertThat(updated.ingredient()).isEqualTo(new Ingredient(4L, "Lait d'amande", Category.BOISSONS,
                new BigDecimal("3"), Unit.L, TODAY.plusDays(9), original.addedAt()));
        assertThat(repository.findAll()).containsExactly(updated.ingredient());
    }

    @Test
    void update_idInconnu_leveIngredientNotFoundException() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository();

        assertThatThrownBy(() -> serviceOver(repository).update(99L, draft("Lait", null)))
                .isInstanceOf(IngredientNotFoundException.class).hasMessage("Aucun aliment avec l'identifiant 99");
    }

    @ParameterizedTest
    @MethodSource("invalidDrafts")
    void update_valeurRefusee_leveIllegalArgumentException_etLAlimentResteIntact(IngredientDraft draft,
            String message) {
        Ingredient original = ingredient(4, "Lait", Category.PRODUITS_LAITIERS, 5);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(original);

        assertThatThrownBy(() -> serviceOver(repository).update(4L, draft))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(message);
        assertThat(repository.findAll()).containsExactly(original);
    }

    @Test
    void updateQuantity_changeSeulementLaQuantite() {
        Ingredient original = ingredient(4, "Lait", Category.PRODUITS_LAITIERS, 5);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(original);

        IngredientView updated = serviceOver(repository).updateQuantity(4L, new BigDecimal("0.5"));

        assertThat(updated.ingredient()).isEqualTo(new Ingredient(4L, "Lait", Category.PRODUITS_LAITIERS,
                new BigDecimal("0.5"), Unit.PIECE, original.expiryDate(), original.addedAt()));
    }

    @Test
    void updateQuantity_quantiteNonPositive_ouIdInconnu_sontRefuses() {
        Ingredient original = ingredient(4, "Lait", Category.PRODUITS_LAITIERS, 5);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(original);
        IngredientService service = serviceOver(repository);

        assertThatThrownBy(() -> service.updateQuantity(4L, BigDecimal.ZERO))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("La quantité doit être positive : 0");
        assertThat(repository.findAll()).containsExactly(original);
        assertThatThrownBy(() -> service.updateQuantity(99L, BigDecimal.ONE))
                .isInstanceOf(IngredientNotFoundException.class).hasMessage("Aucun aliment avec l'identifiant 99");
    }

    @Test
    void remove_supprimeLAliment_etIdInconnuLeveNotFound() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Riz", Category.FECULENTS, 200), ingredient(2, "Lait", Category.PRODUITS_LAITIERS, 5));
        IngredientService service = serviceOver(repository);

        service.remove(1L);

        assertThat(names(service.listByUrgency())).containsExactly("Lait");
        assertThatThrownBy(() -> service.remove(99L)).isInstanceOf(IngredientNotFoundException.class)
                .hasMessage("Aucun aliment avec l'identifiant 99");
    }

    @Test
    void removeAll_supprimeTousLesAlimentsDonnes() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Riz", Category.FECULENTS, 200), ingredient(2, "Lait", Category.PRODUITS_LAITIERS, 5),
                ingredient(3, "Pommes", Category.FRUITS, 4));

        serviceOver(repository).removeAll(List.of(1L, 3L));

        assertThat(repository.findAll()).extracting(Ingredient::name).containsExactly("Lait");
    }

    @Test
    void removeAll_unIdentifiantInconnu_neSupprimeRien_etCiteLesInconnus() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Riz", Category.FECULENTS, 200), ingredient(2, "Lait", Category.PRODUITS_LAITIERS, 5));

        assertThatThrownBy(() -> serviceOver(repository).removeAll(List.of(1L, 98L, 99L)))
                .isInstanceOf(IngredientNotFoundException.class)
                .hasMessage("Aucun aliment avec les identifiants 98, 99");
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void removeAll_listeVideOuNulle_leveIllegalArgumentException() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Riz", Category.FECULENTS, 200));
        IngredientService service = serviceOver(repository);

        assertThatThrownBy(() -> service.removeAll(List.of())).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Aucun aliment à retirer");
        assertThatThrownBy(() -> service.removeAll(null)).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Aucun aliment à retirer");
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void consume_decrementeRetireLesAlimentsEpuisesEtIgnoreLesIdentifiantsAbsents() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Riz", Category.FECULENTS, 200), ingredient(2, "Lait", Category.PRODUITS_LAITIERS, 5),
                ingredient(3, "Pommes", Category.FRUITS, 4));
        repository.save(new Ingredient(1L, "Riz", Category.FECULENTS, new BigDecimal("2.5"), Unit.KG,
                TODAY.plusDays(200), NOW));

        serviceOver(repository).consume(Map.of(1L, new BigDecimal("1"), 2L, BigDecimal.ONE, 99L, BigDecimal.TEN));

        assertThat(repository.findAll()).extracting(Ingredient::name).containsExactlyInAnyOrder("Riz", "Pommes");
        assertThat(repository.findById(1L)).get().extracting(Ingredient::quantity).isEqualTo(new BigDecimal("1.5"));
    }

    @Test
    void consume_quantiteNegative_leveIllegalArgumentException_etRienNEstModifie() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Riz", Category.FECULENTS, 200), ingredient(2, "Lait", Category.PRODUITS_LAITIERS, 5));
        IngredientService service = serviceOver(repository);
        Map<Long, BigDecimal> used = new LinkedHashMap<>();
        used.put(1L, BigDecimal.ONE);
        used.put(2L, new BigDecimal("-1"));

        assertThatThrownBy(() -> service.consume(used)).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(repository.findAll()).hasSize(2);
    }

    private static IngredientDraft nouveau(String name, BigDecimal quantity) {
        return new IngredientDraft(name, Category.FRUITS, quantity, Unit.PIECE, TODAY.plusDays(5));
    }

    private static IngredientPatch patch(long id, String name, BigDecimal quantity, LocalDate expiryDate,
            boolean clearExpiryDate) {
        return new IngredientPatch(id, name, null, quantity, null, expiryDate, clearExpiryDate);
    }

    @Test
    void addAll_plusieursAliments_attribueLesIdentifiantsDansLOrdreRecu() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(7, "Riz", Category.FECULENTS, 200));

        List<IngredientView> added = serviceOver(repository).addAll(List.of(nouveau("  Pommes ", new BigDecimal("6")),
                new IngredientDraft("Lait", Category.PRODUITS_LAITIERS, BigDecimal.ONE, Unit.L, null)));

        assertThat(added).extracting(v -> v.ingredient().id()).containsExactly(8L, 9L);
        assertThat(added).extracting(v -> v.ingredient().name()).containsExactly("Pommes", "Lait");
        assertThat(added).extracting(v -> v.ingredient().addedAt()).containsOnly(NOW);
        assertThat(added.get(1).estimated()).isTrue();
        assertThat(repository.findAll()).hasSize(3);
    }

    @Test
    void addAll_listeVideOuNulle_estRefusee() {
        IngredientService service = serviceOver(new InMemoryIngredientRepository());

        assertThatThrownBy(() -> service.addAll(List.of())).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.addAll(null)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addAll_unAlimentRefuse_refuseToutSansRienEnregistrer_etNommeLAliment() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository();
        IngredientService service = serviceOver(repository);

        assertThatThrownBy(
                () -> service.addAll(List.of(nouveau("Pommes", BigDecimal.ONE), nouveau("Poires", BigDecimal.ZERO))))
                        .isExactlyInstanceOf(IllegalArgumentException.class).hasMessageContaining("Poires")
                        .hasMessageContaining("La quantité doit être positive");
        assertThatThrownBy(
                () -> service.addAll(List.of(nouveau("Pommes", BigDecimal.ONE), nouveau("  ", BigDecimal.ONE))))
                        .isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Le nom de l'aliment est vide");
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void updateAll_seulsLesChampsRenseignesChangent_etLaDateDAjoutResteInchangee() {
        Ingredient courgettes = new Ingredient(2L, "Courgettes", Category.LEGUMES, new BigDecimal("2.5"), Unit.KG,
                TODAY.plusDays(2), NOW.minus(Duration.ofDays(3)));
        Ingredient lait = new Ingredient(3L, "Lait", Category.PRODUITS_LAITIERS, BigDecimal.ONE, Unit.L, null, NOW);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(courgettes, lait);

        List<IngredientView> updated = serviceOver(repository)
                .updateAll(List.of(patch(3L, " Lait entier ", null, TODAY.plusDays(4), false),
                        patch(2L, null, BigDecimal.ONE, null, false)));

        assertThat(updated).extracting(v -> v.ingredient().id()).containsExactly(3L, 2L);
        assertThat(updated.get(0).ingredient()).isEqualTo(new Ingredient(3L, "Lait entier", Category.PRODUITS_LAITIERS,
                BigDecimal.ONE, Unit.L, TODAY.plusDays(4), NOW));
        assertThat(updated.get(1).ingredient()).isEqualTo(new Ingredient(2L, "Courgettes", Category.LEGUMES,
                BigDecimal.ONE, Unit.KG, TODAY.plusDays(2), NOW.minus(Duration.ofDays(3))));
        assertThat(repository.findById(3L)).contains(updated.get(0).ingredient());
    }

    @Test
    void updateAll_categorieEtUniteChangent() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Lait", Category.PRODUITS_LAITIERS, 5));

        IngredientView updated = serviceOver(repository)
                .updateAll(List.of(new IngredientPatch(1L, null, Category.BOISSONS, null, Unit.L, null, false))).get(0);

        assertThat(updated.ingredient().category()).isEqualTo(Category.BOISSONS);
        assertThat(updated.ingredient().unit()).isEqualTo(Unit.L);
        assertThat(updated.ingredient().name()).isEqualTo("Lait");
    }

    @Test
    void updateAll_effacerLaDate_retireLaDateDePeremption() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(
                ingredient(1, "Lait", Category.PRODUITS_LAITIERS, 5));

        IngredientView updated = serviceOver(repository).updateAll(List.of(patch(1L, null, null, null, true))).get(0);

        assertThat(updated.ingredient().expiryDate()).isNull();
        assertThat(updated.estimated()).isTrue();
    }

    @Test
    void updateAll_dateDonneeEtEffacee_estRefusee() {
        IngredientService service = serviceOver(
                new InMemoryIngredientRepository(ingredient(1, "Lait", Category.PRODUITS_LAITIERS, 5)));

        assertThatThrownBy(() -> service.updateAll(List.of(patch(1L, null, null, TODAY.plusDays(3), true))))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAll_listeVideOuNulle_estRefusee() {
        IngredientService service = serviceOver(new InMemoryIngredientRepository());

        assertThatThrownBy(() -> service.updateAll(List.of())).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateAll(null)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAll_identifiantInconnu_leveNotFoundSansRienModifier() {
        Ingredient lait = ingredient(1, "Lait", Category.PRODUITS_LAITIERS, 5);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(lait);

        assertThatThrownBy(() -> serviceOver(repository)
                .updateAll(List.of(patch(1L, "Skyr", null, null, false), patch(99L, "X", null, null, false))))
                        .isInstanceOf(IngredientNotFoundException.class).hasMessageContaining("99");
        assertThat(repository.findAll()).containsExactly(lait);
    }

    @Test
    void updateAll_identifiantEnDouble_estRefuseSansRienModifier() {
        Ingredient lait = ingredient(1, "Lait", Category.PRODUITS_LAITIERS, 5);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(lait);

        assertThatThrownBy(() -> serviceOver(repository)
                .updateAll(List.of(patch(1L, "Skyr", null, null, false), patch(1L, "Y", null, null, false))))
                        .isExactlyInstanceOf(IllegalArgumentException.class).hasMessageContaining("1");
        assertThat(repository.findAll()).containsExactly(lait);
    }

    @Test
    void updateAll_valeurRenseigneeRefusee_refuseToutSansRienModifier() {
        Ingredient lait = ingredient(1, "Lait", Category.PRODUITS_LAITIERS, 5);
        Ingredient riz = ingredient(2, "Riz", Category.FECULENTS, 200);
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(lait, riz);
        IngredientService service = serviceOver(repository);

        assertThatThrownBy(() -> service
                .updateAll(List.of(patch(1L, "Skyr", null, null, false), patch(2L, "  ", null, null, false))))
                        .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateAll(
                List.of(patch(1L, "Skyr", null, null, false), patch(2L, null, BigDecimal.ZERO, null, false))))
                        .isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(repository.findAll()).containsExactly(lait, riz);
    }

    @Test
    void consume_resteInferieurAuMillieme_alimentRetire() {
        InMemoryIngredientRepository repository = new InMemoryIngredientRepository(new Ingredient(1L, "Safran",
                Category.CONDIMENTS, new BigDecimal("0.001"), Unit.G, TODAY.plusDays(200), NOW));

        serviceOver(repository).consume(Map.of(1L, new BigDecimal("0.0007")));

        assertThat(repository.findById(1L)).isEmpty();
    }
}
