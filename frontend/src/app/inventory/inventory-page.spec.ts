import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ingredient } from './ingredient.fixtures';
import { Ingredient } from './ingredient.model';
import { InventoryPage } from './inventory-page';

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
});
