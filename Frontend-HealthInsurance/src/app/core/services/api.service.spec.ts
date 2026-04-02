import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';
import { environment } from '../../../environments/environment';
import { PolicyResponse } from '../models/models';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;
  const api = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);

    // Mock localStorage for functions relying on it
    spyOn(localStorage, 'getItem').and.callFake((key: string) => {
      if (key === 'hs_token') return 'fake-mock-token';
      return null;
    });
  });

  afterEach(() => {
    httpMock.verify(); // Ensure no outstanding requests remain
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ====== ADMIN API CALLS ======
  it('should get admin dashboard data', () => {
    service.getAdminDashboard().subscribe();
    const req = httpMock.expectOne(`${api}/admin/dashboard`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('should get all customers', () => {
    service.getAllCustomers().subscribe();
    const req = httpMock.expectOne(`${api}/admin/customers`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should deactivate a user', () => {
    service.deactivateUser(123).subscribe();
    const req = httpMock.expectOne(`${api}/admin/users/123/deactivate`);
    expect(req.request.method).toBe('PATCH');
    req.flush({});
  });

  // ====== PLANS API CALLS ======
  it('should get all plans', () => {
    service.getAllPlans().subscribe();
    const req = httpMock.expectOne(`${api}/plans`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should create a plan', () => {
    service.createPlan({ name: 'Test' } as any).subscribe();
    const req = httpMock.expectOne(`${api}/admin/plans`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  // ====== PREMIUM QUOTES ======
  it('should calculate premium', () => {
    service.calculatePremium({ age: 30 } as any).subscribe();
    const req = httpMock.expectOne(`${api}/premium/calculate/me`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  // ====== POLICY CALLS ======
  it('should purchase a policy', () => {
    service.purchasePolicy({ planId: 1 } as any).subscribe();
    const req = httpMock.expectOne(`${api}/policies`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should get ALL policies', () => {
    service.getAllPolicies().subscribe();
    const req = httpMock.expectOne(`${api}/policies`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should renew policy', () => {
    service.renewPolicy(123, {}).subscribe();
    const req = httpMock.expectOne(`${api}/policies/123/renew`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  // ====== COMPLEX OBSERVABLE MAPPING (BLOB) ======
  it('should format and download policy document properly', () => {
    service.getPolicyDocument(99).subscribe((blob) => {
      expect(blob instanceof Blob).toBeTrue();
      expect(blob.type).toBe('application/pdf');
    });

    const req = httpMock.expectOne(`${api}/policies/99/document/download`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['test-pdf']), { headers: { 'Content-Type': 'application/pdf' } });
  });

  it('should format and download Aadhaar document properly', () => {
    service.getAadhaarDocument(99).subscribe((blob) => {
      expect(blob instanceof Blob).toBeTrue();
      expect(blob.type).toBe('image/jpeg');
    });

    const req = httpMock.expectOne(`${api}/policies/99/aadhaar/download`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['test-img']), { headers: { 'Content-Type': 'image/jpeg' } });
  });

  // ====== CLAIMS ======
  it('should get pending claims', () => {
    service.getPendingClaims().subscribe();
    const req = httpMock.expectOne(`${api}/claims/pending`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should file a claim', () => {
    const formData = new FormData();
    service.fileClaim(formData).subscribe();
    const req = httpMock.expectOne(`${api}/claims`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  // ====== RAZORPAY ======
  it('should verify Razorpay payment', () => {
    service.verifyPayment({ policyId: '1', razorpayOrderId: 'ord_1', razorpayPaymentId: 'pay_1', razorpaySignature: 'sig' }).subscribe();
    const req = httpMock.expectOne(`${api}/razorpay/verify`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  // ====== KYC & CHAT ======
  it('should initiate KYC', () => {
    service.initiateKyc('123456789012').subscribe();
    const req = httpMock.expectOne(`${api}/kyc/initiate`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should verify KYC OTP', () => {
    service.verifyKycOtp('tx-123', '123456').subscribe();
    const req = httpMock.expectOne(`${api}/kyc/verify-otp`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should send a chat message', () => {
    service.sendChatMessage('Hello', []).subscribe();
    const req = httpMock.expectOne(`${api}/chat`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  // ====== DOCUMENT DOWNLOAD (FETCH MOCKS) ======
  it('should trigger browser download for a document', (done) => {
    // Mock the global native fetch specifically for this test
    spyOn(window, 'fetch').and.returnValue(Promise.resolve(new Response(new Blob())));
    spyOn(URL, 'createObjectURL').and.returnValue('blob:fake-url');
    spyOn(URL, 'revokeObjectURL');
    
    const clickSpy = spyOn(HTMLAnchorElement.prototype, 'click');

    service.downloadDocument(11, 'doc.pdf');
    
    setTimeout(() => {
      expect(window.fetch).toHaveBeenCalledWith(
        `${api}/claims/11/documents/download/doc.pdf`,
        jasmine.objectContaining({ headers: { Authorization: `Bearer fake-mock-token` } })
      );
      expect(clickSpy).toHaveBeenCalled();
      done();
    }, 100);
  });
});
