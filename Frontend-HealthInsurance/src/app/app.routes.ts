import { Routes } from '@angular/router';
import { guestGuard, adminGuard, customerGuard, agentGuard, claimsOfficerGuard } from './core/guards/auth.guard';

export const routes: Routes = [
    // Public
    { path: '', loadComponent: () => import('./pages/landing/landing.component').then(m => m.LandingComponent) },
    { path: 'login', loadComponent: () => import('./pages/auth/login/login.component').then(m => m.LoginComponent), canActivate: [guestGuard] },
    { path: 'register', loadComponent: () => import('./pages/auth/register/register.component').then(m => m.RegisterComponent), canActivate: [guestGuard] },
    { path: 'plans', loadComponent: () => import('./pages/plans/plans.component').then(m => m.PlansComponent) },

    // Admin
    { path: 'admin/dashboard', loadComponent: () => import('./pages/admin/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent), canActivate: [adminGuard] },
    { path: 'admin/customers', loadComponent: () => import('./pages/admin/customers/manage-customers.component').then(m => m.ManageCustomersComponent), canActivate: [adminGuard] },
    { path: 'admin/agents', loadComponent: () => import('./pages/admin/agents/manage-agents.component').then(m => m.ManageAgentsComponent), canActivate: [adminGuard] },
    { path: 'admin/claims-officers', loadComponent: () => import('./pages/admin/claims-officers/manage-claims-officers.component').then(m => m.ManageClaimsOfficersComponent), canActivate: [adminGuard] },
    { path: 'admin/plans', loadComponent: () => import('./pages/admin/plans/manage-plans.component').then(m => m.ManagePlansComponent), canActivate: [adminGuard] },
    { path: 'admin/policies', loadComponent: () => import('./pages/admin/policies/manage-policies.component').then(m => m.ManagePoliciesComponent), canActivate: [adminGuard] },
    { path: 'admin/claims', loadComponent: () => import('./pages/admin/claims/manage-claims.component').then(m => m.ManageClaimsComponent), canActivate: [adminGuard] },
    { path: 'admin/escalated-claims', loadComponent: () => import('./pages/admin/escalated-claims/escalated-claims.component').then(m => m.EscalatedClaimsComponent), canActivate: [adminGuard] },
    { path: 'admin/audit-logs', loadComponent: () => import('./pages/admin/audit-logs/audit-logs.component').then(m => m.AuditLogsComponent), canActivate: [adminGuard] },

    // Customer
    { path: 'customer/dashboard', loadComponent: () => import('./pages/customer/dashboard/customer-dashboard.component').then(m => m.CustomerDashboardComponent), canActivate: [customerGuard] },
    { path: 'customer/plans', loadComponent: () => import('./pages/customer/browse-plans/browse-plans.component').then(m => m.BrowsePlansComponent), canActivate: [customerGuard] },
    { path: 'customer/calculator', loadComponent: () => import('./pages/customer/calculator/calculator.component').then(m => m.CalculatorComponent), canActivate: [customerGuard] },
    { path: 'customer/policies', loadComponent: () => import('./pages/customer/my-policies/my-policies.component').then(m => m.MyPoliciesComponent), canActivate: [customerGuard] },
    { path: 'customer/claims', loadComponent: () => import('./pages/customer/my-claims/my-claims.component').then(m => m.MyClaimsComponent), canActivate: [customerGuard] },
    { path: 'customer/claims/new', loadComponent: () => import('./pages/customer/file-claim/file-claim.component').then(m => m.FileClaimComponent), canActivate: [customerGuard] },
    { path: 'customer/payment/:policyId', loadComponent: () => import('./pages/customer/payment/payment.component').then(m => m.PaymentComponent), canActivate: [customerGuard] },

    // Agent
    { path: 'agent/dashboard', loadComponent: () => import('./pages/agent/dashboard/agent-dashboard.component').then(m => m.AgentDashboardComponent), canActivate: [agentGuard] },
    { path: 'agent/plans', loadComponent: () => import('./pages/agent/plans/agent-plans.component').then(m => m.AgentPlansComponent), canActivate: [agentGuard] },
    { path: 'agent/policies', loadComponent: () => import('./pages/agent/policies/agent-policies.component').then(m => m.AgentPoliciesComponent), canActivate: [agentGuard] },
    { path: 'agent/sell-policy', loadComponent: () => import('./pages/agent/sell-policy/sell-policy.component').then(m => m.SellPolicyComponent), canActivate: [agentGuard] },

    // Claims Officer
    { path: 'claims-officer/dashboard', loadComponent: () => import('./pages/claims-officer/dashboard/co-dashboard.component').then(m => m.CODashboardComponent), canActivate: [claimsOfficerGuard] },
    { path: 'claims-officer/queue', loadComponent: () => import('./pages/claims-officer/queue/claim-queue.component').then(m => m.ClaimQueueComponent), canActivate: [claimsOfficerGuard] },
    { path: 'claims-officer/my-claims', loadComponent: () => import('./pages/claims-officer/my-claims/officer-claims.component').then(m => m.OfficerClaimsComponent), canActivate: [claimsOfficerGuard] },
    { path: 'claims-officer/review/:claimId', loadComponent: () => import('./pages/claims-officer/review/claim-review.component').then(m => m.ClaimReviewComponent), canActivate: [claimsOfficerGuard] },

    // Fallback
    { path: '**', redirectTo: '' }
];
