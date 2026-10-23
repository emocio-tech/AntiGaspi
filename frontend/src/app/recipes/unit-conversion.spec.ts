import { toInventoryQuantity } from './unit-conversion';

describe('toInventoryQuantity', () => {
  it('laisse la quantité inchangée pour la même unité, sans tenir compte de la casse ni des espaces', () => {
    expect(toInventoryQuantity(2.5, 'kg', 'kg')).toBe(2.5);
    expect(toInventoryQuantity(2.5, ' KG ', 'kg')).toBe(2.5);
  });

  it('convertit des grammes en kilogrammes', () => {
    expect(toInventoryQuantity(500, 'g', 'kg')).toBe(0.5);
  });

  it('convertit des litres en millilitres', () => {
    expect(toInventoryQuantity(0.25, 'l', 'ml')).toBe(250);
  });

  it('arrondit au millième pour éviter les erreurs de virgule flottante', () => {
    expect(toInventoryQuantity(300, 'g', 'kg')).toBe(0.3);
  });

  it('considère pièce, pièces et piece comme équivalents', () => {
    expect(toInventoryQuantity(2, 'pièces', 'pièce')).toBe(2);
    expect(toInventoryQuantity(2, 'piece', 'pièce')).toBe(2);
  });

  it('renvoie null pour des unités incompatibles', () => {
    expect(toInventoryQuantity(200, 'g', 'pièce')).toBeNull();
    expect(toInventoryQuantity(1, 'pincée', 'g')).toBeNull();
  });
});
