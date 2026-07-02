import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { PricingPlan } from '../../models/travel.models';
import { AuthService } from '../../services/auth.service';
import { SubscriptionQuotaService } from '../../services/subscription-quota.service';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-payment-page',
  imports: [ReactiveFormsModule, RouterLink, AppLogoComponent],
  templateUrl: './payment-page.component.html',
})
export class PaymentPageComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly quota = inject(SubscriptionQuotaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly travelApiService = inject(TravelApiService);
  private pricingSubscription?: Subscription;
  private redirectId?: ReturnType<typeof setTimeout>;

  protected readonly plan = signal<PricingPlan | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly isProcessing = signal(false);
  protected readonly isPaid = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly paymentForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    cardName: ['', [Validators.required, Validators.minLength(2)]],
    cardNumber: ['4242 4242 4242 4242', [Validators.required, Validators.pattern(/^[0-9 ]{16,23}$/)]],
    expiry: ['12/29', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/[0-9]{2}$/)]],
    cvc: ['123', [Validators.required, Validators.pattern(/^[0-9]{3,4}$/)]],
    zipCode: ['75001', [Validators.required, Validators.minLength(4)]],
  });

  ngOnInit(): void {
    void this.prefillFromProfile();
    const planId = this.route.snapshot.paramMap.get('planId') ?? '';

    if (!planId || this.quota.isCurrentPlan(planId)) {
      void this.router.navigate(['/pricing'], { replaceUrl: true });
      return;
    }

    this.pricingSubscription = this.travelApiService.getPricingPlans().subscribe({
      next: (plans) => {
        const selectedPlan = plans.find((plan) => plan.id === planId) ?? null;

        if (!selectedPlan) {
          this.errorMessage.set('Cette offre est introuvable.');
        }

        this.plan.set(selectedPlan);
        this.isLoading.set(false);
      },
      error: () => {
        const selectedPlan = this.quota.demoPricingPlans.find((plan) => plan.id === planId) ?? null;

        if (!selectedPlan) {
          this.errorMessage.set('Cette offre est introuvable.');
        }

        this.plan.set(selectedPlan);
        this.isLoading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.pricingSubscription?.unsubscribe();

    if (this.redirectId) {
      clearTimeout(this.redirectId);
    }
  }

  protected pay(): void {
    const selectedPlan = this.plan();

    if (!selectedPlan || this.paymentForm.invalid || this.isProcessing()) {
      this.paymentForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    this.errorMessage.set('');

    this.redirectId = setTimeout(() => {
      this.quota.activatePlan(selectedPlan.id);
      this.isProcessing.set(false);
      this.isPaid.set(true);
    }, 900);
  }

  private async prefillFromProfile(): Promise<void> {
    const profile = await this.authService.getCurrentProfile();

    if (!profile) {
      return;
    }

    this.paymentForm.patchValue({
      email: profile.email,
      cardName: profile.name,
    });
  }
}
