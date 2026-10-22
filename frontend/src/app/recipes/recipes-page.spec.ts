import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ingredient } from '../inventory/ingredient.fixtures';
import { Ingredient } from '../inventory/ingredient.model';
import { InventoryStore } from '../inventory/inventory-store';
import { generated } from './recipe.fixtures';
import { RecipesPage } from './recipes-page';

describe('RecipesPage', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<RecipesPage>;
  const inventaire = signal<Ingredient[]>([]);

  beforeEach(() => {
    inventaire.set([
      ingredient({ id: 1, name: 'Lait', urgency: 'EXPIRED_OR_TODAY' }),
      ingredient({ id: 2, name: 'Courgettes', urgency: 'SOON' }),
      ingredient({ id: 3, name: 'Riz', urgency: 'LATER' }),
    ]);
    TestBed.configureTestingModule({
      imports: [RecipesPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: InventoryStore, useValue: { items: inventaire } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RecipesPage);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const html = () => fixture.nativeElement as HTMLElement;
  const q = (testId: string) => html().querySelector(`[data-testid=${testId}]`) as HTMLElement | null;
  const all = (testId: string) => Array.from(html().querySelectorAll(`[data-testid=${testId}]`)) as HTMLElement[];
  const cliquer = (element: HTMLElement | null) => {
    element?.click();
    fixture.detectChanges();
  };
  const requetes = () => http.match({ method: 'POST', url: '/api/recipes' });

  it('invite à générer tant qu’aucune recette n’est demandée', () => {
    expect(q('idle')).not.toBeNull();
    expect(q('generate')?.textContent).toContain('Générer 3 recettes');
  });

  it('propose les aliments de l’inventaire, les urgents étant présélectionnés', () => {
    const pastilles = all('ingredient-chip');

    expect(pastilles.map((p) => p.textContent?.trim())).toEqual(['Lait', 'Courgettes', 'Riz']);
    expect(pastilles.map((p) => p.getAttribute('aria-pressed'))).toEqual(['true', 'true', 'false']);
  });

  it('bascule la sélection d’un aliment au clic', () => {
    cliquer(all('ingredient-chip')[2]);
    cliquer(all('ingredient-chip')[0]);

    expect(all('ingredient-chip').map((p) => p.getAttribute('aria-pressed'))).toEqual(['false', 'true', 'true']);
  });

  it('règle le nombre de personnes et le temps maximum', () => {
    cliquer(q('servings-plus'));
    cliquer(q('servings-plus'));
    cliquer(q('servings-minus'));
    cliquer(all('time-option')[1]);

    expect(q('servings')?.textContent?.trim()).toBe('3');
    expect(all('time-option').map((o) => o.getAttribute('aria-pressed'))).toEqual(['false', 'true', 'false', 'false']);
  });

  it('lance la génération avec les réglages et affiche l’attente', () => {
    cliquer(q('servings-plus'));
    cliquer(all('time-option')[2]);

    cliquer(q('generate'));

    const reqs = requetes();
    expect(reqs.length).toBe(3);
    expect(reqs[0].request.body).toEqual({ ingredientIds: [1, 2], servings: 3, maxMinutes: 45 });
    expect(q('loading')?.textContent).toContain('Nova Lite compose vos recettes');
    reqs.forEach((req) => req.flush(generated()));
  });

  it('affiche une carte par recette avec ses informations et un lien vers le détail', async () => {
    cliquer(q('generate'));
    requetes().forEach((req) => req.flush(generated()));
    await fixture.whenStable();
    fixture.detectChanges();

    const cartes = all('recipe-card');
    expect(cartes.length).toBe(3);
    const premiere = cartes[0];
    expect(premiere.textContent).toContain('Gratin de courgettes');
    expect(premiere.textContent).toContain('2 pers.');
    expect(premiere.textContent).toContain('35 min');
    expect(premiere.textContent).toContain('Courgettes');
    expect(premiere.textContent).toContain('Sauve 1 produit urgent');
    expect(premiere.getAttribute('href')).toBe('/recettes/0');
    expect(cartes[2].getAttribute('href')).toBe('/recettes/2');
    expect(q('idle')).toBeNull();
  });

  it('affiche l’échec d’une recette sans cacher les autres', async () => {
    cliquer(q('generate'));
    const reqs = requetes();
    reqs[0].flush({ message: 'Réponse du LLM illisible' }, { status: 502, statusText: 'Bad Gateway' });
    reqs[1].flush(generated());
    reqs[2].flush(generated());
    await fixture.whenStable();
    fixture.detectChanges();

    expect(q('recipe-error')?.textContent).toContain('Réponse du LLM illisible');
    expect(all('recipe-card').length).toBe(2);
  });

  it('empêche de relancer pendant la génération', () => {
    cliquer(q('generate'));

    expect((q('generate') as HTMLButtonElement).disabled).toBe(true);
    requetes().forEach((req) => req.flush(generated()));
  });

  it('désactive la génération quand l’inventaire est vide', () => {
    inventaire.set([]);
    fixture.detectChanges();

    expect((q('generate') as HTMLButtonElement).disabled).toBe(true);
    expect(q('empty-inventory')).not.toBeNull();
  });
});
