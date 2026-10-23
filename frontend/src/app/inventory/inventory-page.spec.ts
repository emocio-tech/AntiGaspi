import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { ingredient } from './ingredient.fixtures';
import { Ingredient } from './ingredient.model';
import { InventoryPage } from './inventory-page';
import { REMOVAL_DELAY_MS } from './inventory-store';

describe('InventoryPage', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<InventoryPage>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [InventoryPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(InventoryPage);
  });

  afterEach(() => http.verify());

  const html = () => fixture.nativeElement as HTMLElement;
  const texte = () => html().textContent ?? '';

  async function afficher(aliments: Ingredient[]) {
    fixture.detectChanges();
    http.expectOne('/api/ingredients').flush(aliments);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const inventaire = () => [
    ingredient({ id: 1, name: 'Lait', daysLeft: -1, urgency: 'EXPIRED_OR_TODAY' }),
    ingredient({ id: 2, name: 'Courgettes', category: 'LEGUMES', categoryLabel: 'Légumes', quantity: 2.5, unit: 'kg', daysLeft: 2, urgency: 'SOON' }),
    ingredient({ id: 3, name: 'Yaourts', quantity: 4, unit: 'pièce', daysLeft: 10, urgency: 'LATER' }),
  ];

  it('affiche un état de chargement avant la réponse de l’API', () => {
    fixture.detectChanges();

    expect(html().querySelector('[data-testid=loading]')).not.toBeNull();
    http.expectOne('/api/ingredients').flush([]);
  });

  it('affiche les groupes d’urgence non vides avec leur nombre d’aliments', async () => {
    await afficher(inventaire());

    expect(texte()).toContain("À consommer aujourd'hui");
    expect(texte()).toContain('Dans les 3 jours');
    expect(texte()).not.toContain('Cette semaine');
    expect(html().querySelector('[data-testid=group-SOON]')?.textContent).toContain('Courgettes');
    expect(html().querySelector('[data-testid=group-SOON]')?.textContent).toContain('2,5 kg');
  });

  it('met en avant les aliments à consommer d’abord, avec un accès aux recettes', async () => {
    await afficher(inventaire());

    const hero = html().querySelector('[data-testid=hero]');
    expect(hero?.textContent).toContain('Lait');
    expect(hero?.textContent).toContain('Courgettes');
    expect(hero?.textContent).not.toContain('Yaourts');
    expect(hero?.textContent).toContain('Trouver une recette');
  });

  it('mène aux recettes depuis la mise en avant', async () => {
    await afficher(inventaire());

    const lien = html().querySelector('[data-testid=hero] a');
    expect(lien?.getAttribute('href')).toBe('/recettes');
  });

  it('n’affiche pas la mise en avant quand rien ne presse', async () => {
    await afficher([ingredient({ urgency: 'LATER', daysLeft: 20 })]);

    expect(html().querySelector('[data-testid=hero]')).toBeNull();
  });

  it('replie « Plus tard » par défaut et le déplie à la demande', async () => {
    await afficher(inventaire());
    expect(texte()).not.toContain('Yaourts');

    (html().querySelector('[data-testid=toggle-LATER]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(html().querySelector('[data-testid=group-LATER]')?.textContent).toContain('Yaourts');
  });

  it('indique les aliments à date estimée', async () => {
    await afficher([ingredient({ name: 'Pommes', daysLeft: 3, estimated: true, urgency: 'SOON' })]);

    expect(texte()).toContain('≈ 3 jours');
  });

  it('affiche un message quand l’inventaire est vide', async () => {
    await afficher([]);

    expect(html().querySelector('[data-testid=empty]')).not.toBeNull();
  });

  it('affiche une erreur avec un bouton pour réessayer quand l’API échoue', async () => {
    fixture.detectChanges();
    http.expectOne('/api/ingredients').flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(html().querySelector('[data-testid=error]')).not.toBeNull();

    (html().querySelector('[data-testid=retry]') as HTMLButtonElement).click();
    fixture.detectChanges();
    http.expectOne('/api/ingredients').flush([]);
  });

  describe('gestion de l’inventaire', () => {
    const testid = (id: string) => html().querySelector(`[data-testid=${id}]`) as HTMLElement | null;
    const tous = (id: string) => Array.from(html().querySelectorAll(`[data-testid=${id}]`)) as HTMLElement[];
    const cliquer = (id: string) => {
      (testid(id) as HTMLElement).click();
      fixture.detectChanges();
    };
    const categories = [{ code: 'PRODUITS_LAITIERS', label: 'Produits laitiers' }];

    async function ouvrirFormulaire() {
      fixture.detectChanges();
      http.expectOne('/api/ingredients/categories').flush(categories);
      await fixture.whenStable();
      fixture.detectChanges();
    }

    /** Deux aliments visibles (le groupe « Plus tard » est replié). */
    const deuxAliments = () => [
      ingredient({ id: 1, name: 'Lait', urgency: 'EXPIRED_OR_TODAY' }),
      ingredient({ id: 2, name: 'Courgettes', category: 'LEGUMES', unit: 'kg', urgency: 'SOON', daysLeft: 2 }),
    ];

    it('propose d’ajouter un aliment même quand l’inventaire est vide', async () => {
      await afficher([]);

      expect(testid('empty')).not.toBeNull();
      expect(testid('add-open')).not.toBeNull();
    });

    it('ouvre le formulaire en mode ajout', async () => {
      await afficher(deuxAliments());
      expect(html().querySelector('app-ingredient-form')).toBeNull();

      cliquer('add-open');
      await ouvrirFormulaire();

      expect(html().querySelector('app-ingredient-form')).not.toBeNull();
      expect((testid('form-name') as HTMLInputElement).value).toBe('');
    });

    it('ouvre le formulaire en édition au clic sur un aliment', async () => {
      await afficher(deuxAliments());

      cliquer('ingredient-2');
      await ouvrirFormulaire();

      expect((testid('form-name') as HTMLInputElement).value).toBe('Courgettes');
    });

    it('affiche une case par aliment en mode sélection', async () => {
      await afficher(deuxAliments());
      expect(tous('select-ingredient').length).toBe(0);

      cliquer('select-mode');

      expect(tous('select-ingredient').length).toBe(2);
    });

    it('compte les aliments cochés dans le bouton Retirer, désactivé à zéro', async () => {
      await afficher(deuxAliments());
      cliquer('select-mode');
      const retirer = () => testid('remove-selected') as HTMLButtonElement;
      expect(retirer().textContent).toContain('Retirer (0)');
      expect(retirer().disabled).toBe(true);

      tous('select-ingredient')[0].click();
      tous('select-ingredient')[1].click();
      fixture.detectChanges();

      expect(retirer().textContent).toContain('Retirer (2)');
      expect(retirer().disabled).toBe(false);
    });

    it('masque les aliments retirés et propose d’annuler', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
      try {
        await afficher(deuxAliments());
        cliquer('select-mode');
        tous('select-ingredient').forEach((c) => c.click());
        fixture.detectChanges();

        cliquer('remove-selected');

        expect(texte()).not.toContain('Courgettes');
        expect(testid('undo-banner')?.textContent).toContain('2 aliments retirés');
        expect(testid('undo')?.textContent).toContain('Annuler');
        vi.advanceTimersByTime(REMOVAL_DELAY_MS);
        http.expectOne({ method: 'DELETE', url: '/api/ingredients?ids=1,2' }).flush(null, { status: 204, statusText: 'No Content' });
        TestBed.tick();
        http.expectOne('/api/ingredients').flush([]);
      } finally {
        vi.useRealTimers();
      }
    });

    it('réaffiche les aliments à l’annulation, sans jamais envoyer de suppression', async () => {
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
      try {
        await afficher(deuxAliments());
        cliquer('select-mode');
        tous('select-ingredient').forEach((c) => c.click());
        fixture.detectChanges();
        cliquer('remove-selected');

        cliquer('undo');
        vi.advanceTimersByTime(10_000);

        expect(texte()).toContain('Courgettes');
        expect(texte()).toContain('Lait');
        expect(testid('undo-banner')).toBeNull();
        http.expectNone((req) => req.method === 'DELETE');
      } finally {
        vi.useRealTimers();
      }
    });
  });

  it('recharge la liste quand on revient sur l’écran', async () => {
    await afficher(inventaire());
    fixture.destroy();

    fixture = TestBed.createComponent(InventoryPage);
    fixture.detectChanges();
    http.expectOne('/api/ingredients').flush([ingredient({ id: 9, name: 'Beurre', daysLeft: 1, urgency: 'SOON' })]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(texte()).toContain('Beurre');
    expect(texte()).not.toContain('Courgettes');
  });
});
