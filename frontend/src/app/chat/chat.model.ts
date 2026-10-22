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
}

/** Réponse de `POST /api/chat`. */
export interface ChatReply {
  reply: string;
  tools: ToolCall[];
}

/** Réponse de `GET /api/chat`. */
export interface ChatHistory {
  messages: { role: 'user' | 'assistant'; text: string }[];
}
