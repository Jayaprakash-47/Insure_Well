import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { CustomerDashboardComponent } from './customer-dashboard.component';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { ChangeDetectorRef } from '@angular/core';
import { of, throwError } from 'rxjs';
import { RouterTestingModule } from '@angular/router/testing';
import { PolicyResponse, ClaimResponse, PaymentResponse } from '../../../core/models/models';

describe('CustomerDashboardComponent', () => {
  let component: CustomerDashboardComponent;
  let fixture: ComponentFixture<CustomerDashboardComponent>;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    const mockApi = jasmine.createSpyObj('ApiService', ['getMyPolicies', 'getMyClaims', 'getMyPayments', 'getProfile']);
    // Setup default mock returns
    mockApi.getMyPolicies.and.returnValue(of([{ policyId: 1, policyStatus: 'ACTIVE', coverageAmount: 100000, endDate: new Date(Date.now() + 86400000 * 10).toISOString() }]));
    mockApi.getMyClaims.and.returnValue(of([{ claimId: 1, policyId: 1, claimStatus: 'APPROVED', approvedAmount: 5000, claimAmount: 5000 }]));
    mockApi.getMyPayments.and.returnValue(of([{ paymentId: 1, policyNumber: 'POL-1', paymentStatus: 'SUCCESS', amount: 1500 }]));
    mockApi.getProfile.and.returnValue(of({ firstName: 'Test', email: 'test@test.com' }));
    
    // Stub out the api property array access used in fetch
    (mockApi as any).api = 'http://localhost:8080/api';

    const mockAuth = jasmine.createSpyObj('AuthService', ['getUserName', 'getUser']);
    // Return a dummy user to prevent Chatbot from crashing on init
    mockAuth.getUser.and.returnValue({ firstName: 'Test', email: 'test@test.com' });

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, CustomerDashboardComponent],
      providers: [
        { provide: ApiService, useValue: mockApi },
        { provide: AuthService, useValue: mockAuth },
        ChangeDetectorRef
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerDashboardComponent);
    component = fixture.componentInstance;
    apiSpy = TestBed.inject(ApiService) as jasmine.SpyObj<ApiService>;
    authSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  it('should create and load data on init', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
    expect(component.policies.length).toBe(1);
    expect(component.claims.length).toBe(1);
    expect(component.payments.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  describe('Getters and Statistics', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should calculate active policies', () => {
      expect(component.activePolicies).toBe(1);
      component.policies.push({ policyStatus: 'PENDING' } as any);
      expect(component.pendingPolicies).toBe(1);
      expect(component.concernPolicies.length).toBe(0);
    });

    it('should calculate claims metrics', () => {
      expect(component.totalClaimed).toBe(5000); // 1 claim APPROVED 5000
      expect(component.totalCoverage).toBe(100000);
      expect(component.totalRemaining).toBe(95000);
      expect(component.coverageUsedPct).toBe(5);
      expect(component.coverageRemainingPct).toBe(95);
      expect(component.totalClaimedAmount).toBe(5000);
      expect(component.totalApprovedAmount).toBe(5000);
      expect(component.totalSettledAmount).toBe(0); // settlement is undefined in mock
    });

    it('should calculate claim breakdowns', () => {
      const breakdown = component.claimsBreakdown;
      expect(breakdown.length).toBeGreaterThan(0);
      expect(breakdown.find((b: any) => b.key === 'APPROVED').count).toBe(1);
    });

    it('should calculate payments metrics', () => {
      expect(component.totalAmountPaid).toBe(1500);
      expect(component.totalSuccessfulPayments).toBe(1);
      expect(component.uniquePolicyNumbers).toEqual(['POL-1']);
      expect(component.invoicePendingCount).toBe(0);
    });

    it('should identify policies expiring soon and NCB', () => {
      expect(component.expiringSoon.length).toBe(1); // 10 days out
      component.policies.push({ policyStatus: 'ACTIVE', noClaimBonus: 500 } as any);
      expect(component.ncbPolicies.length).toBe(1);
    });
  });

  describe('Filters', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should filter payments correctly', () => {
      component.paymentFilter = 'POL-1';
      expect(component.filteredPayments.length).toBe(1);
      component.paymentFilter = 'POL-NONE';
      expect(component.filteredPayments.length).toBe(0);
    });

    it('should filter invoices by status and search', () => {
      component.invoiceStatusFilter = 'FAILED';
      expect(component.filteredInvoices.length).toBe(0);
      component.invoiceStatusFilter = 'ALL';
      component.invoiceSearch = 'pol-1';
      expect(component.filteredInvoices.length).toBe(1);
    });
  });

  describe('UI Formatters', () => {
    it('should return correct payment method icons', () => {
      expect(component.getPaymentMethodIcon('RAZORPAY')).toBe('💳');
      expect(component.getPaymentMethodIcon('UPI')).toBe('📱');
      expect(component.getPaymentMethodIcon('UNKNOWN')).toBe('💰');
    });

    it('should format currency correctly', () => {
      expect(component.formatCurrency(5000)).toContain('5,000');
    });

    it('should return correct policy status colors', () => {
      expect(component.getPolicyStatusColor('ACTIVE')).toBe('#10b981');
      expect(component.getPolicyStatusColor('CANCELLED')).toBe('#ef4444');
    });

    it('should return correct policy steps sequence', () => {
      const steps = component.getPolicySteps({ policyStatus: 'ACTIVE' } as any);
      const activeStep = steps.find(s => s.state === 'active');
      expect(activeStep?.key).toBe('ACTIVE');
    });
  });

  describe('Error Handling on Init', () => {
    it('should handle API errors gracefully', () => {
      apiSpy.getMyPolicies.and.returnValue(throwError(() => new Error('API down')));
      apiSpy.getMyClaims.and.returnValue(throwError(() => new Error('API down')));
      fixture.detectChanges();
      expect(component.loading).toBeFalse();
      expect(component.policies.length).toBe(0);
    });
  });

  describe('Download Receipts and Invoices (Fetch mock)', () => {
    it('should trigger browser download logic', async () => {
      const mockPayment: PaymentResponse = { paymentId: 99, transactionId: 'TX123' } as any;
      spyOn(window, 'fetch').and.returnValue(Promise.resolve(new Response(new Blob())));
      spyOn(URL, 'createObjectURL').and.returnValue('blob:fake');
      spyOn(URL, 'revokeObjectURL');
      spyOn(localStorage, 'getItem').and.returnValue('token123');
      const clickSpy = spyOn(HTMLAnchorElement.prototype, 'click');

      component.downloadReceipt(mockPayment);
      
      // Allow promises to resolve naturally
      await new Promise(resolve => setTimeout(resolve, 10));

      expect(window.fetch).toHaveBeenCalled();
      expect(clickSpy).toHaveBeenCalled();
      expect(component.downloadingReceipt).toBeNull();
    });
  });
});
