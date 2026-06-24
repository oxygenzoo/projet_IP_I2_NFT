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
      lieu?: string;
      date?: string;
      scenes?: unknown[];
    }>;
  };
  videos: string[];
  workdir: string;
}
