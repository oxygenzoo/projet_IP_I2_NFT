import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { PricingPlan } from '../../models/travel.models';
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
  protected readonly quota = inject(SubscriptionQuotaService);
  private readonly travelApiService = inject(TravelApiService);
  private pricingSubscription?: Subscription;

  protected readonly plans = signal<PricingPlan[]>([]);
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
