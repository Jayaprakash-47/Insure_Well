import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
    DashboardResponse, CreateUnderwriterRequest, AuthResponse,
    InsurancePlan, InsurancePlanRequest,
    PolicyResponse, ClaimRequest, ClaimResponse, ClaimStatusUpdateRequest,
    PremiumCalculateRequest, PremiumQuoteResponse,
    PolicyPurchaseRequest, PaymentRequest, PaymentResponse,
    CreateClaimsOfficerRequest, ClaimsOfficerDashboardResponse,
    ClaimReviewRequest, UnderwriterDashboardResponse,
    PolicyRenewalRequest
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
    private api = environment.apiUrl;

    constructor(private http: HttpClient) { }


    getAdminDashboard(): Observable<DashboardResponse> {
        return this.http.get<DashboardResponse>(`${this.api}/admin/dashboard`);
    }

    getAllCustomers(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/customers`);
    }

    getAllUnderwriters(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/underwriters`);
    }

    getAllClaimsOfficers(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/claims-officers`);
    }

    createUnderwriter(req: CreateUnderwriterRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.api}/admin/create-underwriter`, req);
    }

    createClaimsOfficer(req: CreateClaimsOfficerRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.api}/admin/create-claims-officer`, req);
    }

    deactivateUser(id: number): Observable<any> {
        return this.http.patch(`${this.api}/admin/users/${id}/deactivate`, {});
    }

    activateUser(id: number): Observable<any> {
        return this.http.patch(`${this.api}/admin/users/${id}/activate`, {});
    }


    settleClaim(id: number): Observable<ClaimResponse> {
        return this.http.post<ClaimResponse>(`${this.api}/admin/claims/${id}/settle`, {});
    }

    getUnderwriterPerformance(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/underwriter-performance`);
    }


    // ====== ADMIN ASSIGNMENT ======
    getPendingPolicyApplications(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/pending-applications`);
    }

    assignUnderwriter(policyId: number, req: { underwriterId: number }): Observable<any> {
        return this.http.post(`${this.api}/admin/policies/${policyId}/assign-underwriter`, req);
    }

    getSubmittedClaims(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/submitted-claims`);
    }

    assignClaimsOfficer(claimId: number, req: { claimsOfficerId: number }): Observable<any> {
        return this.http.post(`${this.api}/admin/claims/${claimId}/assign-officer`, req);
    }

    // ====== ADMIN PLAN MANAGEMENT ======
    createPlan(req: InsurancePlanRequest): Observable<InsurancePlan> {
        return this.http.post<InsurancePlan>(`${this.api}/admin/plans`, req);
    }

    updatePlan(id: number, req: InsurancePlanRequest): Observable<InsurancePlan> {
        return this.http.put<InsurancePlan>(`${this.api}/admin/plans/${id}`, req);
    }

    deactivatePlan(id: number): Observable<any> {
        return this.http.patch(`${this.api}/admin/plans/${id}/deactivate`, {});
    }

    activatePlan(id: number): Observable<any> {
        return this.http.patch(`${this.api}/admin/plans/${id}/activate`, {});
    }

    // ====== PLANS (PUBLIC) ======
    getAllPlans(): Observable<InsurancePlan[]> {
        return this.http.get<InsurancePlan[]>(`${this.api}/plans`);
    }

    getPlanById(id: number): Observable<InsurancePlan> {
        return this.http.get<InsurancePlan>(`${this.api}/plans/${id}`);
    }

    // ====== PREMIUM QUOTES ======
    calculatePremium(req: PremiumCalculateRequest): Observable<PremiumQuoteResponse> {
        return this.http.post<PremiumQuoteResponse>(`${this.api}/premium/calculate/me`, req);
    }

    calculatePremiumGuest(req: PremiumCalculateRequest): Observable<PremiumQuoteResponse> {
        return this.http.post<PremiumQuoteResponse>(`${this.api}/premium/calculate`, req);
    }

    getMyQuotes(): Observable<PremiumQuoteResponse[]> {
        return this.http.get<PremiumQuoteResponse[]>(`${this.api}/premium/my-quotes`);
    }

    // ====== POLICIES ======
    purchasePolicy(req: PolicyPurchaseRequest): Observable<PolicyResponse> {
        return this.http.post<PolicyResponse>(`${this.api}/policies`, req);
    }

    getMyPolicies(): Observable<PolicyResponse[]> {
        return this.http.get<PolicyResponse[]>(`${this.api}/policies/my-policies`);
    }

    getAllPolicies(): Observable<PolicyResponse[]> {
        return this.http.get<PolicyResponse[]>(`${this.api}/policies`);
    }

    getPolicyById(id: number): Observable<PolicyResponse> {
        return this.http.get<PolicyResponse>(`${this.api}/policies/${id}`);
    }

    cancelPolicy(id: number): Observable<PolicyResponse> {
        return this.http.patch<PolicyResponse>(`${this.api}/policies/${id}/cancel`, {});
    }

    renewPolicy(id: number, req: PolicyRenewalRequest): Observable<PolicyResponse> {
        return this.http.post<PolicyResponse>(`${this.api}/policies/${id}/renew`, req);
    }

    // ====== CLAIMS ======
    fileClaim(req: FormData): Observable<ClaimResponse> {
        return this.http.post<ClaimResponse>(`${this.api}/claims`, req);
    }

    getMyClaims(): Observable<ClaimResponse[]> {
        return this.http.get<ClaimResponse[]>(`${this.api}/claims/my-claims`);
    }

    getAllClaims(): Observable<ClaimResponse[]> {
        return this.http.get<ClaimResponse[]>(`${this.api}/claims`);
    }

    getPendingClaims(): Observable<ClaimResponse[]> {
        return this.http.get<ClaimResponse[]>(`${this.api}/claims/pending`);
    }

    getClaimsByStatus(status: string): Observable<ClaimResponse[]> {
        return this.http.get<ClaimResponse[]>(`${this.api}/claims/status/${status}`);
    }

    updateClaimStatus(id: number, req: ClaimStatusUpdateRequest): Observable<ClaimResponse> {
        return this.http.patch<ClaimResponse>(`${this.api}/claims/${id}/status`, req);
    }

    // ====== PAYMENTS ======
    makePayment(req: PaymentRequest): Observable<PaymentResponse> {
        return this.http.post<PaymentResponse>(`${this.api}/payments`, req);
    }

    getMyPayments(): Observable<PaymentResponse[]> {
        return this.http.get<PaymentResponse[]>(`${this.api}/payments/my-payments`);
    }

    // ====== UNDERWRITER ======
    getUnderwriterDashboard(): Observable<UnderwriterDashboardResponse> {
        return this.http.get<UnderwriterDashboardResponse>(`${this.api}/underwriter/dashboard`);
    }

    getUnderwriterProfile(): Observable<UnderwriterDashboardResponse> {
        return this.http.get<UnderwriterDashboardResponse>(`${this.api}/underwriter/profile`);
    }

    getUnderwriterPlans(): Observable<InsurancePlan[]> {
        return this.http.get<InsurancePlan[]>(`${this.api}/underwriter/plans`);
    }

    getUnderwriterPendingAssignments(): Observable<PolicyResponse[]> {
        return this.http.get<PolicyResponse[]>(`${this.api}/underwriter/pending-assignments`);
    }

    getUnderwriterMyPolicies(): Observable<PolicyResponse[]> {
        return this.http.get<PolicyResponse[]>(`${this.api}/underwriter/my-policies`);
    }

    sendQuote(policyId: number, req: { quoteAmount: number; remarks?: string }): Observable<PolicyResponse> {
        return this.http.post<PolicyResponse>(`${this.api}/underwriter/policy/${policyId}/send-quote`, req);
    }

    calculateUnderwriterQuote(policyId: number): Observable<{ quoteAmount: number }> {
        return this.http.get<{ quoteAmount: number }>(`${this.api}/underwriter/policy/${policyId}/calculate-quote`);
    }

    // ====== CLAIMS OFFICER ======
    getOfficerDashboard(): Observable<ClaimsOfficerDashboardResponse> {
        return this.http.get<ClaimsOfficerDashboardResponse>(`${this.api}/claims-officer/dashboard`);
    }

    getOfficerAssignedClaims(): Observable<ClaimResponse[]> {
        return this.http.get<ClaimResponse[]>(`${this.api}/claims-officer/my-claims`);
    }

    getOfficerClaimsByStatus(status: string): Observable<ClaimResponse[]> {
        return this.http.get<ClaimResponse[]>(`${this.api}/claims-officer/my-claims/status/${status}`);
    }

    getOfficerDecisionHistory(): Observable<ClaimResponse[]> {
        return this.http.get<ClaimResponse[]>(`${this.api}/claims-officer/my-decisions`);
    }

    getOfficerClaimDetail(claimId: number): Observable<ClaimResponse> {
        return this.http.get<ClaimResponse>(`${this.api}/claims-officer/claim/${claimId}`);
    }

    reviewClaim(claimId: number, req: ClaimReviewRequest): Observable<ClaimResponse> {
        return this.http.post<ClaimResponse>(`${this.api}/claims-officer/claim/${claimId}/review`, req);
    }
}
