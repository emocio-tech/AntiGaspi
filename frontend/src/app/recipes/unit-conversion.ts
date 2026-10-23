/** Facteurs vers l'unité de base de chaque famille (g, ml, pièce). */
const FACTORS: Record<string, { family: string; factor: number }> = {
  g: { family: 'masse', factor: 1 },
  kg: { family: 'masse', factor: 1000 },
  ml: { family: 'volume', factor: 1 },
  l: { family: 'volume', factor: 1000 },
  pièce: { family: 'pièce', factor: 1 },
  pièces: { family: 'pièce', factor: 1 },
  piece: { family: 'pièce', factor: 1 },
  pieces: { family: 'pièce', factor: 1 },
};

const normalize = (unit: string) => unit.trim().toLowerCase();

/**
 * Convertit une quantité de recette dans l'unité de l'inventaire, arrondie au millième.
 *
 * @returns la quantité convertie, ou `null` si les unités sont incompatibles ou inconnues
 */
export function toInventoryQuantity(quantity: number, recipeUnit: string, inventoryUnit: string): number | null {
  const from = normalize(recipeUnit);
  const to = normalize(inventoryUnit);
  if (from === to) return quantity;
  const source = FACTORS[from];
  const target = FACTORS[to];
  if (!source || !target || source.family !== target.family) return null;
  return Math.round(((quantity * source.factor) / target.factor) * 1000) / 1000;
}
