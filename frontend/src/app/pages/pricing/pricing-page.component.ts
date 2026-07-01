import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { PricingPlan } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-pricing-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './pricing-page.component.html',
})
export class PricingPageComponent implements OnInit, OnDestroy {
  private readonly travelApiService = inject(TravelApiService);
  private pricingSubscription?: Subscription;

  protected readonly plans = signal<PricingPlan[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  ngOnInit(): void {
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
        this.plans.set([]);
        this.errorMessage.set('Impossible de charger les offres depuis le backend.');
        this.isLoading.set(false);
      },
    });
  }
}
