import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router, provideRouter } from '@angular/router';

import { API_URL } from '../../config/api.config';
import { TravelDraftService } from '../../services/travel-draft.service';
import { PreferencesPageComponent } from './preferences-page.component';

describe('PreferencesPageComponent', () => {
  let fixture: ComponentFixture<PreferencesPageComponent>;
  let httpTesting: HttpTestingController;
  let draft: TravelDraftService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PreferencesPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: 'http://api.test' },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ travelId: 'travel-1' }),
              queryParamMap: convertToParamMap({}),
            },
          },
        },
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    draft = TestBed.inject(TravelDraftService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    draft.selectedImages.set([
      {
        file: new File(['photo'], 'plage.jpg', { type: 'image/jpeg' }),
        previewUrl: 'blob:plage.jpg',
      },
    ]);

    fixture = TestBed.createComponent(PreferencesPageComponent);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
    vi.restoreAllMocks();
  });

  it('recharge les préférences déjà enregistrées', () => {
    httpTesting.expectOne('http://api.test/api/travels/travel-1/preferences').flush({
      id: 'preference-1',
      travelId: 'travel-1',
      style: 'Documentaire',
      people: 'Couple',
      moments: 'Culture',
      tone: 'Nostalgique',
      createdAt: '2026-06-24T10:00:00Z',
      updatedAt: '2026-06-24T10:00:00Z',
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Documentaire · Couple · Culture · Nostalgique');
    expect(draft.preferences()).toEqual({
      style: 'Documentaire',
      people: 'Couple',
      moments: 'Culture',
      tone: 'Nostalgique',
    });
  });

  it('sauvegarde le questionnaire pour le voyage courant', () => {
    httpTesting
      .expectOne('http://api.test/api/travels/travel-1/preferences')
      .flush({}, { status: 404, statusText: 'Not Found' });

    fixture.nativeElement.querySelector('button.btn--primary').click();

    const request = httpTesting.expectOne('http://api.test/api/travels/travel-1/preferences');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      style: 'Cinematographique',
      people: 'Tout le monde',
      moments: 'Paysages',
      tone: 'Inspirant',
    });

    request.flush({
      id: 'preference-1',
      travelId: 'travel-1',
      style: 'Cinematographique',
      people: 'Tout le monde',
      moments: 'Paysages',
      tone: 'Inspirant',
      createdAt: '2026-06-24T10:00:00Z',
      updatedAt: '2026-06-24T10:00:00Z',
    });

    expect(router.navigate).toHaveBeenCalledWith(['/generating']);
  });
});
