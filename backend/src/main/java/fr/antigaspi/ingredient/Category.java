package fr.antigaspi.ingredient;

/**
 * Catégorie d'aliment, avec une durée de conservation par défaut (en jours) utilisée pour estimer la péremption quand
 * aucune date n'est saisie. Valeurs provisoires, à affiner (question ouverte du README).
 */
public enum Category {

    FRUITS("Fruits", 3, 7), LEGUMES("Légumes", 4, 10), PRODUITS_LAITIERS("Produits laitiers", 5, 10),
    VIANDES_POISSONS("Viandes & poissons", 1, 3), OEUFS("Œufs", 14, 28), FECULENTS("Féculents", 180, 365),
    EPICERIE_SECHE("Épicerie sèche", 180, 365), CONSERVES("Conserves", 365, 730), SURGELES("Surgelés", 90, 180),
    BOISSONS("Boissons", 30, 180), CONDIMENTS("Condiments", 90, 365);

    private final String label;
    private final int minShelfDays;
    private final int maxShelfDays;

    Category(String label, int minShelfDays, int maxShelfDays) {
        this.label = label;
        this.minShelfDays = minShelfDays;
        this.maxShelfDays = maxShelfDays;
    }

    public String label() {
        return label;
    }

    public int minShelfDays() {
        return minShelfDays;
    }

    public int maxShelfDays() {
        return maxShelfDays;
    }
}
