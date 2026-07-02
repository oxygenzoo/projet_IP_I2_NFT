import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { I18nDomService } from './services/i18n-dom.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly i18nDom = inject(I18nDomService);

  constructor() {
    this.i18nDom.start();
  }
}
