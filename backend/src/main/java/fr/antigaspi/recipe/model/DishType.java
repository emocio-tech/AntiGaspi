package fr.antigaspi.recipe.model;

/**
 * Type de plat imposé à une variante de recette, pour que des recettes demandées en parallèle ne se ressemblent pas.
 */
public enum DishType {

    POELEE("poêlée ou sauté"), FOUR("plat au four : gratin, tarte ou cake"), MIJOTE("soupe, velouté ou plat mijoté");

    private final String label;

    DishType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
