declare module 'qrcode' {
  export type ErrorCorrectionLevel = 'L' | 'M' | 'Q' | 'H';

  export interface QRCodeToDataURLOptions {
    errorCorrectionLevel?: ErrorCorrectionLevel;
    margin?: number;
    scale?: number;
    color?: {
      dark?: string;
      light?: string;
    };
  }

  const QRCode: {
    toDataURL(text: string, options?: QRCodeToDataURLOptions): Promise<string>;
  };

  export default QRCode;
}
