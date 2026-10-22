import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { InventoryStore } from '../inventory/inventory-store';
import { generated } from './recipe.fixtures';
import { RecipeDetailPage } from './recipe-detail-page';
import { RecipeStore } from './recipe-store';

describe('RecipeDetailPage', () => {
  let http: HttpTestingController;
  let store: RecipeStore;
  let fixture: ComponentFixture<RecipeDetailPage>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RecipeDetailPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: InventoryStore, useValue: { items: signal([]) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(RecipeStore);
  });

  afterEach(() => http.verify());

  const html = () => fixture.nativeElement as HTMLElement;
  const q = (testId: string) => html().querySelector(`[data-testid=${testId}]`);
  const textes = (testId: string) => Array.from(html().querySelectorAll(`[data-testid=${testId}]`)).map((e) => e.textContent?.trim());

  /** Fait recevoir une recette au store, puis ouvre le détail de la recette à `id`. */
  function ouvrir(id: string, recette = generated()) {
    store.generate();
    http.match({ method: 'POST', url: '/api/recipes' }).forEach((req) => req.flush(recette));
    fixture = TestBed.createComponent(RecipeDetailPage);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
  }

  it('affiche le titre, les personnes, la durée et les ustensiles', () => {
    ouvrir('0');

    expect(html().querySelector('h1')?.textContent).toContain('Gratin de courgettes');
    expect(q('meta')?.textContent).toContain('2 personnes');
    expect(q('meta')?.textContent).toContain('35 min');
    expect(textes('utensil')).toEqual(['Four', 'Poêle']);
  });

  it('liste les ingrédients avec leur quantité lisible', () => {
    ouvrir('0');

    expect(textes('ingredient')).toEqual(['Courgettes2,5 kg', 'Sel1 pincée']);
  });

  it('numérote les étapes dans l’ordre', () => {
    ouvrir('1');

    expect(textes('step')).toEqual(['Couper les courgettes.', 'Enfourner 25 minutes.']);
  });

  it('signale les anomalies du garde-fou', () => {
    ouvrir('0', generated({}, [{ type: 'USTENSILE_INDISPONIBLE', label: 'Ustensile indisponible', message: '« Four » n’est pas disponible' }]));

    expect(q('issues')?.textContent).toContain('Ustensile indisponible');
    expect(q('issues')?.textContent).toContain('« Four » n’est pas disponible');
  });

  it('n’affiche pas de bloc d’anomalies pour une recette conforme', () => {
    ouvrir('0');

    expect(q('issues')).toBeNull();
  });

  it('propose de revenir aux recettes quand la recette n’existe plus', () => {
    fixture = TestBed.createComponent(RecipeDetailPage);
    fixture.componentRef.setInput('id', '7');
    fixture.detectChanges();

    expect(q('missing')).not.toBeNull();
    expect(html().querySelector('a[href="/recettes"]')).not.toBeNull();
  });

  it('propose de revenir à la liste depuis une recette', () => {
    ouvrir('0');

    expect(html().querySelector('a[href="/recettes"]')).not.toBeNull();
  });

  it('propose de cuisiner la recette', () => {
    ouvrir('0');

    expect(q('cook-link')?.getAttribute('href')).toBe('/recettes/0/cuisine');
    expect(q('cook-link')?.textContent).toContain('J’ai cuisiné cette recette');
  });
});
