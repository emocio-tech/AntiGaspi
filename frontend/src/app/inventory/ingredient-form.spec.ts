import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ingredient } from './ingredient.fixtures';
import { Ingredient } from './ingredient.model';
import { IngredientForm } from './ingredient-form';

describe('IngredientForm', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<IngredientForm>;

  const categories = [
    { code: 'PRODUITS_LAITIERS', label: 'Produits laitiers' },
    { code: 'LEGUMES', label: 'Légumes' },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [IngredientForm],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  const html = () => fixture.nativeElement as HTMLElement;
  const champ = <T extends HTMLElement>(id: string) => html().querySelector(`[data-testid=${id}]`) as T;
  const saisie = (id: string) => champ<HTMLInputElement>(id);
  const liste = (id: string) => champ<HTMLSelectElement>(id);

  /** Répond à l'éventuel rechargement de l'inventaire déclenché par l'ajout ou la modification. */
  function viderInventaire() {
    TestBed.tick();
    http.match('/api/ingredients').forEach((req) => req.flush([]));
  }

  async function ouvrir(aliment: Ingredient | null = null) {
    fixture = TestBed.createComponent(IngredientForm);
    fixture.componentRef.setInput('ingredient', aliment);
    fixture.detectChanges();
    viderInventaire();
    http.expectOne('/api/ingredients/categories').flush(categories);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function remplir(id: string, valeur: string, evenement = 'input') {
    const element = champ<HTMLInputElement | HTMLSelectElement>(id);
    element.value = valeur;
    element.dispatchEvent(new Event(evenement));
    fixture.detectChanges();
  }

  function remplirTout(nom = 'Beurre', quantite = '250', date = '2026-10-25') {
    remplir('form-name', nom);
    remplir('form-category', 'PRODUITS_LAITIERS', 'change');
    remplir('form-quantity', quantite);
    remplir('form-unit', 'G', 'change');
    remplir('form-expiry', date);
  }

  const envoyer = () => {
    champ<HTMLButtonElement>('form-submit').click();
    fixture.detectChanges();
  };

  const corps = (surcharge = {}) => ({
    name: 'Beurre',
    category: 'PRODUITS_LAITIERS',
    quantity: 250,
    unit: 'G',
    expiryDate: '2026-10-25',
    ...surcharge,
  });

  describe('ajout', () => {
    it('propose les catégories du serveur et les unités', async () => {
      await ouvrir();

      const categoriesAffichees = Array.from(liste('form-category').options).map((o) => o.value);
      expect(categoriesAffichees).toEqual(expect.arrayContaining(['PRODUITS_LAITIERS', 'LEGUMES']));
      expect(html().textContent).toContain('Produits laitiers');
      const unites = Array.from(liste('form-unit').options).map((o) => o.value);
      expect(unites).toEqual(expect.arrayContaining(['G', 'KG', 'ML', 'L', 'PIECE']));
    });

    it('envoie le formulaire en POST avec la date saisie', async () => {
      await ouvrir();
      remplirTout();

      envoyer();

      const req = http.expectOne({ method: 'POST', url: '/api/ingredients' });
      expect(req.request.body).toEqual(corps());
      req.flush(ingredient({ id: 9 }), { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      viderInventaire();
    });

    it('envoie une date nulle quand elle est vide', async () => {
      await ouvrir();
      remplirTout('Beurre', '250', '');

      envoyer();

      const req = http.expectOne({ method: 'POST', url: '/api/ingredients' });
      expect(req.request.body).toEqual(corps({ expiryDate: null }));
      req.flush(ingredient({ id: 9 }), { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      viderInventaire();
    });

    it('reste ouvert après un ajout : nom, quantité et date vidés, catégorie et unité conservées, focus sur le nom', async () => {
      await ouvrir();
      remplirTout();
      const fermeture = vi.fn();
      fixture.componentInstance.closed.subscribe(fermeture);

      envoyer();
      http.expectOne({ method: 'POST', url: '/api/ingredients' }).flush(ingredient({ id: 9 }), { status: 201, statusText: 'Created' });
      await fixture.whenStable();
      viderInventaire();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(saisie('form-name').value).toBe('');
      expect(saisie('form-quantity').value).toBe('');
      expect(saisie('form-expiry').value).toBe('');
      expect(liste('form-category').value).toBe('PRODUITS_LAITIERS');
      expect(liste('form-unit').value).toBe('G');
      expect(document.activeElement).toBe(saisie('form-name'));
      expect(fermeture).not.toHaveBeenCalled();
    });

    it.each([
      ['le nom est vide', () => remplir('form-name', '')],
      ['le nom est blanc', () => remplir('form-name', '   ')],
      ['la quantité est vide', () => remplir('form-quantity', '')],
      ['la quantité est nulle', () => remplir('form-quantity', '0')],
      ['la quantité est négative', () => remplir('form-quantity', '-3')],
      ['la catégorie est absente', () => remplir('form-category', '', 'change')],
      ['l’unité est absente', () => remplir('form-unit', '', 'change')],
    ])('désactive l’envoi quand %s', async (_cas, rendreInvalide) => {
      await ouvrir();
      remplirTout();
      expect(champ<HTMLButtonElement>('form-submit').disabled).toBe(false);

      rendreInvalide();
      envoyer();

      expect(champ<HTMLButtonElement>('form-submit').disabled).toBe(true);
      http.expectNone({ method: 'POST', url: '/api/ingredients' });
    });

    it('affiche le message du serveur et conserve les champs quand l’ajout est refusé', async () => {
      await ouvrir();
      remplirTout();

      envoyer();
      http
        .expectOne({ method: 'POST', url: '/api/ingredients' })
        .flush({ message: 'Ce nom existe déjà' }, { status: 400, statusText: 'Bad Request' });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(champ('form-error').textContent).toContain('Ce nom existe déjà');
      expect(saisie('form-name').value).toBe('Beurre');
      expect(saisie('form-quantity').value).toBe('250');
      expect(saisie('form-expiry').value).toBe('2026-10-25');
    });

    it('émet closed quand on ferme le formulaire', async () => {
      await ouvrir();
      const fermeture = vi.fn();
      fixture.componentInstance.closed.subscribe(fermeture);

      champ<HTMLButtonElement>('form-close').click();

      expect(fermeture).toHaveBeenCalledTimes(1);
    });
  });

  describe('modification', () => {
    const courgettes = () =>
      ingredient({ id: 5, name: 'Courgettes', category: 'LEGUMES', quantity: 2.5, unit: 'kg', expiryDate: '2026-10-25' });

    it('préremplit les champs, l’unité étant convertie du symbole vers le code', async () => {
      await ouvrir(courgettes());

      expect(saisie('form-name').value).toBe('Courgettes');
      expect(liste('form-category').value).toBe('LEGUMES');
      expect(saisie('form-quantity').value).toBe('2.5');
      expect(liste('form-unit').value).toBe('KG');
      expect(saisie('form-expiry').value).toBe('2026-10-25');
    });

    it('reprend les champs du nouvel aliment quand on passe à un autre sans fermer', async () => {
      await ouvrir(courgettes());

      fixture.componentRef.setInput(
        'ingredient',
        ingredient({ id: 8, name: 'Lait', category: 'PRODUITS_LAITIERS', quantity: 1, unit: 'l', expiryDate: '2026-10-28' }),
      );
      fixture.detectChanges();

      expect(saisie('form-name').value).toBe('Lait');
      expect(liste('form-category').value).toBe('PRODUITS_LAITIERS');
      expect(saisie('form-quantity').value).toBe('1');
      expect(liste('form-unit').value).toBe('L');
      expect(saisie('form-expiry').value).toBe('2026-10-28');
    });

    it('laisse la date vide quand elle est estimée', async () => {
      await ouvrir(ingredient({ ...courgettes(), estimated: true }));

      expect(saisie('form-expiry').value).toBe('');
    });

    it('enregistre en PUT avec le corps complet puis émet closed', async () => {
      await ouvrir(courgettes());
      const fermeture = vi.fn();
      fixture.componentInstance.closed.subscribe(fermeture);
      remplir('form-name', 'Courgettes bio');

      envoyer();

      const req = http.expectOne({ method: 'PUT', url: '/api/ingredients/5' });
      expect(req.request.body).toEqual({
        name: 'Courgettes bio',
        category: 'LEGUMES',
        quantity: 2.5,
        unit: 'KG',
        expiryDate: '2026-10-25',
      });
      req.flush(courgettes());
      await fixture.whenStable();
      viderInventaire();
      await fixture.whenStable();
      expect(fermeture).toHaveBeenCalledTimes(1);
    });

    it('reste ouvert et affiche le message du serveur quand la modification est refusée', async () => {
      await ouvrir(courgettes());
      const fermeture = vi.fn();
      fixture.componentInstance.closed.subscribe(fermeture);

      envoyer();
      http
        .expectOne({ method: 'PUT', url: '/api/ingredients/5' })
        .flush({ message: 'Quantité invalide' }, { status: 400, statusText: 'Bad Request' });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(champ('form-error').textContent).toContain('Quantité invalide');
      expect(fermeture).not.toHaveBeenCalled();
    });
  });
});
