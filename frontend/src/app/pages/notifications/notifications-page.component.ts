import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { Episode, Travel } from '../../models/travel.models';
import { ContributionLink, TravelApiService } from '../../services/travel-api.service';
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
export class NotificationsPageComponent implements OnInit {
  private readonly travelApiService = inject(TravelApiService);
  private readonly readStorageKey = 'nft.readNotifications';

  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly notifications = signal<AppNotification[]>([]);

  protected readonly unreadCount = computed(() => this.notifications().filter((notification) => !notification.read).length);

  ngOnInit(): void {
    this.loadNotifications();
  }

  protected markAllAsRead(): void {
    const readIds = [...this.readIds(), ...this.notifications().map((notification) => notification.id)];
    this.persistReadIds(readIds);
    this.notifications.set([]);
  }

  protected markAsRead(notificationId: string): void {
    this.persistReadIds([...this.readIds(), notificationId]);
    this.notifications.update((notifications) => notifications.filter((notification) => notification.id !== notificationId));
  }

  protected loadNotifications(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    forkJoin({
      travels: this.travelApiService.getTravels().pipe(catchError(() => of([]))),
      episodes: this.travelApiService.getEpisodes().pipe(catchError(() => of([]))),
      contributionLinks: this.travelApiService.getContributionLinks().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ travels, episodes, contributionLinks }) => {
        const readIds = this.readIds();
        this.notifications.set(
          this.buildNotifications(travels, episodes, contributionLinks)
            .filter((notification) => !readIds.includes(notification.id))
            .slice(0, 12),
        );
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les notifications pour le moment.');
        this.notifications.set([]);
        this.isLoading.set(false);
      },
    });
  }

  private buildNotifications(travels: Travel[], episodes: Episode[], contributionLinks: ContributionLink[]): AppNotification[] {
    const travelById = new Map(travels.map((travel) => [travel.id, travel]));
    const notifications: AppNotification[] = [];

    for (const episode of episodes) {
      const travel = travelById.get(episode.travelId);
      const travelLabel = travel?.title ?? episode.location ?? 'Votre voyage';
      const status = (episode.status ?? '').toLowerCase();
      const exportStatus = (episode.exportStatus ?? '').toLowerCase();

      if (episode.videoUrl || status === 'completed' || status === 'ready') {
        notifications.push({
          id: `episode-ready-${episode.id}`,
          title: 'Souvenir prêt',
          description: `${episode.title} est disponible dans ${travelLabel}.`,
          timeLabel: this.formatDate(episode.episodeDate || episode.date),
          kind: 'video',
          read: false,
          link: ['/episode', episode.id],
        });
      }

      if (status === 'generating' || status === 'pending') {
        notifications.push({
          id: `episode-generating-${episode.id}`,
          title: 'Génération en cours',
          description: `${episode.title} est en préparation.`,
          timeLabel: this.formatDate(episode.episodeDate || episode.date),
          kind: 'account',
          read: false,
          link: ['/generating'],
        });
      }

      if (status === 'failed') {
        notifications.push({
          id: `episode-failed-${episode.id}`,
          title: 'Génération à relancer',
          description: `${episode.title} n'a pas pu être généré correctement.`,
          timeLabel: this.formatDate(episode.episodeDate || episode.date),
          kind: 'account',
          read: false,
          link: ['/episode', episode.id],
        });
      }

      if (episode.shareToken) {
        notifications.push({
          id: `episode-shared-${episode.id}`,
          title: 'Lien de partage actif',
          description: `${episode.title} possède un lien de partage.`,
          timeLabel: this.formatDate(episode.episodeDate || episode.date),
          kind: 'share',
          read: false,
          link: ['/episode', episode.id],
        });
      }

      if (exportStatus === 'ready') {
        notifications.push({
          id: `episode-export-ready-${episode.id}`,
          title: 'Export disponible',
          description: `L'export de ${episode.title} est prêt.`,
          timeLabel: this.formatDate(episode.episodeDate || episode.date),
          kind: 'video',
          read: false,
          link: ['/episode', episode.id],
        });
      }
    }

    for (const link of contributionLinks) {
      const travel = travelById.get(link.travelId);
      const travelLabel = travel?.title ?? 'votre souvenir';
      if (link.openedAt) {
        notifications.push({
          id: `contribution-opened-${link.id}`,
          title: 'Lien de contribution ouvert',
          description: `Quelqu'un a ouvert le lien pour ${travelLabel}.`,
          timeLabel: this.formatDate(link.openedAt),
          kind: 'share',
          read: false,
          link: ['/upload'],
        });
      }

      if ((link.uploadCount ?? 0) > 0) {
        notifications.push({
          id: `contribution-uploaded-${link.id}-${link.uploadCount}`,
          title: 'Photos reçues',
          description: `${link.uploadCount} photo${(link.uploadCount ?? 0) > 1 ? 's ont' : ' a'} été ajoutée${(link.uploadCount ?? 0) > 1 ? 's' : ''} à ${travelLabel}.`,
          timeLabel: this.formatDate(link.lastUploadAt ?? link.createdAt),
          kind: 'account',
          read: false,
          link: ['/upload'],
        });
      }
    }

    for (const travel of travels.filter((travel) => travel.photoCount > 0 && travel.episodeCount === 0)) {
      notifications.push({
        id: `travel-photos-${travel.id}`,
        title: 'Photos importées',
        description: `${travel.photoCount} photo${travel.photoCount > 1 ? 's' : ''} attendent un souvenir pour ${travel.title}.`,
        timeLabel: 'À traiter',
        kind: 'account',
        read: false,
        link: ['/upload'],
      });
    }

    return notifications;
  }

  private formatDate(value?: string): string {
    if (!value) {
      return 'Récemment';
    }

    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }

  private readIds(): string[] {
    try {
      return JSON.parse(localStorage.getItem(this.readStorageKey) ?? '[]');
    } catch {
      return [];
    }
  }

  private persistReadIds(ids: string[]): void {
    localStorage.setItem(this.readStorageKey, JSON.stringify(Array.from(new Set(ids))));
  }
}
