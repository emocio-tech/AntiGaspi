import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { generated } from '../recipes/recipe.fixtures';
import { RecipeStore } from '../recipes/recipe-store';
import { InventoryStore } from '../inventory/inventory-store';
import { signal } from '@angular/core';
import { ChatPanel } from './chat-panel';

describe('ChatPanel', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ChatPanel>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ChatPanel],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: InventoryStore, useValue: { items: signal([]) } },
      ],
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
    req.flush({ reply: 'Les courgettes.', tools: [], recipes: [] });
  });

  it('affiche la réponse avec les tools appelés', async () => {
    await historique();
    saisir('Qu’est-ce qui périme ?');
    envoyer();

    http
      .expectOne('/api/chat')
      .flush({ reply: 'Les courgettes.', tools: [{ name: 'listerIngredients', arguments: '{}' }], recipes: [] });
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
    http.expectOne('/api/chat').flush({ reply: 'Salut', tools: [], recipes: [] });
  });

  it('envoie une suggestion au clic', async () => {
    await historique();

    all('chat-suggestion')[0].click();
    maj();

    const req = http.expectOne({ method: 'POST', url: '/api/chat' });
    expect(req.request.body.message).toBe(all('chat-suggestion')[0].textContent?.trim());
    req.flush({ reply: 'Ok', tools: [], recipes: [] });
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
    http.expectOne({ method: 'POST', url: '/api/chat' }).flush({ reply: 'Bonjour !', tools: [], recipes: [] });
  });

  it('rend en HTML le Markdown des réponses de l’assistant', async () => {
    await historique([
      { role: 'assistant', text: '### Idée\n\n**Gratin** de courgettes\n\n- 2 courgettes\n- 50 g d’emmental' },
    ]);

    const texte = all('chat-text')[0];
    expect(texte.querySelector('h3')?.textContent).toBe('Idée');
    expect(texte.querySelector('strong')?.textContent).toBe('Gratin');
    expect(Array.from(texte.querySelectorAll('li')).map((li) => li.textContent)).toEqual([
      '2 courgettes',
      '50 g d’emmental',
    ]);
    expect(texte.textContent).not.toContain('**');
    expect(texte.textContent).not.toContain('###');
  });

  it('rend aussi le Markdown d’une réponse reçue pendant la session', async () => {
    await historique();
    saisir('Qu’est-ce qui périme ?');
    envoyer();

    http.expectOne('/api/chat').flush({ reply: '**Les courgettes** périment demain.', tools: [], recipes: [] });
    await fixture.whenStable();
    maj();

    expect(all('chat-text').at(-1)?.querySelector('strong')?.textContent).toBe('Les courgettes');
  });

  it('n’interprète que les réponses de l’assistant, pas les messages de l’utilisateur', async () => {
    await historique([
      { role: 'user', text: '**gras** <b>html</b>' },
      { role: 'assistant', text: '**gras**' },
    ]);

    const [utilisateur, assistant] = all('chat-text');
    expect(utilisateur.textContent).toBe('**gras** <b>html</b>');
    expect(utilisateur.querySelector('strong, b')).toBeNull();
    expect(assistant.querySelector('strong')?.textContent).toBe('gras');
  });

  it('neutralise le HTML et les scripts injectés dans une réponse', async () => {
    await historique([
      {
        role: 'assistant',
        text:
          '<script>alert(1)</script><img src="x" onerror="alert(2)"><iframe src="https://exemple.org"></iframe>\n\n' +
          '[clic](javascript:alert(3)) **Fin**',
      },
    ]);

    const texte = all('chat-text')[0];
    expect(texte.querySelector('strong')?.textContent).toBe('Fin');
    expect(texte.querySelector('script, iframe')).toBeNull();
    const attributs = Array.from(texte.querySelectorAll('*')).flatMap((element) =>
      element.getAttributeNames().map((nom) => nom.toLowerCase()),
    );
    expect(attributs.filter((nom) => nom.startsWith('on'))).toEqual([]);
    expect(texte.querySelector('a[href^="javascript:"]')).toBeNull();
  });

  it('garde un retour à la ligne simple d’une réponse', async () => {
    await historique([{ role: 'assistant', text: 'Bonjour !\nVoici ce qui presse.' }]);

    expect(all('chat-text')[0].querySelector('br')).not.toBeNull();
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

  it('affiche une carte par recette générée et l’ouvre dans le détail au clic', async () => {
    await historique();
    saisir('Une recette ?');
    envoyer();
    const recette = generated({ title: 'Gratin de courgettes', servings: 4, preparationMinutes: 35 });
    http
      .expectOne({ method: 'POST', url: '/api/chat' })
      .flush({ reply: 'Voici.', tools: [], recipes: [recette] });
    maj();

    const cartes = all('chat-recipe');
    expect(cartes).toHaveLength(1);
    expect(cartes[0].textContent).toContain('Gratin de courgettes');
    expect(cartes[0].textContent).toContain('4');
    expect(cartes[0].textContent).toContain('35');

    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const index = TestBed.inject(RecipeStore);
    q('chat-recipe-open')!.click();

    expect(navigate).toHaveBeenCalledWith('/recettes/100');
    expect(index.result(100)).toEqual(recette);
  });

  it('n’affiche aucune carte pour une réponse sans recette', async () => {
    await historique([{ role: 'assistant', text: 'Bonjour !' }]);

    expect(q('chat-recipe')).toBeNull();
  });
});
