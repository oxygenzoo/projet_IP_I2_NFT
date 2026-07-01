import { rankPhotos, scorePhoto, selectTopPhotos } from './photo-scoring';

describe('photo scoring', () => {
  it('classe une liste vide', () => {
    expect(rankPhotos([])).toEqual([]);
  });

  it('penalise les photos floues', () => {
    const sharp = scorePhoto({ id: 'sharp', global_score: 80 });
    const blurry = scorePhoto({ id: 'blurry', global_score: 80, is_blurry: true });

    expect(blurry).toBeLessThan(sharp);
  });

  it('modifie le classement avec les preferences', () => {
    const ranked = rankPhotos(
      [
        { id: 'a', global_score: 70, tags: ['groupe'] },
        { id: 'b', global_score: 65, tags: ['paysage'] },
      ],
      { moments: 'paysage' },
    );

    expect(ranked[0].id).toBe('b');
  });

  it('retourne une nouvelle liste sans muter les objets source', () => {
    const photos = [
      { id: 'a', global_score: 40, tags: ['groupe'] },
      { id: 'b', global_score: 90, tags: ['monument'] },
    ];
    const before = structuredClone(photos);

    const ranked = rankPhotos(photos);

    expect(ranked).not.toBe(photos);
    expect(photos).toEqual(before);
    expect(ranked[0]).not.toBe(photos[1]);
  });

  it('retire les doublons et respecte le top N', () => {
    const ranked = selectTopPhotos(
      [
        { id: 'a', final_score: 90, duplicateKey: 'same' },
        { id: 'b', final_score: 95, duplicateKey: 'same' },
        { id: 'c', final_score: 80, is_duplicate: true },
        { id: 'd', final_score: 70 },
      ],
      {},
      2,
    );

    expect(ranked.map((photo) => photo.id)).toEqual(['a', 'd']);
  });
});
