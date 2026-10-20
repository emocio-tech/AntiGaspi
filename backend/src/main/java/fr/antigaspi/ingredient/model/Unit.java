package fr.antigaspi.ingredient.model;

/**
 * Unité de quantité d'un aliment.
 */
public enum Unit {

    G("g"), KG("kg"), ML("ml"), L("l"), PIECE("pièce");

    private final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
