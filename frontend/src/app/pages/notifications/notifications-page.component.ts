import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

interface AppNotification {
  id: string;
  title: string;
  description: string;
  timeLabel: string;
  kind: 'video' | 'share' | 'account';
  read: boolean;
  link?: string[];
}

@Component({
  selector: 'app-notifications-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './notifications-page.component.html',
})
export class NotificationsPageComponent {
  protected readonly notifications = signal<AppNotification[]>([
    {
      id: 'video-ready',
      title: 'Votre vidéo est prête',
      description: 'Le montage de votre dernier souvenir est disponible dans vos épisodes.',
      timeLabel: 'Il y a 8 min',
      kind: 'video',
      read: false,
      link: ['/home'],
    },
    {
      id: 'shared-memory-viewed',
      title: 'Souvenir regardé',
      description: 'Quelqu’un a regardé votre souvenir via un lien partagé.',
      timeLabel: 'Il y a 42 min',
      kind: 'share',
      read: false,
    },
    {
      id: 'export-ready',
      title: 'Export disponible',
      description: 'Votre export privé est prêt à être téléchargé.',
      timeLabel: 'Hier',
      kind: 'video',
      read: false,
    },
  ]);

  protected readonly unreadCount = computed(() => this.notifications().filter((notification) => !notification.read).length);

  protected markAllAsRead(): void {
    this.notifications.set([]);
  }

  protected markAsRead(notificationId: string): void {
    this.notifications.update((notifications) => notifications.filter((notification) => notification.id !== notificationId));
  }
}
