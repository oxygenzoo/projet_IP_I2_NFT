import { generatedEnvironment } from './environment.generated';

export const environment = {
  production: false,
  apiUrl: generatedEnvironment.apiUrl,
  supabaseUrl: generatedEnvironment.supabaseUrl,
  supabaseAnonKey: generatedEnvironment.supabaseAnonKey,
  googleClientId: generatedEnvironment.googleClientId,
  googleApiKey: generatedEnvironment.googleApiKey,
} as const;
