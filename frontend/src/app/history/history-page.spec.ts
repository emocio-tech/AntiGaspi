import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CookedRecipe } from './history.model';
import { cooked } from './history.fixtures';
import { HistoryPage } from './history-page';

describe('HistoryPage', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<HistoryPage>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HistoryPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(HistoryPage);
  });

  afterEach(() => http.verify());

  const html = () => fixture.nativeElement as HTMLElement;
  const un = (testId: string, racine: ParentNode = html()) => racine.querySelector(`[data-testid=${testId}]`);
  const tous = (testId: string) => Array.from(html().querySelectorAll(`[data-testid=${testId}]`));
  const textes = (testId: string) => tous(testId).map((e) => e.textContent?.trim());

  const liste = () => [
    cooked({ id: 2, title: 'Soupe de carottes', cookedAt: '2026-10-19T12:00:00Z', rating: 'UP', comment: 'Délicieux' }),
    cooked({ id: 1, title: 'Gratin de courgettes', cookedAt: '2026-10-12T22:30:00Z', rating: null, comment: null }),
  ];

  async function afficher(recettes: CookedRecipe[] = liste()) {
    fixture.detectChanges();
    http.expectOne('/api/cooked-recipes').flush(recettes);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const cliquer = (element: Element | null) => {
    (element as HTMLElement).click();
    fixture.detectChanges();
  };

  /** Ouvre l'édition de la première carte. */
  async function editer(recettes: CookedRecipe[] = liste()) {
    await afficher(recettes);
    cliquer(un('history-edit', tous('history-item')[0]));
  }

  function saisir(valeur: string) {
    const zone = un('edit-comment') as HTMLTextAreaElement;
    zone.value = valeur;
    zone.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('affiche une carte par recette cuisinée, dans l’ordre reçu', async () => {
    await afficher();

    expect(tous('history-item').length).toBe(2);
    expect(textes('history-title')).toEqual(['Soupe de carottes', 'Gratin de courgettes']);
  });

  it('affiche la date de cuisson au format français long en UTC', async () => {
    await afficher();

    expect(textes('history-date')).toEqual(['19 octobre 2026', '12 octobre 2026']);
  });

  it('affiche l’avis et le commentaire, et rien quand ils sont absents', async () => {
    await afficher();
    const [avec, sans] = tous('history-item');

    expect(un('history-rating', avec)?.textContent).toContain('👍');
    expect(un('history-comment', avec)?.textContent?.trim()).toBe('Délicieux');
    expect(un('history-rating', sans)).toBeNull();
    expect(un('history-comment', sans)).toBeNull();
  });

  it('affiche 👎 pour un avis négatif', async () => {
    await afficher([cooked({ rating: 'DOWN' })]);

    expect(un('history-rating')?.textContent).toContain('👎');
  });

  it('déplie puis replie la recette', async () => {
    await afficher();
    const carte = tous('history-item')[0];
    expect(un('history-recipe', carte)).toBeNull();

    cliquer(un('history-toggle', carte));
    const recette = un('history-recipe', carte)?.textContent ?? '';
    expect(recette).toContain('Courgettes');
    expect(recette).toContain('Couper les courgettes.');
    expect(recette).toContain('Enfourner 25 minutes.');

    cliquer(un('history-toggle', carte));
    expect(un('history-recipe', carte)).toBeNull();
  });

  it('passe en édition avec l’avis et le commentaire préremplis', async () => {
    await editer();

    expect(un('edit-up')?.getAttribute('aria-pressed')).toBe('true');
    expect(un('edit-down')?.getAttribute('aria-pressed')).toBe('false');
    const zone = un('edit-comment') as HTMLTextAreaElement;
    expect(zone.value).toBe('Délicieux');
    expect(zone.maxLength).toBe(1000);
  });

  it('bascule l’avis : un second clic sur le même pouce le retire', async () => {
    await editer();

    cliquer(un('edit-down'));
    expect(un('edit-down')?.getAttribute('aria-pressed')).toBe('true');
    expect(un('edit-up')?.getAttribute('aria-pressed')).toBe('false');

    cliquer(un('edit-down'));
    expect(un('edit-down')?.getAttribute('aria-pressed')).toBe('false');
  });

  it('enregistre l’avis modifié puis quitte l’édition', async () => {
    await editer();
    cliquer(un('edit-down'));
    saisir('Trop salé');

    cliquer(un('edit-save'));

    const put = http.expectOne({ method: 'PUT', url: '/api/cooked-recipes/2/feedback' });
    expect(put.request.body).toEqual({ rating: 'DOWN', comment: 'Trop salé' });
    put.flush(cooked({ id: 2, rating: 'DOWN', comment: 'Trop salé' }));
    await fixture.whenStable();
    fixture.detectChanges();
    http.expectOne('/api/cooked-recipes').flush([cooked({ id: 2, title: 'Soupe de carottes', rating: 'DOWN', comment: 'Trop salé' })]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(un('edit-save')).toBeNull();
    expect(un('history-rating')?.textContent).toContain('👎');
    expect(un('history-comment')?.textContent?.trim()).toBe('Trop salé');
  });

  it('envoie un commentaire vide comme null', async () => {
    await editer();
    saisir('   ');

    cliquer(un('edit-save'));

    const put = http.expectOne({ method: 'PUT', url: '/api/cooked-recipes/2/feedback' });
    expect(put.request.body).toEqual({ rating: 'UP', comment: null });
    put.flush(cooked({ id: 2, rating: 'UP' }));
    await fixture.whenStable();
    http.expectOne('/api/cooked-recipes').flush(liste());
  });

  it('annule l’édition sans envoyer de requête', async () => {
    await editer();
    saisir('Autre chose');

    cliquer(un('edit-cancel'));

    expect(un('edit-save')).toBeNull();
    expect(un('history-comment')?.textContent?.trim()).toBe('Délicieux');
    http.expectNone({ method: 'PUT' });
  });

  it('affiche le message du serveur et garde la saisie quand l’avis est refusé', async () => {
    await editer();
    saisir('Nouveau commentaire');

    cliquer(un('edit-save'));
    http
      .expectOne({ method: 'PUT', url: '/api/cooked-recipes/2/feedback' })
      .flush({ message: 'Le commentaire est trop long' }, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(un('edit-error')?.textContent).toContain('Le commentaire est trop long');
    expect((un('edit-comment') as HTMLTextAreaElement).value).toBe('Nouveau commentaire');
  });

  it('invite à cuisiner quand aucune recette n’a été cuisinée', async () => {
    await afficher([]);

    const vide = un('empty');
    expect(vide?.textContent).toContain('Vous n’avez encore cuisiné aucune recette.');
    expect(vide?.querySelector('a')?.getAttribute('href')).toBe('/recettes');
  });

  it('affiche une erreur avec un bouton pour réessayer quand le chargement échoue', async () => {
    fixture.detectChanges();
    http.expectOne('/api/cooked-recipes').flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(un('error')).not.toBeNull();

    cliquer(un('retry'));

    http.expectOne('/api/cooked-recipes').flush([]);
  });

  it('recharge la liste quand on revient sur l’écran', async () => {
    await afficher();
    fixture.destroy();

    fixture = TestBed.createComponent(HistoryPage);
    fixture.detectChanges();
    http.expectOne('/api/cooked-recipes').flush([cooked({ id: 3, title: 'Omelette aux herbes' })]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(textes('history-title')).toEqual(['Omelette aux herbes']);
  });
});
