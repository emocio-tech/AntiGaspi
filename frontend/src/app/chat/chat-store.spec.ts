import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ChatStore } from './chat-store';

describe('ChatStore', () => {
  let http: HttpTestingController;
  let store: ChatStore;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(ChatStore);
  });

  afterEach(() => http.verify());

  const reponse = (reply: string, tools: { name: string; arguments: string }[] = []) => ({ reply, tools });

  it('charge l’historique de la conversation, sans tools', () => {
    store.load();

    http.expectOne({ method: 'GET', url: '/api/chat' }).flush({
      messages: [
        { role: 'user', text: 'Bonjour' },
        { role: 'assistant', text: 'Bonjour !' },
      ],
    });

    expect(store.messages()).toEqual([
      { role: 'user', text: 'Bonjour', tools: [] },
      { role: 'assistant', text: 'Bonjour !', tools: [] },
    ]);
  });

  it('ne recharge l’historique qu’une fois', () => {
    store.load();
    http.expectOne('/api/chat').flush({ messages: [] });

    store.load();

    http.expectNone('/api/chat');
  });

  it('affiche tout de suite la question, puis la réponse avec les tools appelés', () => {
    store.send('  Qu’est-ce qui périme ?  ');

    expect(store.messages()).toEqual([{ role: 'user', text: 'Qu’est-ce qui périme ?', tools: [] }]);
    expect(store.pending()).toBe(true);
    const req = http.expectOne({ method: 'POST', url: '/api/chat' });
    expect(req.request.body).toEqual({ message: 'Qu’est-ce qui périme ?' });

    req.flush(reponse('Les courgettes.', [{ name: 'listerIngredients', arguments: '{"joursMax":3}' }]));

    expect(store.pending()).toBe(false);
    expect(store.messages()[1]).toEqual({
      role: 'assistant',
      text: 'Les courgettes.',
      tools: [{ name: 'listerIngredients', arguments: '{"joursMax":3}' }],
    });
  });

  it('ignore un message vide', () => {
    store.send('   ');

    http.expectNone('/api/chat');
    expect(store.messages()).toEqual([]);
  });

  it('ignore un nouveau message tant que la réponse n’est pas arrivée', () => {
    store.send('Premier');
    store.send('Second');

    expect(store.messages().length).toBe(1);
    http.expectOne('/api/chat').flush(reponse('Ok'));
  });

  it('garde la question et affiche l’erreur du serveur quand la réponse échoue', () => {
    store.send('Bonjour');

    http
      .expectOne('/api/chat')
      .flush({ message: 'Échec de la réponse de l’assistant' }, { status: 502, statusText: 'Bad Gateway' });

    expect(store.error()).toBe('Échec de la réponse de l’assistant');
    expect(store.pending()).toBe(false);
    expect(store.messages()).toEqual([{ role: 'user', text: 'Bonjour', tools: [] }]);
  });

  it('renvoie le même message sans le réafficher, et efface l’erreur', () => {
    store.send('Bonjour');
    http.expectOne('/api/chat').flush({ message: 'boom' }, { status: 502, statusText: 'Bad Gateway' });

    store.retry();

    expect(store.error()).toBeNull();
    const req = http.expectOne({ method: 'POST', url: '/api/chat' });
    expect(req.request.body).toEqual({ message: 'Bonjour' });
    req.flush(reponse('Bonjour !'));
    expect(store.messages().map((m) => m.text)).toEqual(['Bonjour', 'Bonjour !']);
  });

  it('efface l’erreur précédente quand on envoie un autre message', () => {
    store.send('Bonjour');
    http.expectOne('/api/chat').flush({ message: 'boom' }, { status: 502, statusText: 'Bad Gateway' });

    store.send('Salut');

    expect(store.error()).toBeNull();
    http.expectOne('/api/chat').flush(reponse('Salut !'));
  });

  it('efface la conversation côté serveur, puis ici', () => {
    store.send('Bonjour');
    http.expectOne('/api/chat').flush(reponse('Bonjour !'));

    store.reset();

    http.expectOne({ method: 'DELETE', url: '/api/chat' }).flush(null, { status: 204, statusText: 'No Content' });
    expect(store.messages()).toEqual([]);
    expect(store.error()).toBeNull();
  });

  it('garde la conversation et affiche l’erreur quand l’effacement échoue', () => {
    store.send('Bonjour');
    http.expectOne('/api/chat').flush(reponse('Bonjour !'));

    store.reset();
    http.expectOne({ method: 'DELETE', url: '/api/chat' }).flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });

    expect(store.messages().length).toBe(2);
    expect(store.error()).toBe('boom');
  });
});
