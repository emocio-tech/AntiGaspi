import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChatPanel } from './chat-panel';

describe('ChatPanel', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ChatPanel>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ChatPanel],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ChatPanel);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const html = () => fixture.nativeElement as HTMLElement;
  const q = (testId: string) => html().querySelector(`[data-testid=${testId}]`) as HTMLElement | null;
  const all = (testId: string) => Array.from(html().querySelectorAll(`[data-testid=${testId}]`)) as HTMLElement[];
  const maj = () => fixture.detectChanges();

  async function historique(messages: { role: string; text: string }[] = []) {
    http.expectOne({ method: 'GET', url: '/api/chat' }).flush({ messages });
    await fixture.whenStable();
    maj();
  }

  function saisir(valeur: string) {
    const input = q('chat-input') as HTMLInputElement;
    input.value = valeur;
    input.dispatchEvent(new Event('input'));
    maj();
  }

  const envoyer = () => {
    (q('chat-form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    maj();
  };

  it('affiche l’historique de la conversation avec le rôle de chaque message', async () => {
    await historique([
      { role: 'user', text: 'Bonjour' },
      { role: 'assistant', text: 'Bonjour !' },
    ]);

    expect(all('chat-message').map((m) => m.getAttribute('data-role'))).toEqual(['user', 'assistant']);
    expect(all('chat-text').map((t) => t.textContent?.trim())).toEqual(['Bonjour', 'Bonjour !']);
    expect(q('chat-empty')).toBeNull();
  });

  it('invite à poser une question quand la conversation est vide', async () => {
    await historique([]);

    expect(q('chat-empty')).not.toBeNull();
  });

  it('envoie la saisie, affiche la question, vide le champ et montre l’attente', async () => {
    await historique();

    saisir('Qu’est-ce qui périme ?');
    envoyer();

    const req = http.expectOne({ method: 'POST', url: '/api/chat' });
    expect(req.request.body).toEqual({ message: 'Qu’est-ce qui périme ?' });
    expect(all('chat-text').map((t) => t.textContent?.trim())).toEqual(['Qu’est-ce qui périme ?']);
    expect((q('chat-input') as HTMLInputElement).value).toBe('');
    expect(q('thinking')?.textContent).toContain('Le chef réfléchit');
    req.flush({ reply: 'Les courgettes.', tools: [] });
  });

  it('affiche la réponse avec les tools appelés', async () => {
    await historique();
    saisir('Qu’est-ce qui périme ?');
    envoyer();

    http
      .expectOne('/api/chat')
      .flush({ reply: 'Les courgettes.', tools: [{ name: 'listerIngredients', arguments: '{}' }] });
    await fixture.whenStable();
    maj();

    expect(all('chat-text').at(-1)?.textContent?.trim()).toBe('Les courgettes.');
    expect(all('chat-tool').map((t) => t.textContent?.trim())).toEqual(['listerIngredients']);
    expect(q('thinking')).toBeNull();
  });

  it('bloque l’envoi quand le champ est vide ou qu’une réponse est attendue', async () => {
    await historique();
    expect((q('chat-send') as HTMLButtonElement).disabled).toBe(true);

    saisir('Bonjour');
    expect((q('chat-send') as HTMLButtonElement).disabled).toBe(false);

    envoyer();
    saisir('Encore');
    expect((q('chat-send') as HTMLButtonElement).disabled).toBe(true);
    http.expectOne('/api/chat').flush({ reply: 'Salut', tools: [] });
  });

  it('envoie une suggestion au clic', async () => {
    await historique();

    all('chat-suggestion')[0].click();
    maj();

    const req = http.expectOne({ method: 'POST', url: '/api/chat' });
    expect(req.request.body.message).toBe(all('chat-suggestion')[0].textContent?.trim());
    req.flush({ reply: 'Ok', tools: [] });
  });

  it('affiche l’erreur avec un bouton pour renvoyer le message', async () => {
    await historique();
    saisir('Bonjour');
    envoyer();
    http.expectOne('/api/chat').flush({ message: 'LLM indisponible' }, { status: 502, statusText: 'Bad Gateway' });
    await fixture.whenStable();
    maj();

    expect(q('chat-error')?.textContent).toContain('LLM indisponible');

    q('chat-retry')?.click();
    maj();
    http.expectOne({ method: 'POST', url: '/api/chat' }).flush({ reply: 'Bonjour !', tools: [] });
  });

  it('efface la conversation avec le bouton « Nouvelle conversation »', async () => {
    await historique([{ role: 'user', text: 'Bonjour' }]);

    q('chat-reset')?.click();
    maj();
    http.expectOne({ method: 'DELETE', url: '/api/chat' }).flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();
    maj();

    expect(all('chat-message').length).toBe(0);
    expect(q('chat-empty')).not.toBeNull();
  });
});
