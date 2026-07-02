import { Injectable, computed, signal } from '@angular/core';

export interface DemoPlanQuota {
  id: string;
  name: string;
  videoTokens: number;
}

@Injectable({ providedIn: 'root' })
export class SubscriptionQuotaService {
  private static readonly PLAN_STORAGE_KEY = 'nft-demo-plan';
  private static readonly USED_STORAGE_KEY = 'nft-demo-used-video-tokens';

  private readonly plans: Record<string, DemoPlanQuota> = {
    starter: { id: 'starter', name: 'Découverte', videoTokens: 2 },
    travel: { id: 'travel', name: 'Voyage complet', videoTokens: 6 },
    archive: { id: 'archive', name: 'Archive', videoTokens: 30 },
    organization: { id: 'organization', name: 'Organisation', videoTokens: 100 },
  };

  readonly currentPlanId = signal(this.readPlanId());
  readonly usedVideoTokens = signal(this.readUsedTokens());
  readonly currentPlan = computed(() => this.plans[this.currentPlanId()] ?? this.plans['starter']);
  readonly remainingVideoTokens = computed(() => Math.max(0, this.currentPlan().videoTokens - this.usedVideoTokens()));

  readonly demoPricingPlans = [
    {
      id: 'starter',
      name: 'Starter',
      price: '0 EUR',
      cadence: 'pour commencer',
      description: 'Premier souvenir pour découvrir la génération de souvenirs.',
      features: ['1 voyage', '2 générations vidéo', 'Export standard'],
      highlighted: false,
      audience: 'Découverte',
    },
    {
      id: 'travel',
      name: 'Voyage complet',
      price: '9 EUR',
      cadence: 'par voyage',
      description: 'Un voyage transformé en souvenir partageable.',
      features: ["Jusqu'à 6 souvenirs", 'Sélection IA', 'Lien de partage', 'Export souvenir si disponible'],
      highlighted: true,
      audience: 'Le plus choisi',
    },
    {
      id: 'archive',
      name: 'Archive',
      price: '19 EUR',
      cadence: 'par mois',
      description: 'Pour conserver plusieurs voyages et exports dans le temps.',
      features: ['Voyages multiples', 'Historique complet', 'Exports prioritaires', 'Support groupe'],
      highlighted: false,
      audience: 'Régulier',
    },
    {
      id: 'organization',
      name: 'Organisation',
      price: 'Sur devis',
      cadence: 'équipes',
      description: 'Offre adaptée aux groupes, associations et séjours organisés.',
      features: ['Espace administrateur', 'Gestion multi-voyages', 'Accompagnement', 'Facturation dédiée'],
      highlighted: false,
      audience: 'Groupes',
    },
  ];

  getPlanName(planId: string): string {
    return this.plans[planId]?.name ?? planId;
  }

  isCurrentPlan(planId: string): boolean {
    return this.currentPlanId() === planId;
  }

  canGenerateVideo(): boolean {
    return this.remainingVideoTokens() > 0;
  }

  consumeVideoToken(): void {
    const nextUsedTokens = Math.min(this.currentPlan().videoTokens, this.usedVideoTokens() + 1);
    this.usedVideoTokens.set(nextUsedTokens);
    this.writeNumber(SubscriptionQuotaService.USED_STORAGE_KEY, nextUsedTokens);
  }

  activatePlan(planId: string): void {
    const nextPlanId = this.plans[planId] ? planId : 'starter';
    this.currentPlanId.set(nextPlanId);
    this.writeText(SubscriptionQuotaService.PLAN_STORAGE_KEY, nextPlanId);
    this.resetVideoTokens();
  }

  resetVideoTokens(): void {
    this.usedVideoTokens.set(0);
    this.writeNumber(SubscriptionQuotaService.USED_STORAGE_KEY, 0);
  }

  private readPlanId(): string {
    const planId = this.readText(SubscriptionQuotaService.PLAN_STORAGE_KEY);
    return planId && this.plans[planId] ? planId : 'starter';
  }

  private readUsedTokens(): number {
    const rawValue = this.readText(SubscriptionQuotaService.USED_STORAGE_KEY);
    const parsedValue = Number(rawValue);
    return Number.isFinite(parsedValue) && parsedValue >= 0 ? parsedValue : 0;
  }

  private readText(key: string): string {
    if (typeof window === 'undefined') {
      return '';
    }

    return window.localStorage.getItem(key) ?? '';
  }

  private writeText(key: string, value: string): void {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(key, value);
  }

  private writeNumber(key: string, value: number): void {
    this.writeText(key, String(value));
  }
}
