import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, SocialProvider } from '../../services/auth.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, AppLogoComponent],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
})
export class LoginPageComponent {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);

  protected readonly isLoading = signal(false);
  protected readonly activeProvider = signal<SocialProvider | null>(null);
  protected readonly statusMessage = signal('');

  protected readonly loginForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  protected async submitLogin(): Promise<void> {
    if (this.loginForm.invalid || this.isLoading()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.statusMessage.set('');

    const result = await this.authService.login(this.loginForm.getRawValue());
    await this.completeMockLogin(result.success);
  }

  protected async loginWith(provider: SocialProvider): Promise<void> {
    if (this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.activeProvider.set(provider);
    this.statusMessage.set('');

    const result = await this.authService.loginWithSocialProvider(provider);
    await this.completeMockLogin(result.success);
  }

  protected showSignupMessage(event: Event): void {
    event.preventDefault();
    this.statusMessage.set("La création de compte sera disponible avec Supabase Auth.");
  }

  private async completeMockLogin(success: boolean): Promise<void> {
    if (!success) {
      this.statusMessage.set('La connexion mockée a échoué.');
      this.isLoading.set(false);
      this.activeProvider.set(null);
      return;
    }

    this.statusMessage.set('Connexion mockée réussie');
    await this.router.navigate(['/home']);
  }
}
