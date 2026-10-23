import { MarkdownPipe } from './markdown';

describe('MarkdownPipe', () => {
  /** HTML produit par le pipe, inséré dans un élément pour l'interroger. */
  function rendu(markdown: string): HTMLElement {
    const element = document.createElement('div');
    element.innerHTML = new MarkdownPipe().transform(markdown);
    return element;
  }

  it('convertit titres, gras et italique', () => {
    const html = rendu('## Étapes\n\n**Courgettes** et *crème*');

    expect(html.querySelector('h2')?.textContent).toBe('Étapes');
    expect(html.querySelector('strong')?.textContent).toBe('Courgettes');
    expect(html.querySelector('em')?.textContent).toBe('crème');
  });

  it('convertit les listes à puces et numérotées', () => {
    const html = rendu('- Lait\n- Œufs\n\n1. Couper\n2. Cuire');

    expect(Array.from(html.querySelectorAll('ul > li')).map((li) => li.textContent)).toEqual(['Lait', 'Œufs']);
    expect(Array.from(html.querySelectorAll('ol > li')).map((li) => li.textContent)).toEqual(['Couper', 'Cuire']);
  });

  it('garde le texte, les accents et les caractères spéciaux sans créer d’élément', () => {
    const texte = 'Sel & poivre < 5 g, crème brûlée 🥕';
    const html = rendu(texte);

    expect(html.textContent?.trim()).toBe(texte);
    expect(Array.from(html.querySelectorAll('*')).map((element) => element.tagName)).toEqual(['P']);
  });

  it('renvoie une chaîne vide pour un texte absent', () => {
    const pipe = new MarkdownPipe();

    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });

  it('remplace une image par son texte alternatif, sans rien charger', () => {
    const html = rendu('Voici ![un gratin](https://exemple.org/gratin.png) maison');

    expect(html.querySelector('img')).toBeNull();
    expect(html.textContent).toContain('un gratin');
  });

  it('affiche en texte le HTML brut au lieu de l’interpréter', () => {
    const html = rendu('Passe l’<id> au tool <img src="https://exemple.org/x.png">');

    expect(html.querySelector('img')).toBeNull();
    expect(html.textContent).toContain('<id>');
    expect(html.textContent).toContain('<img src="https://exemple.org/x.png">');
  });

  it('ouvre les liens dans un nouvel onglet', () => {
    const lien = rendu('[Open Food Facts](https://fr.openfoodfacts.org)').querySelector('a');

    expect(lien?.getAttribute('href')).toBe('https://fr.openfoodfacts.org');
    expect(lien?.getAttribute('target')).toBe('_blank');
    expect(lien?.getAttribute('rel')).toBe('noopener noreferrer');
  });
});
