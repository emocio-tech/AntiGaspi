import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ingredient } from './ingredient.fixtures';
import { InventoryStore } from './inventory-store';

describe('InventoryStore', () => {
  let http: HttpTestingController;
  let store: InventoryStore;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(InventoryStore);
  });

  afterEach(() => http.verify());

  /** La requête part après le premier cycle de détection, la réponse est appliquée de façon asynchrone. */
  function demarrer() {
    TestBed.tick();
    return http.expectOne('/api/ingredients');
  }

  const stabiliser = () => TestBed.inject(ApplicationRef).whenStable();

  it('charge l’inventaire depuis /api/ingredients', async () => {
    expect(store.loading()).toBe(true);

    demarrer().flush([ingredient({ id: 1, name: 'Lait' }), ingredient({ id: 2, name: 'Beurre' })]);
    await stabiliser();

    expect(store.loading()).toBe(false);
    expect(store.error()).toBe(false);
    expect(store.items().map((i) => i.name)).toEqual(['Lait', 'Beurre']);
  });

  it('compte les aliments à consommer aujourd’hui ou sous 3 jours', async () => {
    demarrer().flush([
      ingredient({ id: 1, urgency: 'EXPIRED_OR_TODAY' }),
      ingredient({ id: 2, urgency: 'SOON' }),
      ingredient({ id: 3, urgency: 'WEEK' }),
      ingredient({ id: 4, urgency: 'LATER' }),
    ]);
    await stabiliser();

    expect(store.urgentCount()).toBe(2);
  });

  it('signale l’erreur quand l’API échoue, sans aliment', async () => {
    demarrer().flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    await stabiliser();

    expect(store.error()).toBe(true);
    expect(store.loading()).toBe(false);
    expect(store.items()).toEqual([]);
    expect(store.urgentCount()).toBe(0);
  });

  it('recharge l’inventaire à la demande', async () => {
    demarrer().flush([ingredient({ name: 'Lait' })]);
    await stabiliser();

    store.reload();
    TestBed.tick();
    http.expectOne('/api/ingredients').flush([ingredient({ name: 'Beurre' })]);
    await stabiliser();

    expect(store.items().map((i) => i.name)).toEqual(['Beurre']);
  });
});
