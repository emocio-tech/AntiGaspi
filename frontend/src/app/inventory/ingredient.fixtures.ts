import { Ingredient } from './ingredient.model';

/** Aliment de test : périmé hier par défaut, surchargeable champ par champ. */
export function ingredient(overrides: Partial<Ingredient> = {}): Ingredient {
  return {
    id: 1,
    name: 'Lait',
    category: 'PRODUITS_LAITIERS',
    categoryLabel: 'Produits laitiers',
    quantity: 1,
    unit: 'l',
    expiryDate: '2026-10-19',
    daysLeft: -1,
    estimated: false,
    urgency: 'EXPIRED_OR_TODAY',
    ...overrides,
  };
}
