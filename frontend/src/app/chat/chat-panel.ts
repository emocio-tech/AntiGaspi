import { afterRenderEffect, Component, ElementRef, inject, Injector, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LucideChefHat, LucideRotateCcw, LucideSendHorizontal, LucideWrench } from '@lucide/angular';
import { GeneratedRecipe } from '../recipes/recipe.model';
import { RecipeStore } from '../recipes/recipe-store';
import { ChatStore } from './chat-store';
import { MarkdownPipe } from './markdown';

/** Questions proposées en un clic. */
const SUGGESTIONS = [
  'Que dois-je consommer en priorité ?',
  'Propose-moi une recette avec ce qui presse',
  'Que sais-tu de mes préférences ?',
  'Quels ustensiles ai-je ?',
];

/**
 * Panneau de chat avec l'assistant : bulles, tools appelés, suggestions et saisie. Les réponses de l'assistant sont
 * rendues en Markdown ({@link MarkdownPipe}), les messages de l'utilisateur restent en texte brut.
 */
@Component({
  selector: 'app-chat-panel',
  imports: [LucideChefHat, LucideRotateCcw, LucideSendHorizontal, LucideWrench, MarkdownPipe],
  templateUrl: './chat-panel.html',
  styleUrl: './chat-panel.css',
})
export class ChatPanel {
  protected readonly store = inject(ChatStore);
  protected readonly suggestions = SUGGESTIONS;
  private readonly injector = inject(Injector);
  private readonly router = inject(Router);
  protected readonly draft = signal('');

  private readonly log = viewChild<ElementRef<HTMLElement>>('log');

  constructor() {
    this.store.load();
    // Garde le dernier échange visible : nouveau message, attente ou erreur
    afterRenderEffect(() => {
      this.store.messages();
      this.store.pending();
      this.store.error();
      const element = this.log()?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }

  protected submit(event: Event): void {
    event.preventDefault();
    const text = this.draft().trim();
    if (!text || this.store.pending()) return;
    this.store.send(text);
    this.draft.set('');
  }

  /** Ouvre la recette dans l'écran de détail (et son « J'ai cuisiné »), comme une carte de l'écran Recettes. */
  protected openRecipe(result: GeneratedRecipe): void {
    // Récupéré à la demande : l'injecter plus tôt lancerait la lecture de l'inventaire dès l'ouverture du chat
    const index = this.injector.get(RecipeStore).addFromChat(result);
    this.router.navigateByUrl(`/recettes/${index}`);
  }

  protected valueOf(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }
}
