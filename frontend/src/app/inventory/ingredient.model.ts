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

/** Code d'unité attendu par les écritures de l'API. */
export type UnitCode = 'G' | 'KG' | 'ML' | 'L' | 'PIECE';

/** Unités proposées à la saisie : code envoyé à l'API et symbole affiché. */
export const UNITS: readonly { code: UnitCode; symbol: string }[] = [
  { code: 'G', symbol: 'g' },
  { code: 'KG', symbol: 'kg' },
  { code: 'ML', symbol: 'ml' },
  { code: 'L', symbol: 'l' },
  { code: 'PIECE', symbol: 'pièce' },
];

/** Corps de `POST /api/ingredients` et `PUT /api/ingredients/{id}`. */
export interface IngredientRequest {
  name: string;
  category: string;
  quantity: number;
  unit: UnitCode;
  /** Date au format `2026-10-25`, ou `null` pour laisser le serveur l'estimer. */
  expiryDate: string | null;
}
