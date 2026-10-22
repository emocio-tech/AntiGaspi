import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { ingredient } from '../inventory/ingredient.fixtures';
import { Ingredient } from '../inventory/ingredient.model';
import { CookPage } from './cook-page';
import { generated } from './recipe.fixtures';
import { RecipeStore } from './recipe-store';

describe('CookPage', () => {
  let http: HttpTestingController;
  let store: RecipeStore;
  let router: Router;
  let navigation: ReturnType<typeof vi.spyOn>;
  let fixture: ComponentFixture<CookPage>;

  const courgettes = (surcharge: Partial<Ingredient> = {}) =>
    ingredient({ id: 2, name: 'Courgettes', category: 'LEGUMES', quantity: 3, unit: 'kg', ...surcharge });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CookPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(RecipeStore);
    router = TestBed.inject(Router);
    // La navigation est observée sur `Router.navigateByUrl` (neutralisé) : le composant doit appeler cette méthode.
    navigation = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  const html = () => fixture.nativeElement as HTMLElement;
  const q = (testId: string) => html().querySelector(`[data-testid=${testId}]`);
  const champ = <T extends HTMLElement>(testId: string) => q(testId) as T;
  const textes = (testId: string) => Array.from(html().querySelectorAll(`[data-testid=${testId}]`)).map((e) => e.textContent?.trim());

  /** Répond au GET de l'inventaire (le store root le lance au premier accès) avec les aliments donnés. */
  function repondreInventaire(aliments: Ingredient[]) {
    TestBed.tick();
    http.match('/api/ingredients').forEach((req) => req.flush(aliments));
  }

  /** Charge l'inventaire, fait recevoir `recette` au store, puis ouvre l'écran de la recette à `id`. */
  async function ouvrir(aliments: Ingredient[] = [courgettes()], recette = generated(), id = '0') {
    store.generate();
    http.match({ method: 'POST', url: '/api/recipes' }).forEach((req) => req.flush(recette));
    fixture = TestBed.createComponent(CookPage);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    repondreInventaire(aliments);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function saisir(testId: string, valeur: string) {
    const element = champ<HTMLInputElement | HTMLTextAreaElement>(testId);
    element.value = valeur;
    element.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  const cliquer = (testId: string) => {
    champ<HTMLButtonElement>(testId).click();
    fixture.detectChanges();
  };

  const valider = () => cliquer('cook-submit');

  const corpsAttendu = (surcharge = {}) => ({
    recipe: generated().recipe,
    rating: null,
    comment: null,
    consumed: [{ ingredientId: 2, quantity: 2.5 }],
    ...surcharge,
  });

  describe('lignes', () => {
    it('ne liste que les ingrédients de l’inventaire, avec nom, stock, quantité préremplie et unité', async () => {
      await ouvrir();

      expect(html().querySelectorAll('[data-testid=cook-line]').length).toBe(1);
      expect(textes('cook-name')).toEqual(['Courgettes']);
      expect(q('cook-stock')?.textContent).toContain('3 kg');
      expect(champ<HTMLInputElement>('cook-quantity').value).toBe('2.5');
      expect(q('cook-unit')?.textContent?.trim()).toBe('kg');
    });

    it('convertit la quantité de la recette dans l’unité de l’inventaire', async () => {
      const recette = generated({ ingredients: [{ ingredientId: 2, name: 'Courgettes', quantity: 500, unit: 'g' }] });

      await ouvrir([courgettes()], recette);

      expect(champ<HTMLInputElement>('cook-quantity').value).toBe('0.5');
    });

    it('laisse le champ vide quand les unités sont incompatibles', async () => {
      const recette = generated({ ingredients: [{ ingredientId: 2, name: 'Courgettes', quantity: 200, unit: 'g' }] });

      await ouvrir([courgettes({ unit: 'pièce', quantity: 4 })], recette);

      expect(champ<HTMLInputElement>('cook-quantity').value).toBe('');
      expect(q('cook-unit')?.textContent?.trim()).toBe('pièce');
    });

    it('invite à compléter la quantité quand l’unité de la recette est différente', async () => {
      const recette = generated({ ingredients: [{ ingredientId: 2, name: 'Courgettes', quantity: 200, unit: 'g' }] });

      await ouvrir([courgettes({ unit: 'pièce', quantity: 4 })], recette);

      expect(champ<HTMLInputElement>('cook-quantity').placeholder).toBe('À compléter');
      expect(q('cook-hint')?.textContent).toContain('Unité de la recette : g');
    });

    it('limite le commentaire à 1000 caractères', async () => {
      await ouvrir();

      expect(champ<HTMLTextAreaElement>('cook-comment').getAttribute('maxlength')).toBe('1000');
      expect(champ<HTMLTextAreaElement>('cook-comment').placeholder).toBe('Facultatif : ce qui vous a plu ou non');
    });

    it('désactive la validation tant que l’inventaire charge', async () => {
      store.generate();
      http.match({ method: 'POST', url: '/api/recipes' }).forEach((req) => req.flush(generated()));
      fixture = TestBed.createComponent(CookPage);
      fixture.componentRef.setInput('id', '0');
      fixture.detectChanges();
      TestBed.tick();
      fixture.detectChanges();

      try {
        expect(champ<HTMLButtonElement>('cook-submit').disabled).toBe(true);
        (fixture.componentInstance as unknown as { submit(): void }).submit();
        http.expectNone({ method: 'POST', url: '/api/cooked-recipes' });
      } finally {
        http.match('/api/ingredients').forEach((req) => req.flush([courgettes()]));
        http.match({ method: 'POST', url: '/api/cooked-recipes' }).forEach((req) => req.flush({}));
      }
    });

    it('signale un ingrédient déjà retiré de l’inventaire, sans champ', async () => {
      await ouvrir([ingredient({ id: 9 })]);

      expect(q('cook-missing')?.textContent).toContain('Déjà retiré de l’inventaire');
      expect(q('cook-quantity')).toBeNull();
    });
  });

  describe('avis', () => {
    it('bascule le pouce levé et le désélectionne au second clic', async () => {
      await ouvrir();

      cliquer('cook-up');
      expect(champ('cook-up').getAttribute('aria-pressed')).toBe('true');
      expect(champ('cook-down').getAttribute('aria-pressed')).toBe('false');

      cliquer('cook-up');
      expect(champ('cook-up').getAttribute('aria-pressed')).toBe('false');
    });

    it('passe d’un avis à l’autre', async () => {
      await ouvrir();

      cliquer('cook-up');
      cliquer('cook-down');

      expect(champ('cook-up').getAttribute('aria-pressed')).toBe('false');
      expect(champ('cook-down').getAttribute('aria-pressed')).toBe('true');
    });
  });

  describe('validation', () => {
    it('envoie la recette, sans avis, avec les quantités préremplies', async () => {
      await ouvrir();

      valider();

      const req = http.expectOne({ method: 'POST', url: '/api/cooked-recipes' });
      expect(req.request.body).toEqual(corpsAttendu());
      req.flush(null, { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      http.match('/api/ingredients').forEach((r) => r.flush([]));
    });

    it('envoie l’avis, le commentaire et les quantités modifiées', async () => {
      await ouvrir();

      cliquer('cook-down');
      saisir('cook-comment', 'Trop salé');
      saisir('cook-quantity', '1.5');
      valider();

      const req = http.expectOne({ method: 'POST', url: '/api/cooked-recipes' });
      expect(req.request.body).toEqual(
        corpsAttendu({ rating: 'DOWN', comment: 'Trop salé', consumed: [{ ingredientId: 2, quantity: 1.5 }] }),
      );
      req.flush(null, { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      http.match('/api/ingredients').forEach((r) => r.flush([]));
    });

    it('omet les lignes à 0 ou vides et envoie un commentaire vide comme null', async () => {
      const recette = generated({
        ingredients: [
          { ingredientId: 2, name: 'Courgettes', quantity: 2.5, unit: 'kg' },
          { ingredientId: 3, name: 'Lait', quantity: 1, unit: 'l' },
          { ingredientId: 4, name: 'Beurre', quantity: 100, unit: 'g' },
        ],
      });
      const stock = [courgettes(), ingredient({ id: 3 }), ingredient({ id: 4, name: 'Beurre', unit: 'g', quantity: 250 })];
      await ouvrir(stock, recette);

      saisir('cook-comment', '   ');
      const champs = Array.from(html().querySelectorAll<HTMLInputElement>('[data-testid=cook-quantity]'));
      champs[1].value = '0';
      champs[1].dispatchEvent(new Event('input'));
      champs[2].value = '';
      champs[2].dispatchEvent(new Event('input'));
      fixture.detectChanges();
      valider();

      const req = http.expectOne({ method: 'POST', url: '/api/cooked-recipes' });
      expect(req.request.body.consumed).toEqual([{ ingredientId: 2, quantity: 2.5 }]);
      expect(req.request.body.comment).toBeNull();
      req.flush(null, { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      http.match('/api/ingredients').forEach((r) => r.flush([]));
    });

    it('n’envoie pas la ligne d’un ingrédient déjà retiré de l’inventaire', async () => {
      const recette = generated({
        ingredients: [
          { ingredientId: 2, name: 'Courgettes', quantity: 2.5, unit: 'kg' },
          { ingredientId: 9, name: 'Tomates', quantity: 1, unit: 'kg' },
        ],
      });
      await ouvrir([courgettes()], recette);

      valider();

      const req = http.expectOne({ method: 'POST', url: '/api/cooked-recipes' });
      expect(req.request.body.consumed).toEqual([{ ingredientId: 2, quantity: 2.5 }]);
      req.flush(null, { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      http.match('/api/ingredients').forEach((r) => r.flush([]));
    });

    it('désactive le bouton quand une quantité est négative', async () => {
      await ouvrir();

      saisir('cook-quantity', '-1');

      expect(champ<HTMLButtonElement>('cook-submit').disabled).toBe(true);
    });

    it('n’envoie qu’une requête malgré un double clic', async () => {
      await ouvrir();

      valider();
      valider();

      const req = http.expectOne({ method: 'POST', url: '/api/cooked-recipes' });
      expect(champ<HTMLButtonElement>('cook-submit').disabled).toBe(true);
      req.flush(null, { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      http.match('/api/ingredients').forEach((r) => r.flush([]));
    });

    it('recharge l’inventaire et revient à l’inventaire après le succès', async () => {
      await ouvrir();

      valider();
      http.expectOne({ method: 'POST', url: '/api/cooked-recipes' }).flush(null, { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      TestBed.tick();

      http.expectOne('/api/ingredients').flush([]);
      expect(navigation).toHaveBeenCalledTimes(1);
      expect(navigation.mock.calls[0][0]).toBe('/inventaire');
    });

    it('affiche le message du serveur en cas de refus, garde la saisie et réactive le bouton', async () => {
      await ouvrir();
      saisir('cook-comment', 'Délicieux');
      saisir('cook-quantity', '1.5');

      valider();
      http
        .expectOne({ method: 'POST', url: '/api/cooked-recipes' })
        .flush({ message: 'Quantité supérieure au stock' }, { status: 400, statusText: 'Bad Request' });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(q('cook-error')?.textContent).toContain('Quantité supérieure au stock');
      expect(champ<HTMLTextAreaElement>('cook-comment').value).toBe('Délicieux');
      expect(champ<HTMLInputElement>('cook-quantity').value).toBe('1.5');
      expect(champ<HTMLButtonElement>('cook-submit').disabled).toBe(false);
      expect(navigation).not.toHaveBeenCalled();
    });
  });

  it('propose de revenir aux recettes quand la recette n’existe plus', async () => {
    await ouvrir([courgettes()], generated(), '7');

    expect(q('missing')).not.toBeNull();
    expect(html().querySelector('a[href="/recettes"]')).not.toBeNull();
  });
});
