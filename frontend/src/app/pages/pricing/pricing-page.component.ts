import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { PricingPlan } from '../../models/travel.models';
import { I18nService } from '../../services/i18n.service';
import { SubscriptionQuotaService } from '../../services/subscription-quota.service';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-pricing-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './pricing-page.component.html',
})
export class PricingPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  protected readonly i18n = inject(I18nService);
  protected readonly quota = inject(SubscriptionQuotaService);
  private readonly travelApiService = inject(TravelApiService);
  private pricingSubscription?: Subscription;

  protected readonly plans = signal<PricingPlan[]>([]);
  protected readonly localizedPlans = computed(() =>
    this.plans().map((plan) => ({
      ...plan,
      name: this.i18n.literal(plan.name),
      cadence: this.i18n.literal(plan.cadence),
      description: this.i18n.literal(plan.description),
      audience: plan.audience ? this.i18n.literal(plan.audience) : plan.audience,
      features: plan.features.map((feature) => this.i18n.literal(feature)),
      price: this.i18n.literal(plan.price),
    })),
  );
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly noticeMessage = signal('');

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('reason') === 'token-limit') {
      this.noticeMessage.set('Vous avez utilisé vos 2 générations gratuites. Choisissez une offre pour continuer.');
    }

    this.loadPlans();
  }

  ngOnDestroy(): void {
    this.pricingSubscription?.unsubscribe();
  }

  protected loadPlans(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.pricingSubscription?.unsubscribe();
    this.pricingSubscription = this.travelApiService.getPricingPlans().subscribe({
      next: (plans) => {
        this.plans.set(plans);
        this.isLoading.set(false);
      },
      error: () => {
        this.plans.set(this.quota.demoPricingPlans);
        this.errorMessage.set('');
        this.isLoading.set(false);
      },
    });
  }
}
