import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
    DashboardResponse, CreateAgentRequest, AuthResponse,
    InsurancePlan, InsurancePlanRequest,
    PolicyResponse, ClaimRequest, ClaimResponse, ClaimStatusUpdateRequest,
    PremiumCalculateRequest, PremiumQuoteResponse,
    PolicyPurchaseRequest, PaymentRequest, PaymentResponse,
    CreateClaimsOfficerRequest, ClaimsOfficerDashboardResponse,
    ClaimReviewRequest, AgentSellPolicyRequest, AgentDashboardResponse,
    AdminClaimDecisionRequest, AuditLogResponse, PolicyRenewalRequest
} from '../models/models';

@Injectable({ providedIn: 'root' })
export class ApiService {
    private api = environment.apiUrl;

    constructor(private http: HttpClient) { }

    // ====== ADMIN ======
    getAdminDashboard(): Observable<DashboardResponse> {
        return this.http.get<DashboardResponse>(`${this.api}/admin/dashboard`);
    }

    getAllCustomers(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/customers`);
    }

    getAllAgents(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/agents`);
    }

    getAllClaimsOfficers(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/claims-officers`);
    }

    createAgent(req: CreateAgentRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.api}/admin/create-agent`, req);
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

    getEscalatedClaims(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/escalated-claims`);
    }

    resolveEscalatedClaim(id: number, req: AdminClaimDecisionRequest): Observable<any> {
        return this.http.post(`${this.api}/admin/escalated-claims/${id}/resolve`, req);
    }

    settleClaim(id: number): Observable<ClaimResponse> {
        return this.http.post<ClaimResponse>(`${this.api}/admin/claims/${id}/settle`, {});
    }

    getAgentPerformance(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/admin/agent-performance`);
    }

    getAuditLogs(): Observable<AuditLogResponse[]> {
        return this.http.get<AuditLogResponse[]>(`${this.api}/admin/audit-logs`);
    }

    getAuditTrail(entityType: string, entityId: number): Observable<AuditLogResponse[]> {
        return this.http.get<AuditLogResponse[]>(`${this.api}/admin/audit-logs/entity/${entityType}/${entityId}`);
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

    // ====== AGENT ======
    getAgentDashboard(): Observable<AgentDashboardResponse> {
        return this.http.get<AgentDashboardResponse>(`${this.api}/agent/dashboard`);
    }

    getAgentProfile(): Observable<AgentDashboardResponse> {
        return this.http.get<AgentDashboardResponse>(`${this.api}/agent/profile`);
    }

    getAgentPlans(): Observable<InsurancePlan[]> {
        return this.http.get<InsurancePlan[]>(`${this.api}/agent/plans`);
    }

    getAgentPolicies(): Observable<PolicyResponse[]> {
        return this.http.get<PolicyResponse[]>(`${this.api}/agent/policies`);
    }

    getAgentCustomers(): Observable<any[]> {
        return this.http.get<any[]>(`${this.api}/agent/customers`);
    }

    getAgentMyPolicies(): Observable<PolicyResponse[]> {
        return this.http.get<PolicyResponse[]>(`${this.api}/agent/my-policies`);
    }

    agentSellPolicy(req: AgentSellPolicyRequest): Observable<PolicyResponse> {
        return this.http.post<PolicyResponse>(`${this.api}/agent/sell-policy`, req);
    }

    // ====== CLAIMS OFFICER ======
    getOfficerDashboard(): Observable<ClaimsOfficerDashboardResponse> {
        return this.http.get<ClaimsOfficerDashboardResponse>(`${this.api}/claims-officer/dashboard`);
    }

    getClaimQueue(): Observable<ClaimResponse[]> {
        return this.http.get<ClaimResponse[]>(`${this.api}/claims-officer/queue`);
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

    pickupClaim(claimId: number): Observable<ClaimResponse> {
        return this.http.post<ClaimResponse>(`${this.api}/claims-officer/claim/${claimId}/pickup`, {});
    }

    getOfficerClaimDetail(claimId: number): Observable<ClaimResponse> {
        return this.http.get<ClaimResponse>(`${this.api}/claims-officer/claim/${claimId}`);
    }

    reviewClaim(claimId: number, req: ClaimReviewRequest): Observable<ClaimResponse> {
        return this.http.post<ClaimResponse>(`${this.api}/claims-officer/claim/${claimId}/review`, req);
    }
}
