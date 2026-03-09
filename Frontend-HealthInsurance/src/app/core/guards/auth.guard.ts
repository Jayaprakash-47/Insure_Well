import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.isLoggedIn()) {
        return true;
    }

    router.navigate(['/login']);
    return false;
};

export const adminGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.isLoggedIn() && auth.isAdmin()) {
        return true;
    }

    router.navigate(['/login']);
    return false;
};

export const customerGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.isLoggedIn() && auth.isCustomer()) {
        return true;
    }

    router.navigate(['/login']);
    return false;
};

export const agentGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.isLoggedIn() && auth.isAgent()) {
        return true;
    }

    router.navigate(['/login']);
    return false;
};

export const claimsOfficerGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.isLoggedIn() && auth.isClaimsOfficer()) {
        return true;
    }

    router.navigate(['/login']);
    return false;
};

export const guestGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isLoggedIn()) {
        return true;
    }

    router.navigate([auth.getDashboardRoute()]);
    return false;
};
