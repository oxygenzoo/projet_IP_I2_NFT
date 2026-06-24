export interface DraftImage {
  file: File;
  previewUrl: string;
}

export type TravelPreferences = Record<string, string>;

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
