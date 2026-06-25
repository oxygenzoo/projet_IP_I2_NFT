import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService, SocialProvider } from '../../services/auth.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, AppLogoComponent],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
})
export class LoginPageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);

  protected readonly isLoading = signal(false);
  protected readonly mode = signal<'login' | 'signup'>('login');
  protected readonly activeProvider = signal<SocialProvider | null>(null);
  protected readonly statusMessage = signal('');
  protected readonly statusTone = signal<'info' | 'error' | 'success'>('info');

  protected readonly loginForm = this.formBuilder.nonNullable.group({
    fullName: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  ngOnInit(): void {
    const mode = this.route.snapshot.queryParamMap.get('mode');

    if (mode === 'signup') {
      this.setMode('signup');
    }
  }

  protected async submitLogin(): Promise<void> {
    if (this.loginForm.invalid || this.isLoading()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.clearStatus();

    const credentials = this.loginForm.getRawValue();
    const result =
      this.mode() === 'signup'
        ? await this.authService.signup(credentials)
        : await this.authService.login(credentials);

    await this.completeAuth(result.success, result.message);
  }

  protected async loginWith(provider: SocialProvider): Promise<void> {
    if (this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.activeProvider.set(provider);
    this.clearStatus();

    const result = await this.authService.loginWithSocialProvider(provider);
    await this.completeAuth(result.success, result.message);
  }

  protected switchMode(event: Event, mode: 'login' | 'signup'): void {
    event.preventDefault();
    this.setMode(mode);
  }

  private setMode(mode: 'login' | 'signup'): void {
    this.mode.set(mode);
    this.clearStatus();

    const fullNameControl = this.loginForm.controls.fullName;

    if (mode === 'signup') {
      fullNameControl.setValidators([Validators.required, Validators.minLength(2)]);
    } else {
      fullNameControl.clearValidators();
    }

    fullNameControl.updateValueAndValidity();
  }

  private async completeAuth(success: boolean, message?: string): Promise<void> {
    if (!success) {
      this.setStatus(message ?? 'Authentification impossible.', 'error');
      this.isLoading.set(false);
      this.activeProvider.set(null);
      return;
    }

    if (this.mode() === 'signup' && message?.includes('Vérifiez vos emails')) {
      this.setStatus(message, 'success');
      this.isLoading.set(false);
      this.activeProvider.set(null);
      return;
    }

    this.setStatus(message ?? 'Connexion réussie.', 'success');
    await this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('redirect') ?? '/home');
  }

  private setStatus(message: string, tone: 'info' | 'error' | 'success'): void {
    this.statusMessage.set(message);
    this.statusTone.set(tone);
  }

  private clearStatus(): void {
    this.statusMessage.set('');
    this.statusTone.set('info');
  }
}
