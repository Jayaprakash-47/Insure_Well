import { Component, HostListener } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { ToastService } from './core/services/toast.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class AppComponent {
  mobileMenuOpen = false;
  isScrolled = false;

  @HostListener('window:scroll')
  onScroll(): void {
    this.isScrolled = window.scrollY > 10;
  }

  constructor(
    public auth: AuthService,
    public toast: ToastService,
    private router: Router
  ) { }

  get showNavbar(): boolean {
    const url = this.router.url;
    return url !== '/login' && url !== '/register';
  }

  getNavLinks(): { label: string; route: string; icon: string }[] {
    if (this.auth.isAdmin()) {
      return [
        { label: 'Dashboard', route: '/admin/dashboard', icon: '📊' },
        { label: 'Customers', route: '/admin/customers', icon: '👥' },
        { label: 'Agents', route: '/admin/agents', icon: '🧑‍💼' },
        { label: 'Officers', route: '/admin/claims-officers', icon: '👨‍⚕️' },
        { label: 'Plans', route: '/admin/plans', icon: '📋' },
        { label: 'Claims', route: '/admin/claims', icon: '🏥' },
        { label: 'Escalated', route: '/admin/escalated-claims', icon: '⚠️' },
        { label: 'Audit', route: '/admin/audit-logs', icon: '📜' }
      ];
    }
    if (this.auth.isClaimsOfficer()) {
      return [
        { label: 'Dashboard', route: '/claims-officer/dashboard', icon: '📊' },
        { label: 'Claim Queue', route: '/claims-officer/queue', icon: '📥' },
        { label: 'My Claims', route: '/claims-officer/my-claims', icon: '📋' }
      ];
    }
    if (this.auth.isAgent()) {
      return [
        { label: 'Dashboard', route: '/agent/dashboard', icon: '📊' },
        { label: 'Sell Policy', route: '/agent/sell-policy', icon: '🤝' },
        { label: 'Plans', route: '/agent/plans', icon: '📋' },
        { label: 'My Sales', route: '/agent/policies', icon: '📄' }
      ];
    }
    if (this.auth.isCustomer()) {
      return [
        { label: 'Dashboard', route: '/customer/dashboard', icon: '📊' },
        { label: 'Plans', route: '/customer/plans', icon: '📋' },
        { label: 'Calculator', route: '/customer/calculator', icon: '🧮' },
        { label: 'My Policies', route: '/customer/policies', icon: '📄' },
        { label: 'My Claims', route: '/customer/claims', icon: '🏥' }
      ];
    }
    return [];
  }

  getRoleBadge(): string {
    if (this.auth.isAdmin()) return 'Admin';
    if (this.auth.isClaimsOfficer()) return 'Claims Officer';
    if (this.auth.isAgent()) return 'Agent';
    return 'Customer';
  }

  getRoleBadgeClass(): string {
    if (this.auth.isAdmin()) return 'role-admin';
    if (this.auth.isClaimsOfficer()) return 'role-officer';
    if (this.auth.isAgent()) return 'role-agent';
    return 'role-customer';
  }

  logout(): void {
    this.auth.logout();
    this.toast.success('Logged out successfully');
  }
}
