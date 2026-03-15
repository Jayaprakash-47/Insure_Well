import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { ClaimReviewComponent } from './claim-review.component';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { ClaimResponse } from '../../../core/models/models';
import { of, throwError } from 'rxjs';

const mockClaim: ClaimResponse = {
  claimId: 1,
  claimNumber: 'CLM-001',
  policyId: 10,
  policyNumber: 'POL-001',
  customerId: 5,
  customerName: 'Alice',
  claimType: 'CASHLESS',
  claimAmount: 50000,
  approvedAmount: 0,
  settlementAmount: 0,
  hospitalName: 'City Hospital',
  admissionDate: '2025-01-01',
  dischargeDate: '2025-01-05',
  diagnosis: 'Fever',
  claimStatus: 'UNDER_REVIEW',
  rejectionReason: '',
  createdAt: '2025-01-01',
  documents: [],
  assignedOfficerId: 2,
  assignedOfficerName: 'Bob',
  reviewStartedAt: '2025-01-02',
  reviewedAt: '',
  reviewerRemarks: '',
  adminRemarks: '',
  settlementDate: '',
  tpaReferenceNumber: '',
};

describe('ClaimReviewComponent', () => {
  let fixture: ComponentFixture<ClaimReviewComponent>;
  let component: ClaimReviewComponent;
  let apiSpy: jasmine.SpyObj<ApiService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', [
      'getOfficerClaimDetail',
      'reviewClaim',
      'downloadDocument',
    ]);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    apiSpy.getOfficerClaimDetail.and.returnValue(of(mockClaim));

    await TestBed.configureTestingModule({
      imports: [ClaimReviewComponent, RouterTestingModule, HttpClientTestingModule],
      providers: [
        { provide: ApiService, useValue: apiSpy },
        { provide: ToastService, useValue: toastSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClaimReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // TC-83
  it('should create and load claim on init', () => {
    expect(component).toBeTruthy();
    expect(component.claim).toEqual(mockClaim);
  });

  // TC-84
  it('canReview: should be true when status is UNDER_REVIEW', () => {
    component.claim = { ...mockClaim, claimStatus: 'UNDER_REVIEW' };
    expect(component.canReview).toBeTrue();
  });

  // TC-85
  it('canReview: should be true when status is DOCUMENT_PENDING', () => {
    component.claim = { ...mockClaim, claimStatus: 'DOCUMENT_PENDING' };
    expect(component.canReview).toBeTrue();
  });

  // TC-86
  it('canReview: should be false when status is APPROVED', () => {
    component.claim = { ...mockClaim, claimStatus: 'APPROVED' };
    expect(component.canReview).toBeFalse();
  });

  // TC-87
  it('submitReview: should show error when no decision selected', () => {
    component.decision = '';
    component.submitReview();
    expect(toastSpy.error).toHaveBeenCalledWith('Please select a decision');
  });

  // TC-88
  it('submitReview: should show error when APPROVED without amount', () => {
    component.decision = 'APPROVED';
    component.approvedAmount = null;
    component.submitReview();
    expect(toastSpy.error).toHaveBeenCalledWith('Please enter a valid approved amount');
  });

  // TC-89
  it('submitReview: should show error when REJECTED without reason', () => {
    component.decision = 'REJECTED';
    component.rejectionReason = '';
    component.submitReview();
    expect(toastSpy.error).toHaveBeenCalledWith('Please provide a rejection reason');
  });

  // TC-90
  it('submitReview: should call api.reviewClaim on valid APPROVED decision', () => {
    apiSpy.reviewClaim.and.returnValue(
      of({ ...mockClaim, claimStatus: 'APPROVED', claimNumber: 'CLM-001' }),
    );
    component.decision = 'APPROVED';
    component.approvedAmount = 40000;
    component.submitReview();
    expect(apiSpy.reviewClaim).toHaveBeenCalled();
    expect(toastSpy.success).toHaveBeenCalled();
  });

  // TC-91
  it('setDecision: should set decision and pre-fill amount for APPROVED', () => {
    component.claim = { ...mockClaim, claimAmount: 50000 };
    component.setDecision('APPROVED');
    expect(component.decision).toBe('APPROVED');
    expect(component.approvedAmount).toBe(50000);
  });

  // TC-92
  it('setDecision: should set approvedAmount to null for PARTIALLY_APPROVED', () => {
    component.setDecision('PARTIALLY_APPROVED');
    expect(component.approvedAmount).toBeNull();
  });

  // TC-93
  it('formatCurrency: should format amount with ₹ sign', () => {
    expect(component.formatCurrency(50000)).toContain('₹');
  });

  // TC-94
  it('getStatusClass: should return badge-approved for APPROVED status', () => {
    expect(component.getStatusClass('APPROVED')).toBe('badge-approved');
  });

  // TC-95
  it('getStatusClass: should return badge-rejected for REJECTED status', () => {
    expect(component.getStatusClass('REJECTED')).toBe('badge-rejected');
  });
});
