import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ingredient } from '../inventory/ingredient.fixtures';
import { Ingredient } from '../inventory/ingredient.model';
import { InventoryStore } from '../inventory/inventory-store';
import { generated } from './recipe.fixtures';
import { RECIPE_COUNT, RecipeStore } from './recipe-store';

describe('RecipeStore', () => {
  let http: HttpTestingController;
  let store: RecipeStore;
  const inventaire = signal<Ingredient[]>([]);

  beforeEach(() => {
    inventaire.set([
      ingredient({ id: 1, name: 'Lait', urgency: 'EXPIRED_OR_TODAY' }),
      ingredient({ id: 2, name: 'Courgettes', urgency: 'SOON' }),
      ingredient({ id: 3, name: 'Yaourts', urgency: 'WEEK' }),
      ingredient({ id: 4, name: 'Riz', urgency: 'LATER' }),
    ]);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: InventoryStore, useValue: { items: inventaire } }],
    });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(RecipeStore);
  });

  afterEach(() => http.verify());

  const requetes = (): TestRequest[] => http.match({ method: 'POST', url: '/api/recipes' });

  it('part de 2 personnes, sans limite de temps, avec les aliments urgents présélectionnés', () => {
    expect(store.servings()).toBe(2);
    expect(store.maxMinutes()).toBeNull();
    expect(store.selectedIds()).toEqual([1, 2]);
  });

  it('présélectionne au plus 5 aliments urgents, les plus urgents d’abord', () => {
    inventaire.set(
      Array.from({ length: 7 }, (_, i) => ingredient({ id: i + 1, urgency: 'SOON', name: `A${i + 1}` })),
    );

    expect(store.selectedIds()).toEqual([1, 2, 3, 4, 5]);
  });

  it('retire puis remet un aliment dans la sélection, et garde ce choix', () => {
    store.toggleIngredient(1);
    expect(store.selectedIds()).toEqual([2]);

    store.toggleIngredient(4);
    expect(store.selectedIds()).toEqual([2, 4]);
  });

  it('borne le nombre de personnes entre 1 et 12', () => {
    store.changeServings(-5);
    expect(store.servings()).toBe(1);

    store.changeServings(20);
    expect(store.servings()).toBe(12);

    store.changeServings(-1);
    expect(store.servings()).toBe(11);
  });

  it('retient le temps maximum choisi', () => {
    store.setMaxMinutes(30);
    expect(store.maxMinutes()).toBe(30);

    store.setMaxMinutes(null);
    expect(store.maxMinutes()).toBeNull();
  });

  it('lance trois requêtes en parallèle avec les réglages choisis', () => {
    store.changeServings(2);
    store.setMaxMinutes(30);
    store.toggleIngredient(1);

    store.generate();

    const reqs = requetes();
    expect(reqs.length).toBe(RECIPE_COUNT);
    reqs.forEach((req, index) =>
      expect(req.request.body).toEqual({ ingredientIds: [2], servings: 4, maxMinutes: 30, variant: index }),
    );
    expect(store.slots().map((s) => s.status)).toEqual(['loading', 'loading', 'loading']);
    expect(store.generating()).toBe(true);
    reqs.forEach((req) => req.flush(generated()));
  });

  it('affiche chaque recette dès sa réponse, sans attendre les autres', () => {
    store.generate();
    const [premiere] = requetes();

    premiere.flush(generated({ title: 'Soupe' }));

    expect(store.slots().map((s) => s.status)).toEqual(['done', 'loading', 'loading']);
    expect(store.result(0)?.recipe.title).toBe('Soupe');
    expect(store.result(1)).toBeNull();
    expect(store.generating()).toBe(true);
    requetes();
  });

  it('une recette en échec garde le message du serveur sans gêner les autres', () => {
    store.generate();
    const reqs = requetes();

    reqs[0].flush({ message: 'Réponse du LLM illisible' }, { status: 502, statusText: 'Bad Gateway' });
    reqs[1].flush(generated());
    reqs[2].flush(generated());

    expect(store.slots()[0]).toEqual({ status: 'error', message: 'Réponse du LLM illisible' });
    expect(store.slots()[1].status).toBe('done');
    expect(store.generating()).toBe(false);
  });

  it('propose un message par défaut quand le serveur est injoignable', () => {
    store.generate();
    const reqs = requetes();

    reqs[0].error(new ProgressEvent('error'));
    reqs[1].flush(generated());
    reqs[2].flush(generated());

    const slot = store.slots()[0];
    expect(slot.status).toBe('error');
    expect(slot.status === 'error' && slot.message).toBeTruthy();
  });

  it('ignore les réponses tardives d’une génération remplacée par une nouvelle', () => {
    store.generate();
    const anciennes = requetes();

    store.generate();
    const nouvelles = requetes();
    anciennes.forEach((req) => req.flush(generated({ title: 'Ancienne' })));
    nouvelles.forEach((req) => req.flush(generated({ title: 'Nouvelle' })));

    expect(store.slots().map((s) => (s.status === 'done' ? s.result.recipe.title : s.status))).toEqual([
      'Nouvelle',
      'Nouvelle',
      'Nouvelle',
    ]);
  });

  it('compte les aliments urgents utilisés par une recette', () => {
    const recette = generated({
      ingredients: [
        { ingredientId: 1, name: 'Lait', quantity: 1, unit: 'l' },
        { ingredientId: 2, name: 'Courgettes', quantity: 2, unit: 'pièce' },
        { ingredientId: 3, name: 'Yaourts', quantity: 2, unit: 'pièce' },
        { ingredientId: null, name: 'Sel', quantity: 1, unit: 'pincée' },
      ],
    }).recipe;

    expect(store.urgentSavedCount(recette)).toBe(2);
  });

  it('ajoute une recette du chat consultable par son index, sans toucher aux 3 cartes', () => {
    store.generate();
    requetes().forEach((req, i) => req.flush(generated({ title: `Carte ${i}` })));
    const duChat = generated({ title: 'Du chat' });

    const index = store.addFromChat(duChat);

    expect(store.result(index)).toEqual(duChat);
    expect(store.slots().map((s) => (s.status === 'done' ? s.result.recipe.title : s.status))).toEqual([
      'Carte 0',
      'Carte 1',
      'Carte 2',
    ]);
    expect([0, 1, 2].map((i) => store.result(i)?.recipe.title)).toEqual(['Carte 0', 'Carte 1', 'Carte 2']);
  });

  it('garde les recettes du chat après une nouvelle génération et donne des index distincts', () => {
    const a = generated({ title: 'A' });
    const b = generated({ title: 'B' });
    const indexA = store.addFromChat(a);
    const indexB = store.addFromChat(b);

    store.generate();

    expect(indexA).not.toBe(indexB);
    expect(store.result(indexA)).toEqual(a);
    expect(store.result(indexB)).toEqual(b);
    requetes();
  });

  it('renvoie le même index pour la même recette ajoutée deux fois', () => {
    const recette = generated();

    expect(store.addFromChat(recette)).toBe(store.addFromChat(recette));
  });
});
