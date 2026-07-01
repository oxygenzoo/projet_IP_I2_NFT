import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { UploadPageComponent } from './upload-page.component';

describe('UploadPageComponent', () => {
  let fixture: ComponentFixture<UploadPageComponent>;
  let input: HTMLInputElement;

  beforeEach(async () => {
    vi.spyOn(URL, 'createObjectURL').mockImplementation(
      (file) => `blob:${(file as File).name}`,
    );
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);

    await TestBed.configureTestingModule({
      imports: [UploadPageComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadPageComponent);
    fixture.detectChanges();
    input = fixture.nativeElement.querySelector('[data-testid="photo-input"]');
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('crée le composant et affiche la zone d’upload', () => {
    expect(fixture.componentInstance).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.upload-zone')).toBeTruthy();
    expect(input).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('0 photo sélectionnée');
  });

  it('importe une image valide et affiche son aperçu', () => {
    selectFiles([new File(['photo'], 'plage.jpg', { type: 'image/jpeg' })]);

    expect(fixture.nativeElement.textContent).toContain('1 photo sélectionnée');
    expect(fixture.nativeElement.textContent).toContain('plage.jpg');
    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('.upload-zone--ready')).toBeTruthy();
  });

  it('importe plusieurs formats d’image valides', () => {
    selectFiles([
      new File(['photo-1'], 'plage.jpg', { type: 'image/jpeg' }),
      new File(['photo-2'], 'montagne.png', { type: 'image/png' }),
      new File(['photo-3'], 'ville.webp', { type: 'image/webp' }),
    ]);

    expect(fixture.nativeElement.textContent).toContain('3 photos sélectionnées');
    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(3);
  });

  it('refuse un fichier non image avec un message explicite', () => {
    selectFiles([new File(['document'], 'voyage.pdf', { type: 'application/pdf' })]);

    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(0);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'seuls les fichiers images sont acceptés',
    );
  });

  it('refuse une image supérieure à 10 Mo', () => {
    const oversizedImage = new File(['photo'], 'panorama.jpg', { type: 'image/jpeg' });
    Object.defineProperty(oversizedImage, 'size', {
      value: UploadPageComponent.MAX_FILE_SIZE + 1,
    });

    selectFiles([oversizedImage]);

    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(0);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'dépasse la limite de 10 Mo',
    );
  });

  it('retire une image sélectionnée', () => {
    selectFiles([new File(['photo'], 'plage.jpg', { type: 'image/jpeg' })]);

    (fixture.nativeElement.querySelector('[aria-label="Retirer plage.jpg"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(0);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:plage.jpg');
  });

  function selectFiles(files: File[]): void {
    Object.defineProperty(input, 'files', {
      configurable: true,
      value: files,
    });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }
});
