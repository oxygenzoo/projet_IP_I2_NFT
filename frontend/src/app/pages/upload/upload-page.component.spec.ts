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

  it('importe une image', () => {
    selectFiles([new File(['photo'], 'plage.jpg', { type: 'image/jpeg' })]);

    expect(fixture.nativeElement.textContent).toContain('1 photo sélectionnée');
    expect(fixture.nativeElement.textContent).toContain('plage.jpg');
    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(1);
  });

  it('importe plusieurs images', () => {
    selectFiles([
      new File(['photo-1'], 'plage.jpg', { type: 'image/jpeg' }),
      new File(['photo-2'], 'montagne.png', { type: 'image/png' }),
      new File(['photo-3'], 'ville.webp', { type: 'image/webp' }),
    ]);

    expect(fixture.nativeElement.textContent).toContain('3 photos sélectionnées');
    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(3);
  });

  it('refuse un fichier PDF', () => {
    selectFiles([new File(['document'], 'voyage.pdf', { type: 'application/pdf' })]);

    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(0);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'seuls les fichiers images sont acceptés',
    );
  });

  it('refuse une image trop lourde', () => {
    const oversizedImage = new File(['photo'], 'panorama.jpg', {
      type: 'image/jpeg',
    });
    Object.defineProperty(oversizedImage, 'size', {
      value: UploadPageComponent.MAX_FILE_SIZE + 1,
    });

    selectFiles([oversizedImage]);

    expect(fixture.nativeElement.querySelectorAll('.photo-preview')).toHaveLength(0);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'dépasse la limite de 10 Mo',
    );
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
