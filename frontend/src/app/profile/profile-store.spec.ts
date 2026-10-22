import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProfileStore } from './profile-store';

describe('ProfileStore', () => {
  let http: HttpTestingController;
  let store: ProfileStore;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(ProfileStore);
  });

  afterEach(() => http.verify());

  /** Charge un profil et des ustensiles de départ. */
  function charger(preferences: string[] = ['Sans coriandre'], utensils = [{ id: 1, name: 'Poêle' }, { id: 3, name: 'Casserole' }]) {
    store.load();
    http.expectOne('/api/profile').flush({ preferences });
    http.expectOne('/api/utensils').flush(utensils);
  }

  it('charge les préférences et les ustensiles', () => {
    expect(store.loading()).toBe(true);

    charger(['Végétarien le midi', 'Sans coriandre']);

    expect(store.loading()).toBe(false);
    expect(store.loadError()).toBe(false);
    expect(store.preferences()).toEqual(['Végétarien le midi', 'Sans coriandre']);
    expect(store.utensils().map((u) => u.name)).toEqual(['Poêle', 'Casserole']);
  });

  it('signale l’échec du chargement', () => {
    store.load();
    http.expectOne('/api/profile').flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/utensils').flush([]);

    expect(store.loadError()).toBe(true);
    expect(store.loading()).toBe(false);
  });

  it('ajoute une préférence en envoyant la liste complète, puis affiche la réponse du serveur', () => {
    charger(['Sans coriandre']);

    store.addPreference('  Sans gluten ');

    const req = http.expectOne({ method: 'PUT', url: '/api/profile' });
    expect(req.request.body).toEqual({ preferences: ['Sans coriandre', 'Sans gluten'] });
    req.flush({ preferences: ['Sans coriandre', 'Sans gluten'] });
    expect(store.preferences()).toEqual(['Sans coriandre', 'Sans gluten']);
    expect(store.actionError()).toBeNull();
  });

  it('n’envoie rien pour une préférence vide', () => {
    charger();

    store.addPreference('   ');

    http.expectNone('/api/profile');
  });

  it('garde la liste et affiche le message du serveur quand une préférence est refusée', () => {
    charger(['Sans coriandre']);

    store.addPreference('sans CORIANDRE');
    http
      .expectOne({ method: 'PUT', url: '/api/profile' })
      .flush({ message: 'La préférence « sans CORIANDRE » est en double' }, { status: 400, statusText: 'Bad Request' });

    expect(store.preferences()).toEqual(['Sans coriandre']);
    expect(store.actionError()).toBe('La préférence « sans CORIANDRE » est en double');
  });

  it('retire une préférence en envoyant la liste sans elle', () => {
    charger(['Sans coriandre', 'Sans gluten']);

    store.removePreference('Sans coriandre');

    const req = http.expectOne({ method: 'PUT', url: '/api/profile' });
    expect(req.request.body).toEqual({ preferences: ['Sans gluten'] });
    req.flush({ preferences: ['Sans gluten'] });
    expect(store.preferences()).toEqual(['Sans gluten']);
  });

  it('ajoute un ustensile et le range par ordre alphabétique', () => {
    charger();

    store.addUtensil('four');

    const req = http.expectOne({ method: 'POST', url: '/api/utensils' });
    expect(req.request.body).toEqual({ name: 'four' });
    req.flush({ id: 4, name: 'four' }, { status: 201, statusText: 'Created' });
    expect(store.utensils().map((u) => u.name)).toEqual(['Casserole', 'four', 'Poêle']);
  });

  it('affiche le message du serveur quand un ustensile est refusé', () => {
    charger();

    store.addUtensil('poêle');
    http
      .expectOne({ method: 'POST', url: '/api/utensils' })
      .flush({ message: 'L\'ustensile « Poêle » est déjà dans la liste' }, { status: 400, statusText: 'Bad Request' });

    expect(store.utensils().length).toBe(2);
    expect(store.actionError()).toBe('L\'ustensile « Poêle » est déjà dans la liste');
  });

  it('retire un ustensile', () => {
    charger();

    store.removeUtensil(1);

    http.expectOne({ method: 'DELETE', url: '/api/utensils/1' }).flush(null, { status: 204, statusText: 'No Content' });
    expect(store.utensils().map((u) => u.name)).toEqual(['Casserole']);
  });

  it('efface l’erreur précédente à la modification suivante', () => {
    charger(['Sans coriandre']);
    store.addPreference('sans coriandre');
    http.expectOne({ method: 'PUT', url: '/api/profile' }).flush({ message: 'doublon' }, { status: 400, statusText: 'Bad Request' });
    expect(store.actionError()).toBe('doublon');

    store.addPreference('Sans gluten');

    expect(store.actionError()).toBeNull();
    http.expectOne({ method: 'PUT', url: '/api/profile' }).flush({ preferences: ['Sans coriandre', 'Sans gluten'] });
  });
});
