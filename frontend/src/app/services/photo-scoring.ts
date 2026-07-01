export interface PhotoScoreInput {
  id: string;
  final_score?: number;
  global_score?: number;
  quality?: number;
  is_blurry?: boolean;
  is_duplicate?: boolean;
  duplicateKey?: string;
  tags?: readonly string[];
}

export interface PhotoScorePreferences {
  moments?: string;
  preferredTags?: readonly string[];
}

export interface RankedPhoto extends PhotoScoreInput {
  score: number;
}

export function scorePhoto(photo: PhotoScoreInput, preferences: PhotoScorePreferences = {}): number {
  const baseScore = firstNumber(photo.final_score, photo.global_score, photo.quality, 50);
  const blurPenalty = photo.is_blurry ? 30 : 0;
  const duplicatePenalty = photo.is_duplicate ? 1000 : 0;
  const preferenceBonus = matchedTags(photo.tags ?? [], preferenceTags(preferences)).length * 12;

  return Math.max(0, Math.round(baseScore + preferenceBonus - blurPenalty - duplicatePenalty));
}

export function rankPhotos(
  photos: readonly PhotoScoreInput[],
  preferences: PhotoScorePreferences = {},
): RankedPhoto[] {
  const seen = new Set<string>();

  return photos
    .filter((photo) => {
      const key = photo.duplicateKey ?? photo.id;
      if (photo.is_duplicate || seen.has(key)) {
        return false;
      }

      seen.add(key);
      return true;
    })
    .map((photo) => ({ ...photo, score: scorePhoto(photo, preferences) }))
    .sort((left, right) => right.score - left.score || left.id.localeCompare(right.id));
}

export function selectTopPhotos(
  photos: readonly PhotoScoreInput[],
  preferences: PhotoScorePreferences = {},
  limit = 5,
): RankedPhoto[] {
  return rankPhotos(photos, preferences).slice(0, Math.max(0, limit));
}

function preferenceTags(preferences: PhotoScorePreferences): string[] {
  const tags = preferences.preferredTags ?? [];
  const moment = preferences.moments ? [preferences.moments] : [];

  return [...tags, ...moment].map((tag) => tag.trim().toLowerCase()).filter(Boolean);
}

function matchedTags(photoTags: readonly string[], preferredTags: readonly string[]): string[] {
  const normalizedPhotoTags = new Set(photoTags.map((tag) => tag.trim().toLowerCase()));

  return preferredTags.filter((tag) => normalizedPhotoTags.has(tag));
}

function firstNumber(...values: Array<number | undefined>): number {
  return values.find((value) => Number.isFinite(value)) ?? 50;
}
