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
  title?: string;
  timecode?: string;
  description?: string;
  imageUrl?: string;
  voiceOverText?: string;
  voiceoverText?: string;
  narrationText?: string;
  status?: SceneGenerationStatus;
  order?: number | string;
  sceneOrder?: number | string;
  position?: number | string;
  duration?: number | string;
  createdAt?: string;
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
  date: string;
  photoCount: number;
  coverImage: string;
  videoStill: string;
  progress: number;
  remaining: string;
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
