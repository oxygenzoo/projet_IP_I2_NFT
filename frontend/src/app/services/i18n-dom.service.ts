import { Injectable, effect, inject } from '@angular/core';

import { I18nService } from './i18n.service';

const TRANSLATABLE_ATTRIBUTES = ['aria-label', 'title', 'placeholder', 'alt'];
const IGNORED_TAGS = new Set(['SCRIPT', 'STYLE', 'SVG', 'PATH', 'VIDEO', 'CANVAS']);

@Injectable({ providedIn: 'root' })
export class I18nDomService {
  private readonly i18n = inject(I18nService);
  private readonly originalText = new WeakMap<Text, string>();
  private readonly lastText = new WeakMap<Text, string>();
  private readonly originalAttributes = new WeakMap<Element, Map<string, string>>();
  private readonly lastAttributes = new WeakMap<Element, Map<string, string>>();
  private observer?: MutationObserver;

  constructor() {
    effect(() => {
      this.i18n.language();
      this.scheduleTranslate();
    });
  }

  start(): void {
    if (typeof document === 'undefined' || this.observer) {
      return;
    }

    this.observer = new MutationObserver(() => this.scheduleTranslate());
    this.observer.observe(document.body, {
      childList: true,
      subtree: true,
      characterData: true,
      attributes: true,
      attributeFilter: TRANSLATABLE_ATTRIBUTES,
    });
    this.scheduleTranslate();
  }

  private scheduleTranslate(): void {
    if (typeof document === 'undefined') {
      return;
    }

    window.setTimeout(() => this.translateDocument(), 0);
  }

  private translateDocument(): void {
    if (typeof document === 'undefined') {
      return;
    }

    this.translateTextNodes(document.body);
    this.translateAttributes(document.body);
  }

  private translateTextNodes(root: HTMLElement): void {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let node = walker.nextNode() as Text | null;

    while (node) {
      const parent = node.parentElement;
      if (parent && !IGNORED_TAGS.has(parent.tagName)) {
        this.translateTextNode(node);
      }
      node = walker.nextNode() as Text | null;
    }
  }

  private translateTextNode(node: Text): void {
    let original = this.originalText.get(node) ?? node.data;
    const last = this.lastText.get(node);
    if (last !== undefined && node.data !== last) {
      original = node.data;
      this.originalText.set(node, original);
    } else if (!this.originalText.has(node)) {
      this.originalText.set(node, original);
    }

    const translated = this.translatePreservingWhitespace(original);
    if (node.data !== translated) {
      node.data = translated;
    }
    this.lastText.set(node, translated);
  }

  private translateAttributes(root: HTMLElement): void {
    const elements = root.querySelectorAll('*');
    for (const element of Array.from(elements)) {
      if (IGNORED_TAGS.has(element.tagName)) {
        continue;
      }

      for (const attribute of TRANSLATABLE_ATTRIBUTES) {
        if (!element.hasAttribute(attribute)) {
          continue;
        }

        const originals = this.originalAttributes.get(element) ?? new Map<string, string>();
        if (!this.originalAttributes.has(element)) {
          this.originalAttributes.set(element, originals);
        }

        const lastValues = this.lastAttributes.get(element) ?? new Map<string, string>();
        if (!this.lastAttributes.has(element)) {
          this.lastAttributes.set(element, lastValues);
        }

        const current = element.getAttribute(attribute) ?? '';
        const previousTranslated = lastValues.get(attribute);
        const original = previousTranslated !== undefined && current !== previousTranslated
          ? current
          : originals.get(attribute) ?? current;
        originals.set(attribute, original);
        const translated = this.translatePreservingWhitespace(original);
        if (current !== translated) {
          element.setAttribute(attribute, translated);
        }
        lastValues.set(attribute, translated);
      }
    }
  }

  private translatePreservingWhitespace(value: string): string {
    if (!value.trim()) {
      return value;
    }

    const prefix = value.match(/^\s*/)?.[0] ?? '';
    const suffix = value.match(/\s*$/)?.[0] ?? '';
    const core = value.trim().replace(/\s+/g, ' ');
    return `${prefix}${this.translateCore(core)}${suffix}`;
  }

