import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ingredient } from './ingredient.fixtures';
import { IngredientRequest } from './ingredient.model';
import { InventoryStore, REMOVAL_DELAY_MS } from './inventory-store';

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

  const requete: IngredientRequest = {
    name: 'Beurre',
    category: 'PRODUITS_LAITIERS',
    quantity: 250,
    unit: 'G',
    expiryDate: '2026-10-25',
  };
  const erreur = (message: string) => [{ message }, { status: 400, statusText: 'Bad Request' }] as const;

  /** Charge l'inventaire de départ. */
  async function charger(aliments = [ingredient({ id: 1, name: 'Lait' }), ingredient({ id: 2, name: 'Beurre' })]) {
    demarrer().flush(aliments);
    await stabiliser();
  }

  describe('ajout et modification', () => {
    it('ajoute un aliment par POST puis recharge l’inventaire', async () => {
      await charger([ingredient({ id: 1, name: 'Lait' })]);

      const resultat = store.add(requete);

      const post = http.expectOne({ method: 'POST', url: '/api/ingredients' });
      expect(post.request.body).toEqual(requete);
      post.flush(ingredient({ id: 2, name: 'Beurre' }), { status: 201, statusText: 'Created' });
      expect(await resultat).toBe(true);
      TestBed.tick();
      http.expectOne({ method: 'GET', url: '/api/ingredients' }).flush([ingredient({ id: 1 }), ingredient({ id: 2, name: 'Beurre' })]);
      await stabiliser();
      expect(store.items().map((i) => i.name)).toEqual(['Lait', 'Beurre']);
    });

    it('affiche le message du serveur et ne recharge pas quand l’ajout est refusé', async () => {
      await charger();

      const resultat = store.add(requete);
      http.expectOne({ method: 'POST', url: '/api/ingredients' }).flush(...erreur('Quantité invalide'));

      expect(await resultat).toBe(false);
      expect(store.actionError()).toBe('Quantité invalide');
      TestBed.tick();
      http.expectNone('/api/ingredients');
    });

    it('modifie un aliment par PUT puis recharge l’inventaire', async () => {
      await charger();

      const resultat = store.update(2, requete);

      const put = http.expectOne({ method: 'PUT', url: '/api/ingredients/2' });
      expect(put.request.body).toEqual(requete);
      put.flush(ingredient({ id: 2, name: 'Beurre' }));
      expect(await resultat).toBe(true);
      TestBed.tick();
      http.expectOne({ method: 'GET', url: '/api/ingredients' }).flush([ingredient({ id: 1 }), ingredient({ id: 2, name: 'Beurre doux' })]);
      await stabiliser();
      expect(store.items()[1].name).toBe('Beurre doux');
    });

    it('affiche le message du serveur et ne recharge pas quand la modification est refusée', async () => {
      await charger();

      const resultat = store.update(2, requete);
      http.expectOne({ method: 'PUT', url: '/api/ingredients/2' }).flush(...erreur('Aliment inconnu'));

      expect(await resultat).toBe(false);
      expect(store.actionError()).toBe('Aliment inconnu');
      TestBed.tick();
      http.expectNone('/api/ingredients');
    });
  });

  describe('suppression différée', () => {
    beforeEach(() => vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] }));
    afterEach(() => vi.useRealTimers());

    const suppression = (ids: string) => ({ method: 'DELETE', url: `/api/ingredients?ids=${ids}` });
    const aucuneSuppression = () => http.expectNone((req) => req.method === 'DELETE');

    it('masque tout de suite les aliments retirés, sans requête avant le délai', async () => {
      await charger([
        ingredient({ id: 1, name: 'Lait', urgency: 'SOON' }),
        ingredient({ id: 2, name: 'Beurre', urgency: 'WEEK' }),
        ingredient({ id: 3, name: 'Yaourts', urgency: 'SOON' }),
      ]);

      store.scheduleRemoval([1, 3]);

      expect(store.items().map((i) => i.name)).toEqual(['Beurre']);
      expect(store.urgentCount()).toBe(0);
      expect(store.pendingRemoval()).toBe(2);
      vi.advanceTimersByTime(REMOVAL_DELAY_MS - 1);
      aucuneSuppression();
    });

    it('envoie la suppression à l’expiration du délai puis recharge l’inventaire', async () => {
      await charger([ingredient({ id: 1 }), ingredient({ id: 2, name: 'Beurre' }), ingredient({ id: 3 })]);
      store.scheduleRemoval([1, 3]);

      vi.advanceTimersByTime(REMOVAL_DELAY_MS);

      http.expectOne(suppression('1,3')).flush(null, { status: 204, statusText: 'No Content' });
      TestBed.tick();
      http.expectOne({ method: 'GET', url: '/api/ingredients' }).flush([ingredient({ id: 2, name: 'Beurre' })]);
      await stabiliser();
      expect(store.items().map((i) => i.name)).toEqual(['Beurre']);
      expect(store.pendingRemoval()).toBe(0);
    });

    it('annule le retrait : les aliments reviennent et aucune requête n’est envoyée', async () => {
      await charger();
      store.scheduleRemoval([1, 2]);

      store.undo();
      vi.advanceTimersByTime(10_000);

      expect(store.items().map((i) => i.name)).toEqual(['Lait', 'Beurre']);
      expect(store.pendingRemoval()).toBe(0);
      aucuneSuppression();
    });

    it('envoie tout de suite le retrait en attente quand un second est demandé', async () => {
      await charger([ingredient({ id: 1 }), ingredient({ id: 2 }), ingredient({ id: 3 })]);
      store.scheduleRemoval([1]);

      store.scheduleRemoval([3]);

      http.expectOne(suppression('1')).flush(null, { status: 204, statusText: 'No Content' });
      expect(store.items().map((i) => i.id)).toEqual([2]);
      expect(store.pendingRemoval()).toBe(1);
      TestBed.tick();
      http.match('/api/ingredients').forEach((req) => req.flush([ingredient({ id: 2 }), ingredient({ id: 3 })]));
      vi.advanceTimersByTime(REMOVAL_DELAY_MS);
      http.expectOne(suppression('3')).flush(null, { status: 204, statusText: 'No Content' });
      TestBed.tick();
      http.match('/api/ingredients').forEach((req) => req.flush([ingredient({ id: 2 })]));
    });

    it('réaffiche les aliments, recharge et signale l’erreur quand la suppression échoue', async () => {
      await charger();
      store.scheduleRemoval([1]);
      vi.advanceTimersByTime(REMOVAL_DELAY_MS);

      http.expectOne(suppression('1')).flush(...erreur('Suppression impossible'));

      expect(store.actionError()).toBe('Suppression impossible');
      expect(store.items().map((i) => i.name)).toEqual(['Lait', 'Beurre']);
      TestBed.tick();
      http.expectOne({ method: 'GET', url: '/api/ingredients' }).flush([ingredient({ id: 1 }), ingredient({ id: 2 })]);
    });
  });
});
