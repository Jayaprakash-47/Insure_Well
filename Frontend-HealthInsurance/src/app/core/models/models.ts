// =================== AUTH ===================
export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phone: string;
    dateOfBirth: string;
    gender: string;
    address: string;
    city: string;
    state: string;
    pincode: string;
}

export interface AuthResponse {
    token: string;
    type: string;
    userId: number;
    firstName: string;
    email: string;
    role: string;
    message: string;
}

// =================== DASHBOARD ===================
export interface DashboardResponse {
    totalCustomers: number;
    totalAgents: number;
    totalClaimsOfficers: number;
    totalAdmins: number;
    totalPolicies: number;
    totalActivePolicies: number;
    totalPendingPolicies: number;
    totalExpiredPolicies: number;
    totalClaims: number;
    totalPendingClaims: number;
    totalUnderReviewClaims: number;
    totalApprovedClaims: number;
    totalRejectedClaims: number;
    totalEscalatedClaims: number;
    totalSettledClaims: number;
    totalPayments: number;
    totalRevenue: number;
    totalClaimsPaidOut: number;
    claimSettlementRatio: number;
    totalActivePlans: number;
    totalNetworkHospitals: number;
}

// =================== PLANS ===================
export interface InsurancePlan {
    planId: number;
    planName: string;
    planType: string;
    description: string;
    basePremiumAmount: number;
    coverageAmount: number;
    planDurationMonths: number;
    minAgeLimit: number;
    maxAgeLimit: number;
    waitingPeriodMonths: number;
    maternityCover: boolean;
    preExistingDiseaseCover: boolean;
    isActive: boolean;
    createdAt: string;
}

export interface InsurancePlanRequest {
    planName: string;
    planType: string;
    description: string;
    basePremiumAmount: number;
    coverageAmount: number;
    planDurationMonths: number;
    minAgeLimit: number;
    maxAgeLimit: number;
    waitingPeriodMonths: number;
    maternityCover: boolean;
    preExistingDiseaseCover: boolean;
}

// =================== PREMIUM QUOTES ===================
export interface PremiumCalculateRequest {
    planId: number;
    age: number;
    smoker: boolean;
    preExistingDiseases: boolean;
    numberOfMembers: number;
}

export interface PremiumQuoteResponse {
    quoteId: number;
    planName: string;
    age: number;
    smoker: boolean;
    preExistingDiseases: boolean;
    numberOfMembers: number;
    calculatedPremium: number;
    calculatedAt: string;
}

// =================== POLICIES ===================
export interface PolicyPurchaseRequest {
    planId: number;
    quoteId?: number;
    nomineeName: string;
    nomineeRelationship: string;
    members: PolicyMemberRequest[];
}

export interface PolicyMemberRequest {
    memberName: string;
    relationship: string;
    dateOfBirth: string;
    gender: string;
    preExistingDiseases: string;
}

export interface PolicyResponse {
    policyId: number;
    policyNumber: string;
    customerId: number;
    customerName: string;
    planId: number;
    planName: string;
    premiumAmount: number;
    coverageAmount: number;
    remainingCoverage: number;
    totalClaimedAmount: number;
    startDate: string;
    endDate: string;
    policyStatus: string;
    nomineeName: string;
    nomineeRelationship: string;
    createdAt: string;
    members: PolicyMemberResponse[];
    agentId: number;
    agentName: string;
    commissionAmount: number;
    renewalCount: number;
    noClaimBonus: number;
    originalPolicyId: number;
}

export interface PolicyMemberResponse {
    memberId: number;
    memberName: string;
    relationship: string;
    dateOfBirth: string;
    gender: string;
    preExistingDiseases: string;
}

export interface PolicyRenewalRequest {
    nomineeName?: string;
    nomineeRelationship?: string;
    quoteId?: number;
}

// =================== CLAIMS ===================
export interface ClaimRequest {
    policyId: number;
    claimType: string;
    claimAmount: number;
    hospitalName: string;
    admissionDate: string;
    dischargeDate: string;
    diagnosis: string;
}

export interface ClaimResponse {
    claimId: number;
    claimNumber: string;
    policyId: number;
    policyNumber: string;
    customerId: number;
    customerName: string;
    claimType: string;
    claimAmount: number;
    approvedAmount: number;
    settlementAmount: number;
    hospitalName: string;
    admissionDate: string;
    dischargeDate: string;
    diagnosis: string;
    claimStatus: string;
    rejectionReason: string;
    createdAt: string;
    documents: any[];
    assignedOfficerId: number;
    assignedOfficerName: string;
    reviewStartedAt: string;
    reviewedAt: string;
    reviewerRemarks: string;
    isEscalated: boolean;
    escalationReason: string;
    escalationNotes: string;
    escalatedAt: string;
    adminRemarks: string;
    escalationResolvedAt: string;
    settlementDate: string;
    tpaReferenceNumber: string;
}

export interface ClaimStatusUpdateRequest {
    status: string;
    approvedAmount?: number;
    rejectionReason?: string;
}

export interface ClaimReviewRequest {
    decision: string;
    approvedAmount?: number;
    reviewerRemarks?: string;
    rejectionReason?: string;
    escalationReason?: string;
    escalationNotes?: string;
    additionalDocumentsRequired?: string;
}

// =================== PAYMENTS ===================
export interface PaymentRequest {
    policyId: number;
    amount: number;
    paymentMethod: string;
}

export interface PaymentResponse {
    paymentId: number;
    policyId: number;
    policyNumber: string;
    customerId: number;
    customerName: string;
    amount: number;
    paymentDate: string;
    paymentMethod: string;
    transactionId: string;
    paymentStatus: string;
    message: string;
}

// =================== AGENT ===================
export interface CreateAgentRequest {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phone: string;
    licenseNumber: string;
    territory: string;
    commissionPercentage: number;
}

export interface AgentSellPolicyRequest {
    customerId: number;
    planId: number;
    nomineeName: string;
    nomineeRelationship: string;
    quoteId?: number;
    members?: PolicyMemberRequest[];
}

export interface AgentDashboardResponse {
    agentId: number;
    agentName: string;
    email: string;
    licenseNumber: string;
    territory: string;
    commissionPercentage: number;
    totalPoliciesSold: number;
    totalCommissionEarned: number;
    totalCustomersServed: number;
    activePolicies: number;
    totalPremiumGenerated: number;
}

// =================== CLAIMS OFFICER ===================
export interface CreateClaimsOfficerRequest {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phone: string;
    employeeId: string;
    department: string;
    specialization: string;
    approvalLimit: number;
}

export interface ClaimsOfficerDashboardResponse {
    officerId: number;
    officerName: string;
    email: string;
    employeeId: string;
    department: string;
    specialization: string;
    approvalLimit: number;
    totalClaimsProcessed: number;
    totalClaimsApproved: number;
    totalClaimsRejected: number;
    approvalRate: number;
    pendingReviewCount: number;
    unassignedClaimCount: number;
    escalatedCount: number;
}

export interface AdminClaimDecisionRequest {
    decision: string;
    approvedAmount?: number;
    adminRemarks: string;
    rejectionReason?: string;
}

// =================== AUDIT ===================
export interface AuditLogResponse {
    auditId: number;
    entityType: string;
    entityId: number;
    action: string;
    previousValue: string;
    newValue: string;
    remarks: string;
    performedByName: string;
    timestamp: string;
}
