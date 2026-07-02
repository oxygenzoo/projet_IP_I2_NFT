export interface User {
  id: string;
  name: string;
  avatarUrl: string;
  plan: string;
  totalPhotos: number;
  totalEpisodes: number;
}

export type SceneGenerationStatus = 'pending' | 'generating' | 'completed' | 'failed';

export interface Scene {
  id: string;
  order?: number;
  title: string;
  timecode: string;
  description?: string;
  voiceOverText?: string;
  type?: 'intro' | 'souvenir' | 'transition' | 'conclusion';
  generationStatus?: SceneGenerationStatus | 'generated' | 'ai_reconstructed' | string;
  imageUrl?: string | null;
  photoUrl?: string | null;
  isAiReconstructed?: boolean;
  aiPrompt?: string | null;
}

export interface Episode {
  id: string;
  travelId: string;
  seasonNumber: number;
  episodeNumber: number;
  title: string;
  subtitle: string;
  summary: string;
  duration: string;
  location: string;
  locationName?: string;
  date: string;
  episodeDate?: string;
  introText?: string;
  photoCount: number;
  coverImage: string;
  videoStill: string;
  progress: number;
  remaining: string;
  status?: string;
  shareToken?: string;
  exportStatus?: 'idle' | 'pending' | 'ready' | 'failed' | string;
  videoUrl?: string;
  favorite?: boolean;
  sharedBy?: string;
  scenes: Scene[];
}

export interface Travel {
  id: string;
  title: string;
  destination: string;
  country: string;
  year: number;
  tagline: string;
  description: string;
  coverImage: string;
  heroImage: string;
  posterImage: string;
  duration: string;
  episodeCount: number;
  photoCount: number;
  progress: number;
  remaining: string;
  featured: boolean;
  moodTags: string[];
  episodes: Episode[];
}

export interface Photo {
  id: string;
  travelId: string;
  filename: string;
  size: number;
  type: string;
  imageUrl: string;
  uploadedAt?: string;
}

export interface PricingPlan {
  id: string;
  name: string;
  price: string;
  cadence: string;
  description: string;
  features: string[];
  highlighted?: boolean;
  audience?: string;
}