  private translateCore(value: string): string {
    const dynamic = this.translateDynamic(value);
    if (dynamic) {
      return dynamic;
    }

    return this.i18n.literal(value);
  }

  private translateDynamic(value: string): string | null {
    const language = this.i18n.language();
    if (language === 'fr') {
      return null;
    }

    if (value.includes(' · ')) {
      const translatedParts = value.split(' · ').map((part) => this.i18n.literal(part.trim()));
      if (translatedParts.some((part, index) => part !== value.split(' · ')[index].trim())) {
        return translatedParts.join(' · ');
      }
    }

    const memory = this.i18n.t('memorySingular');
    const memories = this.i18n.t('memoryPlural');
    const photo = this.i18n.t('photoSingular');
    const photos = this.i18n.t('photoPlural');

    const greeting = /^Bonjour (.+) !$/.exec(value);
    if (greeting) {
      return {
        en: `Hello ${greeting[1]}!`,
        es: `¡Hola ${greeting[1]}!`,
        pt: `Olá ${greeting[1]}!`,
      }[language];
    }

    const selectedPhotos = /^(\d+) photos? sélectionnée?s?$/.exec(value);
    if (selectedPhotos) {
      const count = Number(selectedPhotos[1]);
      return {
        en: `${count} ${count === 1 ? 'photo selected' : 'photos selected'}`,
        es: `${count} ${count === 1 ? 'foto seleccionada' : 'fotos seleccionadas'}`,
        pt: `${count} ${count === 1 ? 'foto selecionada' : 'fotos selecionadas'}`,
      }[language];
    }

    const readyPhotos = /^(\d+) photos? prête?s?$/.exec(value);
    if (readyPhotos) {
      const count = Number(readyPhotos[1]);
      return {
        en: `${count} ${count === 1 ? 'photo ready' : 'photos ready'}`,
        es: `${count} ${count === 1 ? 'foto lista' : 'fotos listas'}`,
        pt: `${count} ${count === 1 ? 'foto pronta' : 'fotos prontas'}`,
      }[language];
    }

    const genericMemoryCount = /^(\d+) souvenirs?$/.exec(value);
    if (genericMemoryCount) {
      const count = Number(genericMemoryCount[1]);
      return `${count} ${count === 1 ? memory : memories}`;
    }

    const genericPhotoCount = /^(\d+) photos?$/.exec(value);
    if (genericPhotoCount) {
      const count = Number(genericPhotoCount[1]);
      return `${count} ${count === 1 ? photo : photos}`;
    }

    const unread = /^(\d+) non lues?$/.exec(value);
    if (unread) {
      const count = Number(unread[1]);
      return {
        en: `${count} unread`,
        es: `${count} sin leer`,
        pt: `${count} por ler`,
      }[language];
    }

    const remainingVideoTokens = /^Il vous reste (\d+) génération\(s\) vidéo\.$/.exec(value);
    if (remainingVideoTokens) {
      return {
        en: `You have ${remainingVideoTokens[1]} video generation(s) left.`,
        es: `Te quedan ${remainingVideoTokens[1]} generación(es) de vídeo.`,
        pt: `Restam ${remainingVideoTokens[1]} geração(ões) de vídeo.`,
      }[language];
    }

    const activePlan = /^(.+) est activé\.$/.exec(value);
    if (activePlan) {
      return {
        en: `${activePlan[1]} is active.`,
        es: `${activePlan[1]} está activo.`,
        pt: `${activePlan[1]} está ativo.`,
      }[language];
    }

    const payPrice = /^Payer (.+)$/.exec(value);
    if (payPrice) {
      return {
        en: `Pay ${payPrice[1]}`,
        es: `Pagar ${payPrice[1]}`,
        pt: `Pagar ${payPrice[1]}`,
      }[language];
    }

    return null;
  }
}
