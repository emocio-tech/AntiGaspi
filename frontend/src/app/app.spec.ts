import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { ingredient } from './inventory/ingredient.fixtures';

describe('App', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('propose la navigation vers l’inventaire', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const lien = (fixture.nativeElement as HTMLElement).querySelector('a[href="/inventaire"]');
    expect(lien?.textContent).toContain('Inventaire');
    http.expectOne('/api/ingredients').flush([]);
  });

  it('propose la navigation vers les recettes', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const lien = (fixture.nativeElement as HTMLElement).querySelector('a[href="/recettes"]');
    expect(lien?.textContent).toContain('Recettes');
    http.expectOne('/api/ingredients').flush([]);
  });

  it('propose la navigation vers le profil', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const lien = (fixture.nativeElement as HTMLElement).querySelector('a[href="/profil"]');
    expect(lien?.textContent).toContain('Profil');
    http.expectOne('/api/ingredients').flush([]);
  });

  it('affiche sur l’inventaire le nombre d’aliments à consommer en priorité', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http.expectOne('/api/ingredients').flush([
      ingredient({ id: 1, urgency: 'EXPIRED_OR_TODAY' }),
      ingredient({ id: 2, urgency: 'SOON' }),
      ingredient({ id: 3, urgency: 'LATER' }),
    ]);
    await fixture.whenStable();
    fixture.detectChanges();

    const pastille = (fixture.nativeElement as HTMLElement).querySelector('[data-testid=urgent-count]');
    expect(pastille?.textContent?.trim()).toBe('2');
  });
});
