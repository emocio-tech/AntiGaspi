import { Pipe, PipeTransform } from '@angular/core';
import { Marked, RendererObject, Tokens } from 'marked';

/** Caractères à échapper pour qu'un texte reste du texte, dans le contenu comme dans un attribut. */
const HTML_ENTITIES: Record<string, string> = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
};

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (character) => HTML_ENTITIES[character]);
}

/** Rendus surchargés : HTML brut et images jamais interprétés, liens ouverts dans un nouvel onglet. */
const renderer: RendererObject = {
  html({ text }: Tokens.HTML | Tokens.Tag): string {
    return escapeHtml(text);
  },
  image({ text, tokens }: Tokens.Image): string {
    const alt = tokens ? this.parser.parseInline(tokens, this.parser.textRenderer) : text;
    return escapeHtml(alt);
  },
  link({ href, title, tokens }: Tokens.Link): string {
    const titleAttribute = title ? ` title="${escapeHtml(title)}"` : '';
    const content = this.parser.parseInline(tokens);
    return `<a href="${escapeHtml(href)}"${titleAttribute} target="_blank" rel="noopener noreferrer">${content}</a>`;
  },
};

/** Instance dédiée : la configuration globale de marked reste intacte. */
const parser = new Marked({ renderer });

/**
 * Convertit en HTML le Markdown d'une réponse de l'assistant (GFM, retour à la ligne simple conservé).
 *
 * Première barrière : le HTML brut écrit par le LLM est échappé (affiché en texte), les images sont
 * remplacées par leur texte alternatif (rien n'est chargé) et les liens s'ouvrent dans un nouvel onglet.
 * Seconde barrière : le sanitizer d'Angular, à condition d'insérer le résultat via `[innerHTML]`. Lui seul neutralise
 * les liens `javascript:` : la sortie du pipe n'est pas sûre seule, ne jamais la passer à `bypassSecurityTrustHtml`.
 */
@Pipe({ name: 'markdown' })
export class MarkdownPipe implements PipeTransform {
  transform(text: string | null | undefined): string {
    if (!text) return '';
    return parser.parse(text, { async: false, gfm: true, breaks: true });
  }
}
