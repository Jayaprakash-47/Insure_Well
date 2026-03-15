import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { SendQuoteComponent } from './send-quote.component';
import { ApiService } from '../../../core/services/api.service';
import { PolicyResponse } from '../../../core/models/models';
import { of, throwError } from 'rxjs';

const mockMembers = [
  {
    memberId: 1,
    memberName: 'Alice',
    relationship: 'SELF',
    dateOfBirth: '1990-01-01',
    gender: 'FEMALE',
    preExistingDiseases: 'Diabetes',
  },
];

const mockPolicy: PolicyResponse = {
  policyId: 1,
  policyNumber: 'POL-001',
  customerId: 1,
  customerName: 'Alice',
  planId: 1,
  planName: 'Health Plus',
  premiumAmount: 10000,
  coverageAmount: 500000,
  remainingCoverage: 500000,
  totalClaimedAmount: 0,
  startDate: '',
  endDate: '',
  policyStatus: 'ASSIGNED',
  nomineeName: 'Bob',
  nomineeRelationship: 'Spouse',
  createdAt: '',
  members: mockMembers,
  underwriterId: 2,
  underwriterName: 'UW1',
  commissionAmount: 1000,
  quoteAmount: 0,
  waitingPeriodMonths: 3,
  assignedAt: '',
};

describe('SendQuoteComponent', () => {
  let fixture: ComponentFixture<SendQuoteComponent>;
  let component: SendQuoteComponent;
  let apiSpy: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    apiSpy = jasmine.createSpyObj('ApiService', ['getPolicyById', 'calculateQuote', 'sendQuote']);
    apiSpy.getPolicyById.and.returnValue(of(mockPolicy));
    apiSpy.calculateQuote.and.returnValue(of({ calculatedQuote: 15000 }));

    await TestBed.configureTestingModule({
      imports: [SendQuoteComponent, RouterTestingModule],
      providers: [
        { provide: ApiService, useValue: apiSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SendQuoteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // TC-96
  it('should create and load policy on init', () => {
    expect(component).toBeTruthy();
    expect(component.policy).toEqual(mockPolicy);
  });

  // TC-97
  it('estimatedCommission: should compute 10% of quoteAmount', () => {
    component.quoteAmount = 20000;
    expect(component.estimatedCommission).toBe(2000);
  });

  // TC-98
  it('getMaxAge: should return age of oldest member', () => {
    component.policy = mockPolicy;
    const age = component.getMaxAge();
    expect(age).toBeGreaterThan(0);
  });

  // TC-99
  it('getAgeFactor: should return 1.0 for age <= 30', () => {
    spyOn(component, 'getMaxAge').and.returnValue(25);
    expect(component.getAgeFactor()).toBe(1.0);
  });

  // TC-100
  it('getAgeFactor: should return 1.5 for age between 41 and 50', () => {
    spyOn(component, 'getMaxAge').and.returnValue(45);
    expect(component.getAgeFactor()).toBe(1.5);
  });

  // TC-101
  it('getDiseaseFactor: should return 1.3 when pre-existing diseases exist', () => {
    component.policy = mockPolicy; // Alice has Diabetes
    expect(component.getDiseaseFactor()).toBe(1.3);
  });

  // TC-102
  it('hasPreExistingDiseases: should return true for member with disease', () => {
    component.policy = mockPolicy;
    expect(component.hasPreExistingDiseases()).toBeTrue();
  });

  // TC-103
  it('hasPreExistingDiseases: should return false when all members have "none"', () => {
    component.policy = {
      ...mockPolicy,
      members: [{ ...mockMembers[0], preExistingDiseases: 'none' }],
    };
    expect(component.hasPreExistingDiseases()).toBeFalse();
  });

  // TC-104
  it('getMemberFactor: should return 1.0 for single member', () => {
    component.policy = { ...mockPolicy, members: [mockMembers[0]] };
    expect(component.getMemberFactor()).toBe(1.0);
  });

  // TC-105
  it('getMemberFactor: should increase for more members', () => {
    component.policy = {
      ...mockPolicy,
      members: [mockMembers[0], { ...mockMembers[0], memberId: 2, memberName: 'Bob' }],
    };
    expect(component.getMemberFactor()).toBe(1.7);
  });

  // TC-106
  it('getOverallRiskScore: should return a value between 0 and 100', () => {
    component.policy = mockPolicy;
    const score = component.getOverallRiskScore();
    expect(score).toBeGreaterThanOrEqual(0);
    expect(score).toBeLessThanOrEqual(100);
  });

  // TC-107
  it('useCalculatedQuote: should set quoteAmount to calculatedQuote', () => {
    component.calculatedQuote = 18000;
    component.useCalculatedQuote();
    expect(component.quoteAmount).toBe(18000);
  });

  // TC-108
  it('submitQuote: should call api.sendQuote with correct data', () => {
    apiSpy.sendQuote.and.returnValue(of(true as any));
    component.quoteAmount = 15000;
    component.remarks = 'Good profile';
    component.submitQuote();
    expect(apiSpy.sendQuote).toHaveBeenCalledWith(1, {
      quoteAmount: 15000,
      remarks: 'Good profile',
    });
  });

  // TC-109
  it('submitQuote: should set success=true on success', () => {
    apiSpy.sendQuote.and.returnValue(of(true as any));
    component.quoteAmount = 15000;
    component.submitQuote();
    expect(component.success).toBeTrue();
  });

  // TC-110
  it('submitQuote: should set error message on failure', () => {
    apiSpy.sendQuote.and.returnValue(throwError(() => ({ error: { message: 'Quote failed' } })));
    component.quoteAmount = 15000;
    component.submitQuote();
    expect(component.error).toBe('Quote failed');
  });
});
