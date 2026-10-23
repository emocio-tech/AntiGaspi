import { ApplicationRef } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { cooked } from './history.fixtures';
import { HistoryStore } from './history-store';

describe('HistoryStore', () => {
  let http: HttpTestingController;
  let store: HistoryStore;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(HistoryStore);
  });

  afterEach(() => http.verify());

  /** Déclenche le chargement (l'effet de la ressource) et répond au GET. */
  async function charger(liste = [cooked({ id: 2 }), cooked({ id: 1 })]) {
    TestBed.tick();
    http.expectOne('/api/cooked-recipes').flush(liste);
    await stable();
  }

  it('charge les recettes cuisinées dans l’ordre reçu', async () => {
    TestBed.tick();
    expect(store.loading()).toBe(true);

    http.expectOne('/api/cooked-recipes').flush([cooked({ id: 2 }), cooked({ id: 1 })]);
    await stable();

    expect(store.loading()).toBe(false);
    expect(store.error()).toBe(false);
    expect(store.items().map((i) => i.id)).toEqual([2, 1]);
  });

  it('signale l’échec du chargement', async () => {
    TestBed.tick();

    http.expectOne('/api/cooked-recipes').flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    await stable();

    expect(store.error()).toBe(true);
    expect(store.items()).toEqual([]);
  });

  it('envoie l’avis exact en PUT puis recharge la liste', async () => {
    await charger([cooked({ id: 1 })]);

    const resultat = store.updateFeedback(1, { rating: 'UP', comment: 'Délicieux' });

    const put = http.expectOne({ method: 'PUT', url: '/api/cooked-recipes/1/feedback' });
    expect(put.request.body).toEqual({ rating: 'UP', comment: 'Délicieux' });
    put.flush(cooked({ id: 1, rating: 'UP', comment: 'Délicieux' }));
    expect(await resultat).toBe(true);
    expect(store.actionError()).toBeNull();

    TestBed.tick();
    http.expectOne('/api/cooked-recipes').flush([cooked({ id: 1, rating: 'UP', comment: 'Délicieux' })]);
    await stable();
    expect(store.items()[0].rating).toBe('UP');
  });

  it('garde la liste, expose le message du serveur et ne recharge pas quand l’avis est refusé', async () => {
    await charger([cooked({ id: 1 })]);

    const resultat = store.updateFeedback(1, { rating: null, comment: 'x'.repeat(1001) });
    http
      .expectOne({ method: 'PUT', url: '/api/cooked-recipes/1/feedback' })
      .flush({ message: 'Le commentaire est trop long' }, { status: 400, statusText: 'Bad Request' });

    expect(await resultat).toBe(false);
    expect(store.actionError()).toBe('Le commentaire est trop long');
    TestBed.tick();
    http.expectNone('/api/cooked-recipes');
    expect(store.items().map((i) => i.id)).toEqual([1]);
  });
});

function stable(): Promise<void> {
  return TestBed.inject(ApplicationRef).whenStable();
}
