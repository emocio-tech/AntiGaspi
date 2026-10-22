import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfilePage } from './profile-page';

describe('ProfilePage', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ProfilePage>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProfilePage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProfilePage);
  });

  afterEach(() => http.verify());

  const html = () => fixture.nativeElement as HTMLElement;
  const all = (testId: string) => Array.from(html().querySelectorAll(`[data-testid=${testId}]`));
  const textes = (testId: string) => all(testId).map((e) => e.textContent?.trim());

  async function afficher() {
    fixture.detectChanges();
    http.expectOne('/api/profile').flush({ preferences: ['Végétarien le midi', 'Sans coriandre'] });
    http.expectOne('/api/utensils').flush([
      { id: 3, name: 'Casserole' },
      { id: 1, name: 'Poêle' },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function saisir(testId: string, valeur: string) {
    const input = html().querySelector(`[data-testid=${testId}]`) as HTMLInputElement;
    input.value = valeur;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  const cliquer = (element: Element | null) => {
    (element as HTMLElement).click();
    fixture.detectChanges();
  };

  it('affiche un état de chargement avant la réponse de l’API', () => {
    fixture.detectChanges();

    expect(html().querySelector('[data-testid=loading]')).not.toBeNull();
    http.expectOne('/api/profile').flush({ preferences: [] });
    http.expectOne('/api/utensils').flush([]);
  });

  it('affiche les préférences et les ustensiles', async () => {
    await afficher();

    expect(textes('pref-item')).toEqual(['Végétarien le midi', 'Sans coriandre']);
    expect(textes('utensil-item')).toEqual(['Casserole', 'Poêle']);
  });

  it('ajoute une préférence saisie, puis vide le champ', async () => {
    await afficher();

    saisir('pref-input', 'Sans gluten');
    cliquer(html().querySelector('[data-testid=pref-add]'));

    const req = http.expectOne({ method: 'PUT', url: '/api/profile' });
    expect(req.request.body).toEqual({ preferences: ['Végétarien le midi', 'Sans coriandre', 'Sans gluten'] });
    req.flush({ preferences: ['Végétarien le midi', 'Sans coriandre', 'Sans gluten'] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(textes('pref-item')).toContain('Sans gluten');
    expect((html().querySelector('[data-testid=pref-input]') as HTMLInputElement).value).toBe('');
  });

  it('ajoute aussi une préférence en validant le formulaire avec Entrée', async () => {
    await afficher();

    saisir('pref-input', 'Sans gluten');
    (html().querySelector('[data-testid=pref-form]') as HTMLFormElement).dispatchEvent(new Event('submit'));

    http.expectOne({ method: 'PUT', url: '/api/profile' }).flush({ preferences: ['Sans gluten'] });
  });

  it('retire une préférence', async () => {
    await afficher();

    cliquer(all('pref-remove')[0]);

    const req = http.expectOne({ method: 'PUT', url: '/api/profile' });
    expect(req.request.body).toEqual({ preferences: ['Sans coriandre'] });
    req.flush({ preferences: ['Sans coriandre'] });
  });

  it('affiche le message du serveur quand une modification est refusée', async () => {
    await afficher();

    saisir('pref-input', 'sans CORIANDRE');
    cliquer(html().querySelector('[data-testid=pref-add]'));
    http
      .expectOne({ method: 'PUT', url: '/api/profile' })
      .flush({ message: 'La préférence « sans CORIANDRE » est en double' }, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(html().querySelector('[data-testid=action-error]')?.textContent).toContain('est en double');
    expect(textes('pref-item')).toEqual(['Végétarien le midi', 'Sans coriandre']);
  });

  it('ajoute un ustensile saisi', async () => {
    await afficher();

    saisir('utensil-input', 'four');
    cliquer(html().querySelector('[data-testid=utensil-add]'));

    const req = http.expectOne({ method: 'POST', url: '/api/utensils' });
    expect(req.request.body).toEqual({ name: 'four' });
    req.flush({ id: 4, name: 'four' }, { status: 201, statusText: 'Created' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(textes('utensil-item')).toEqual(['Casserole', 'four', 'Poêle']);
  });

  it('retire un ustensile', async () => {
    await afficher();

    cliquer(all('utensil-remove')[1]);

    http.expectOne({ method: 'DELETE', url: '/api/utensils/1' }).flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(textes('utensil-item')).toEqual(['Casserole']);
  });

  it('affiche une erreur de chargement avec un bouton pour réessayer', async () => {
    fixture.detectChanges();
    http.expectOne('/api/profile').flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/utensils').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(html().querySelector('[data-testid=error]')).not.toBeNull();

    cliquer(html().querySelector('[data-testid=retry]'));
    http.expectOne('/api/profile').flush({ preferences: [] });
    http.expectOne('/api/utensils').flush([]);
  });
});
