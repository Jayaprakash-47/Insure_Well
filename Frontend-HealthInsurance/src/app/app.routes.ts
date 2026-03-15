import { Routes } from '@angular/router';
import {
  guestGuard,
  adminGuard,
  customerGuard,
  underwriterGuard,
  claimsOfficerGuard,
} from './core/guards/auth.guard';

export const routes: Routes = [
  // ── Public ────────────────────────────────────────────────────────────────
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing/landing.component').then((m) => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/auth/login/login.component').then((m) => m.LoginComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/auth/register/register.component').then((m) => m.RegisterComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'plans',
    loadComponent: () =>
      import('./pages/plans/plans.component').then((m) => m.PlansComponent),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./pages/auth/forgot_password/forgot-password/forgot-password').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },

  // ── Admin ─────────────────────────────────────────────────────────────────
  {
    path: 'admin/dashboard',
    loadComponent: () =>
      import('./pages/admin/dashboard/admin-dashboard.component').then(
        (m) => m.AdminDashboardComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/customers',
    loadComponent: () =>
      import('./pages/admin/customers/manage-customers.component').then(
        (m) => m.ManageCustomersComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/underwriters',
    loadComponent: () =>
      import('./pages/admin/underwriters/manage-underwriters.component').then(
        (m) => m.ManageUnderwritersComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/claims-officers',
    loadComponent: () =>
      import('./pages/admin/claims-officers/manage-claims-officers.component').then(
        (m) => m.ManageClaimsOfficersComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/plans',
    loadComponent: () =>
      import('./pages/admin/plans/manage-plans.component').then((m) => m.ManagePlansComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/policies',
    loadComponent: () =>
      import('./pages/admin/policies/manage-policies.component').then(
        (m) => m.ManagePoliciesComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/assign-underwriter',
    loadComponent: () =>
      import('./pages/admin/assign-underwriter/assign-underwriter.component').then(
        (m) => m.AssignUnderwriterComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/claims',
    loadComponent: () =>
      import('./pages/admin/claims/manage-claims.component').then((m) => m.ManageClaimsComponent),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/assign-claims-officer',
    loadComponent: () =>
      import('./pages/admin/assign-claims-officer/assign-claims-officer.component').then(
        (m) => m.AssignClaimsOfficerComponent,
      ),
    canActivate: [adminGuard],
  },
  {
    path: 'admin/profile',
    loadComponent: () =>
      import('./features/profile/profile').then((m) => m.Profile),
    canActivate: [adminGuard],
  },

  // ── Customer ──────────────────────────────────────────────────────────────
  {
    path: 'customer/dashboard',
    loadComponent: () =>
      import('./pages/customer/dashboard/customer-dashboard.component').then(
        (m) => m.CustomerDashboardComponent,
      ),
    canActivate: [customerGuard],
  },
  {
    path: 'customer/plans',
    loadComponent: () =>
      import('./pages/customer/browse-plans/browse-plans.component').then(
        (m) => m.BrowsePlansComponent,
      ),
    canActivate: [customerGuard],
  },
  {
    path: 'customer/calculator',
    loadComponent: () =>
      import('./pages/customer/calculator/calculator.component').then(
        (m) => m.CalculatorComponent,
      ),
    canActivate: [customerGuard],
  },
  {
    path: 'customer/policies',
    loadComponent: () =>
      import('./pages/customer/my-policies/my-policies.component').then(
        (m) => m.MyPoliciesComponent,
      ),
    canActivate: [customerGuard],
  },
  {
    path: 'customer/claims',
    loadComponent: () =>
      import('./pages/customer/my-claims/my-claims.component').then((m) => m.MyClaimsComponent),
    canActivate: [customerGuard],
  },
  {
    path: 'customer/claims/new',
    loadComponent: () =>
      import('./pages/customer/file-claim/file-claim.component').then(
        (m) => m.FileClaimComponent,
      ),
    canActivate: [customerGuard],
  },
  {
    path: 'customer/payment/:policyId',
    loadComponent: () =>
      import('./pages/customer/payment/payment.component').then((m) => m.PaymentComponent),
    canActivate: [customerGuard],
  },
  {
    path: 'customer/profile',
    loadComponent: () =>
      import('./features/profile/profile').then((m) => m.Profile),
    canActivate: [customerGuard],
  },

  // ── Underwriter ───────────────────────────────────────────────────────────
  {
    path: 'underwriter/dashboard',
    loadComponent: () =>
      import('./pages/underwriter/dashboard/underwriter-dashboard.component').then(
        (m) => m.UnderwriterDashboardComponent,
      ),
    canActivate: [underwriterGuard],
  },
  {
    path: 'underwriter/plans',
    loadComponent: () =>
      import('./pages/underwriter/plans/underwriter-plans.component').then(
        (m) => m.UnderwriterPlansComponent,
      ),
    canActivate: [underwriterGuard],
  },
  {
    path: 'underwriter/pending',
    loadComponent: () =>
      import('./pages/underwriter/pending/underwriter-pending.component').then(
        (m) => m.UnderwriterPendingComponent,
      ),
    canActivate: [underwriterGuard],
  },
  {
    path: 'underwriter/send-quote/:policyId',
    loadComponent: () =>
      import('./pages/underwriter/send-quote/send-quote.component').then(
        (m) => m.SendQuoteComponent,
      ),
    canActivate: [underwriterGuard],
  },
  {
    path: 'underwriter/send-quote',
    loadComponent: () =>
      import('./pages/underwriter/send-quote/send-quote.component').then(
        (m) => m.SendQuoteComponent,
      ),
    canActivate: [underwriterGuard],
  },
  {
    path: 'underwriter/policies',
    loadComponent: () =>
      import('./pages/underwriter/policies/underwriter-policies.component').then(
        (m) => m.UnderwriterPoliciesComponent,
      ),
    canActivate: [underwriterGuard],
  },
  {
    path: 'underwriter/profile',
    loadComponent: () =>
      import('./features/profile/profile').then((m) => m.Profile),
    canActivate: [underwriterGuard],
  },

  // ── Claims Officer ────────────────────────────────────────────────────────
  {
    path: 'claims-officer/dashboard',
    loadComponent: () =>
      import('./pages/claims-officer/dashboard/co-dashboard.component').then(
        (m) => m.CODashboardComponent,
      ),
    canActivate: [claimsOfficerGuard],
  },
  {
    path: 'claims-officer/my-claims',
    loadComponent: () =>
      import('./pages/claims-officer/my-claims/officer-claims.component').then(
        (m) => m.OfficerClaimsComponent,
      ),
    canActivate: [claimsOfficerGuard],
  },
  {
    path: 'claims-officer/review/:claimId',
    loadComponent: () =>
      import('./pages/claims-officer/review/claim-review.component').then(
        (m) => m.ClaimReviewComponent,
      ),
    canActivate: [claimsOfficerGuard],
  },
  {
    path: 'claims-officer/profile',
    loadComponent: () =>
      import('./features/profile/profile').then((m) => m.Profile),
    canActivate: [claimsOfficerGuard],
  },

  // ── Fallback ──────────────────────────────────────────────────────────────
  { path: '**', redirectTo: '' },
];
