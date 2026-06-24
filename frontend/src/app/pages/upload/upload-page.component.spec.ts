import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { UploadPageComponent } from './upload-page.component';

describe('UploadPageComponent', () => {
  let fixture: ComponentFixture<UploadPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadPageComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadPageComponent);
    fixture.detectChanges();
  });

  function selectFiles(files: File[]): void {
    const input = fixture.nativeElement.querySelector('input[type="file"]') as HTMLInputElement;
    Object.defineProperty(input, 'files', { configurable: true, value: files });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  it('creates the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the upload zone and file input', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('.upload-zone')).toBeTruthy();
    expect(element.querySelector('input[type="file"]')).toBeTruthy();
    expect(element.querySelector('button')?.textContent).toContain('Importer mes photos');
    expect(element.textContent).toContain('0 photo selectionnee');
  });

  it('opens the file picker when the import button is clicked', () => {
    const input = fixture.nativeElement.querySelector('input[type="file"]') as HTMLInputElement;
    const clickSpy = vi.spyOn(input, 'click');

    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();

    expect(clickSpy).toHaveBeenCalledOnce();
  });

  it('accepts valid image files and displays their names', () => {
    selectFiles([
      new File(['photo-one'], 'bali.jpg', { type: 'image/jpeg' }),
      new File(['photo-two'], 'temple.png', { type: 'image/png' }),
    ]);

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.upload-zone--ready')).toBeTruthy();
    expect(element.textContent).toContain('2 photos selectionnees');
    expect(element.textContent).toContain('bali.jpg, temple.png');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('rejects unsupported files and displays an explicit error', () => {
    selectFiles([new File(['document'], 'notes.pdf', { type: 'application/pdf' })]);

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alert.textContent).toContain('Format non pris en charge');
    expect(fixture.nativeElement.textContent).toContain('0 photo selectionnee');
  });

  it('rejects images larger than 10 MB', () => {
    const largeFile = new File(['image'], 'large.jpg', { type: 'image/jpeg' });
    Object.defineProperty(largeFile, 'size', { value: 10 * 1024 * 1024 + 1 });

    selectFiles([largeFile]);

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alert.textContent).toContain('moins de 10 Mo');
  });
});
