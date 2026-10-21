package fr.antigaspi.recipe.model;

/**
 * Type d'anomalie détectée par le contrôle d'une recette générée.
 */
public enum RecipeIssueType {

    /** Nom mal recopié mais proche de celui de l'inventaire : le nom de l'inventaire est remis. */
    NOM_CORRIGE("Nom corrigé"),
    /** Identifiant d'un autre aliment de l'inventaire : l'identifiant est retiré. */
    IDENTIFIANT_INCOHERENT("Identifiant incohérent"),
    /** Identifiant absent de l'inventaire : l'identifiant est retiré. */
    IDENTIFIANT_INCONNU("Identifiant inconnu"),
    /** Ingrédient listé mais cité dans aucune étape : seulement signalé. */
    INGREDIENT_NON_UTILISE("Ingrédient non utilisé");

    private final String label;

    RecipeIssueType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
