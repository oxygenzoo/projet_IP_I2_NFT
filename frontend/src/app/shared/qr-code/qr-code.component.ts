import { Component, effect, input, signal } from '@angular/core';
import QRCode from 'qrcode';

@Component({
  selector: 'app-qr-code',
  templateUrl: './qr-code.component.html',
  styleUrl: './qr-code.component.scss',
})
export class QrCodeComponent {
  readonly value = input.required<string>();
  readonly label = input('QR code');

  protected readonly dataUrl = signal('');

  constructor() {
    effect(() => {
      const value = this.value();
      if (!value) {
        this.dataUrl.set('');
        return;
      }

      void QRCode.toDataURL(value, {
        errorCorrectionLevel: 'H',
        margin: 4,
        scale: 8,
        color: {
          dark: '#050505',
          light: '#ffffff',
        },
      }).then((url: string) => this.dataUrl.set(url));
    });
  }
}
