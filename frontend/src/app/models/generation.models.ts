export interface DraftImage {
  file: File;
  previewUrl: string;
}

export type TravelPreferences = Record<string, string>;

export interface GenerationResponse {
  job_id: string;
  status: string;
  message: string;
  selection_report: Record<string, unknown>;
  script: {
    voyage?: string;
    nb_episodes?: number;
    episodes?: Array<{
      episode_numero?: number;
      episode_titre?: string;
      intro?: string;
      outro?: string;
      lieu?: string;
      date?: string;
      scenes?: Array<{
        duree_secondes?: number;
      }>;
    }>;
  };
  videos: string[];
  workdir: string;
}

export type CreationStatus = 'uploading' | 'preferences' | 'generating' | 'done' | 'error';

export interface CreationSession {
  id: string;
  ownerId: string;
  travelId?: string | null;
  episodeId?: string | null;
  status: CreationStatus;
  resultVideoUrl?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TravelPreferenceResponse extends TravelPreferences {
  id: string;
  travelId: string;
  style: string;
  people: string;
  moments: string;
  tone: string;
  createdAt: string;
  updatedAt: string;
}
