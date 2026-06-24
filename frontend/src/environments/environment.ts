import { generatedEnvironment } from './environment.generated';

export const environment = {
  production: false,
  apiUrl: generatedEnvironment.apiUrl,
  supabaseUrl: generatedEnvironment.supabaseUrl,
  supabaseAnonKey: generatedEnvironment.supabaseAnonKey,
} as const;
