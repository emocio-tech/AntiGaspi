package fr.antigaspi.ingredient;

/**
 * Niveau d'urgence d'un aliment selon le nombre de jours restants avant péremption.
 */
public enum Urgency {

    /** Périmé ou à consommer aujourd'hui. */
    EXPIRED_OR_TODAY("🔴"),
    /** À consommer sous {@value #SOON_MAX_DAYS} jours. */
    SOON("🟠"),
    /** À consommer sous {@value #WEEK_MAX_DAYS} jours. */
    WEEK("🟡"),
    /** Plus de {@value #WEEK_MAX_DAYS} jours. */
    LATER("🟢");

    public static final int SOON_MAX_DAYS = 3;
    public static final int WEEK_MAX_DAYS = 7;

    private final String emoji;

    Urgency(String emoji) {
        this.emoji = emoji;
    }

    public String emoji() {
        return emoji;
    }

    /**
     * Urgence correspondant à un nombre de jours restants (négatif si l'aliment est périmé).
     */
    public static Urgency of(long daysLeft) {
        if (daysLeft <= 0) {
            return EXPIRED_OR_TODAY;
        }
        if (daysLeft <= SOON_MAX_DAYS) {
            return SOON;
        }
        if (daysLeft <= WEEK_MAX_DAYS) {
            return WEEK;
        }
        return LATER;
    }
}
