/** Code d'urgence renvoyé par l'API, du plus pressant au moins pressant. */
export type Urgency = 'EXPIRED_OR_TODAY' | 'SOON' | 'WEEK' | 'LATER';

/** Aliment de l'inventaire tel que l'API `GET /api/ingredients` le renvoie. */
export interface Ingredient {
  id: number;
  name: string;
  category: string;
  categoryLabel: string;
  quantity: number;
  /** Symbole de l'unité : g, kg, ml, l, pièce. */
  unit: string;
  expiryDate: string;
  /** Jours restants avant péremption, négatif si périmé. */
  daysLeft: number;
  /** `true` si la date de péremption est estimée d'après la catégorie. */
  estimated: boolean;
  urgency: Urgency;
}
