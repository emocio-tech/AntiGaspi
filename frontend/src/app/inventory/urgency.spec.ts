import { ingredient } from './ingredient.fixtures';
import { formatQuantity, groupByUrgency, urgencyLabel } from './urgency';

describe('urgencyLabel', () => {
  it('indique « Périmé » pour un aliment dont la date est passée', () => {
    expect(urgencyLabel({ daysLeft: -1, estimated: false })).toBe('Périmé');
  });

  it("indique « Aujourd'hui » et « Demain » pour 0 et 1 jour", () => {
    expect(urgencyLabel({ daysLeft: 0, estimated: false })).toBe("Aujourd'hui");
    expect(urgencyLabel({ daysLeft: 1, estimated: false })).toBe('Demain');
  });

  it('compte en jours jusqu’à deux semaines, puis en semaines, puis en mois', () => {
    expect(urgencyLabel({ daysLeft: 5, estimated: false })).toBe('5 jours');
    expect(urgencyLabel({ daysLeft: 14, estimated: false })).toBe('14 jours');
    expect(urgencyLabel({ daysLeft: 21, estimated: false })).toBe('3 sem.');
    expect(urgencyLabel({ daysLeft: 90, estimated: false })).toBe('3 mois');
  });

  it('préfixe « ≈ » une durée estimée, mais pas « Périmé », « Aujourd’hui » ni « Demain »', () => {
    expect(urgencyLabel({ daysLeft: 5, estimated: true })).toBe('≈ 5 jours');
    expect(urgencyLabel({ daysLeft: 21, estimated: true })).toBe('≈ 3 sem.');
    expect(urgencyLabel({ daysLeft: -1, estimated: true })).toBe('Périmé');
    expect(urgencyLabel({ daysLeft: 0, estimated: true })).toBe("Aujourd'hui");
    expect(urgencyLabel({ daysLeft: 1, estimated: true })).toBe('Demain');
  });
});

describe('formatQuantity', () => {
  it('met les pièces au pluriel au-delà de 1', () => {
    expect(formatQuantity(1, 'pièce')).toBe('1 pièce');
    expect(formatQuantity(4, 'pièce')).toBe('4 pièces');
  });

  it('utilise la virgule décimale', () => {
    expect(formatQuantity(2.5, 'kg')).toBe('2,5 kg');
  });

  it('convertit en g ou ml sous 1 kg ou 1 l', () => {
    expect(formatQuantity(0.25, 'kg')).toBe('250 g');
    expect(formatQuantity(0.5, 'l')).toBe('500 ml');
  });

  it('garde les g et ml tels quels', () => {
    expect(formatQuantity(200, 'g')).toBe('200 g');
    expect(formatQuantity(330, 'ml')).toBe('330 ml');
  });
});

describe('groupByUrgency', () => {
  it("regroupe dans l'ordre d'urgence et conserve l'ordre de l'API dans un groupe", () => {
    const groups = groupByUrgency([
      ingredient({ id: 1, name: 'Yaourts', urgency: 'LATER' }),
      ingredient({ id: 2, name: 'Lait', urgency: 'EXPIRED_OR_TODAY' }),
      ingredient({ id: 3, name: 'Courgettes', urgency: 'SOON' }),
      ingredient({ id: 4, name: 'Beurre', urgency: 'EXPIRED_OR_TODAY' }),
    ]);

    expect(groups.map((g) => g.urgency)).toEqual(['EXPIRED_OR_TODAY', 'SOON', 'LATER']);
    expect(groups[0].items.map((i) => i.name)).toEqual(['Lait', 'Beurre']);
  });

  it('ne renvoie aucun groupe pour un inventaire vide', () => {
    expect(groupByUrgency([])).toEqual([]);
  });
});
