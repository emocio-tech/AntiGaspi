import { GeneratedRecipe } from '../recipes/recipe.model';

/** Tool appelé par le LLM pour répondre à un message. */
export interface ToolCall {
  name: string;
  arguments: string;
}

/** Message de la conversation ; `tools` n'est renseigné que pour les réponses reçues pendant la session. */
export interface ChatMessage {
  role: 'user' | 'assistant';
  text: string;
  tools: ToolCall[];
  /** Recettes générées pour cette réponse ; vide pour l'historique rechargé. */
  recipes: GeneratedRecipe[];
}

/** Réponse de `POST /api/chat`. */
export interface ChatReply {
  reply: string;
  tools: ToolCall[];
  recipes: GeneratedRecipe[];
}

/** Réponse de `GET /api/chat`. */
export interface ChatHistory {
  messages: { role: 'user' | 'assistant'; text: string }[];
}
