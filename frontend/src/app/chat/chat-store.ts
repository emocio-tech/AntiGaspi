import { HttpClient } from '@angular/common/http';
import { inject, Injectable, Injector, signal } from '@angular/core';
import { messageOf } from '../api-error';
import { InventoryStore } from '../inventory/inventory-store';
import { ChatHistory, ChatMessage, ChatReply } from './chat.model';

/** Tools qui modifient l'inventaire : l'écran Inventaire doit alors être rechargé. */
const INVENTORY_TOOLS = ['ajouterAliment', 'modifierQuantite', 'retirerAliments'];

/**
 * Conversation avec l'assistant. Le serveur n'en garde qu'une, en mémoire : le store la recharge au premier affichage
 * (sans les tools des anciennes réponses) puis la tient à jour.
 */
@Injectable({ providedIn: 'root' })
export class ChatStore {
  private readonly http = inject(HttpClient);

  private readonly injector = inject(Injector);
  private loaded = false;
  /** Dernier message envoyé, conservé tant qu'il n'a pas reçu de réponse pour pouvoir le renvoyer. */
  private unanswered: string | null = null;

  readonly messages = signal<ChatMessage[]>([]);
  /** `true` entre l'envoi d'un message et sa réponse. */
  readonly pending = signal(false);
  /** Message d'erreur du dernier échange raté, `null` sinon. */
  readonly error = signal<string | null>(null);

  /** Charge l'historique une seule fois par session : relancer écraserait les tools des réponses déjà reçues. */
  load(): void {
    if (this.loaded) return;
    this.loaded = true;
    this.http.get<ChatHistory>('/api/chat').subscribe({
      // Un message envoyé avant l'arrivée de l'historique reste prioritaire
      next: (history) => {
        if (this.messages().length === 0) {
          this.messages.set(history.messages.map((message) => ({ ...message, tools: [] })));
        }
      },
      error: () => (this.loaded = false),
    });
  }

  /** Envoie un message ; ignoré s'il est vide ou si une réponse est attendue. */
  send(text: string): void {
    const message = text.trim();
    if (!message || this.pending()) return;
    this.messages.update((messages) => [...messages, { role: 'user', text: message, tools: [] }]);
    this.post(message);
  }

  /** Renvoie le dernier message resté sans réponse, sans le réafficher. */
  retry(): void {
    if (this.pending() || this.unanswered === null) return;
    this.post(this.unanswered);
  }

  /** Efface la conversation côté serveur, puis ici. */
  reset(): void {
    this.http.delete<void>('/api/chat').subscribe({
      next: () => {
        this.messages.set([]);
        this.error.set(null);
        this.unanswered = null;
      },
      error: (error: unknown) => this.error.set(messageOf(error)),
    });
  }

  private post(message: string): void {
    this.unanswered = message;
    this.pending.set(true);
    this.error.set(null);
    this.http.post<ChatReply>('/api/chat', { message }).subscribe({
      next: (reply) => {
        this.unanswered = null;
        this.pending.set(false);
        this.messages.update((messages) => [
          ...messages,
          { role: 'assistant', text: reply.reply, tools: reply.tools },
        ]);
        if (reply.tools.some((tool) => INVENTORY_TOOLS.includes(tool.name))) {
          // Récupéré à la demande : l'injecter plus tôt lancerait son GET dès l'ouverture du chat
          this.injector.get(InventoryStore).reload();
        }
      },
      error: (error: unknown) => {
        this.pending.set(false);
        this.error.set(messageOf(error));
      },
    });
  }
}
