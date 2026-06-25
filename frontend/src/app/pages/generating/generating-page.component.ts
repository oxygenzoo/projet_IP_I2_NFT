import { isPlatformBrowser } from '@angular/common';
import { Component, OnDestroy, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-generating-page',
  imports: [AppLogoComponent],
  templateUrl: './generating-page.component.html',
})
export class GeneratingPageComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private intervalId?: ReturnType<typeof setInterval>;
  private redirectId?: ReturnType<typeof setTimeout>;

  protected readonly steps = [
    'Analyse des photos',
    'Selection des meilleurs souvenirs',
    'Creation du recit',
    'Reconstitution des moments manquants',
    'Creation des episodes',
  ];

  protected readonly percent = signal(7);
  protected readonly activeIndex = signal(0);
  protected readonly completed = signal<string[]>([]);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.percent.set(100);
      this.completed.set(this.steps);
      this.activeIndex.set(this.steps.length - 1);
      return;
    }

    this.intervalId = setInterval(() => {
      const nextPercent = Math.min(this.percent() + 9, 100);
      const nextStepIndex = Math.min(Math.floor(nextPercent / 22), this.steps.length - 1);

      this.percent.set(nextPercent);
      this.activeIndex.set(nextStepIndex);
      this.completed.set(this.steps.slice(0, nextStepIndex));

      if (nextPercent >= 100) {
        this.completed.set(this.steps);
        this.clearTimers();
        this.redirectId = setTimeout(() => {
          this.router.navigate(['/home']);
        }, 700);
      }
    }, 520);
  }

  ngOnDestroy(): void {
    this.clearTimers();
  }

  private clearTimers(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = undefined;
    }

    if (this.redirectId) {
      clearTimeout(this.redirectId);
      this.redirectId = undefined;
    }
  }
}
