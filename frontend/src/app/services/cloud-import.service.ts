import { Injectable } from '@angular/core';

import { environment } from '../../environments/environment';

declare global {
  interface Window {
    google?: any;
    gapi?: any;
  }
}

@Injectable({ providedIn: 'root' })
export class CloudImportService {
  async importFromDrive(): Promise<File[]> {
    this.assertGoogleConfig();
    await Promise.all([
      this.loadScript('https://accounts.google.com/gsi/client'),
      this.loadScript('https://apis.google.com/js/api.js'),
    ]);
    await this.loadPickerApi();
    const token = await this.requestToken('https://www.googleapis.com/auth/drive.readonly');
    const docs = await this.pickDriveImages(token);
    return Promise.all(docs.map((doc) => this.downloadDriveFile(doc.id, doc.name, token)));
  }

  async importFromGooglePhotos(): Promise<File[]> {
    this.assertGoogleConfig();
    await this.loadScript('https://accounts.google.com/gsi/client');
    const token = await this.requestToken('https://www.googleapis.com/auth/photoslibrary.readonly');
    const response = await fetch('https://photoslibrary.googleapis.com/v1/mediaItems?pageSize=20', {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!response.ok) {
      throw new Error("Google Photos n'a pas renvoyé de photos importables.");
    }

    const payload = await response.json();
    const mediaItems = (payload.mediaItems ?? []).filter((item: any) => item.mimeType?.startsWith('image/'));
    if (!mediaItems.length) {
      throw new Error('Aucune photo Google Photos disponible.');
    }

    return Promise.all(mediaItems.slice(0, 20).map((item: any) => this.downloadUrl(`${item.baseUrl}=d`, item.filename)));
  }

  private async downloadDriveFile(id: string, name: string, token: string): Promise<File> {
    const response = await fetch(`https://www.googleapis.com/drive/v3/files/${id}?alt=media`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) {
      throw new Error(`Import impossible pour ${name}.`);
    }
    const blob = await response.blob();
    return new File([blob], name || 'drive-photo.jpg', { type: blob.type || 'image/jpeg' });
  }

  private async downloadUrl(url: string, name: string): Promise<File> {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`Import impossible pour ${name}.`);
    }
    const blob = await response.blob();
    return new File([blob], name || 'google-photo.jpg', { type: blob.type || 'image/jpeg' });
  }

  private pickDriveImages(token: string): Promise<Array<{ id: string; name: string }>> {
    return new Promise((resolve, reject) => {
      const picker = new window.google.picker.PickerBuilder()
        .setDeveloperKey(environment.googleApiKey)
        .setOAuthToken(token)
        .addView(new window.google.picker.DocsView(window.google.picker.ViewId.DOCS_IMAGES).setIncludeFolders(true))
        .enableFeature(window.google.picker.Feature.MULTISELECT_ENABLED)
        .setCallback((data: any) => {
          if (data.action === window.google.picker.Action.PICKED) {
            resolve(data.docs.map((doc: any) => ({ id: doc.id, name: doc.name })));
          }
          if (data.action === window.google.picker.Action.CANCEL) {
            resolve([]);
          }
        })
        .build();
      try {
        picker.setVisible(true);
      } catch (error) {
        reject(error);
      }
    });
  }

  private requestToken(scope: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const client = window.google.accounts.oauth2.initTokenClient({
        client_id: environment.googleClientId,
        scope,
        callback: (response: any) => {
          if (response.error) {
            reject(new Error(response.error));
            return;
          }
          resolve(response.access_token);
        },
      });
      client.requestAccessToken({ prompt: 'consent' });
    });
  }

  private loadPickerApi(): Promise<void> {
    return new Promise((resolve) => {
      window.gapi.load('picker', () => resolve());
    });
  }

  private loadScript(src: string): Promise<void> {
    if (document.querySelector(`script[src="${src}"]`)) {
      return Promise.resolve();
    }
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = src;
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error(`Impossible de charger ${src}.`));
      document.head.appendChild(script);
    });
  }

  private assertGoogleConfig(): void {
    if (!environment.googleClientId || !environment.googleApiKey) {
      throw new Error('Configurez GOOGLE_CLIENT_ID et GOOGLE_API_KEY pour activer cet import.');
    }
  }
}
