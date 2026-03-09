import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
    selector: 'app-landing',
    standalone: true,
    imports: [RouterLink, CommonModule],
    templateUrl: './landing.component.html',
    styleUrl: './landing.component.css'
})
export class LandingComponent {
    constructor(public auth: AuthService) { }

    howItWorks = [
        { icon: '📋', title: 'Choose a Plan', desc: 'Browse our range of plans designed for individuals, families, and seniors.' },
        { icon: '🧮', title: 'Get a Quote', desc: 'Use our premium calculator to get an instant, personalized quote.' },
        { icon: '💳', title: 'Make Payment', desc: 'Complete your purchase with secure online payment options.' },
        { icon: '🛡️', title: 'Get Covered', desc: 'Your policy is activated instantly. Enjoy cashless treatment at 5000+ hospitals.' }
    ];

    features = [
        { icon: '🏥', title: 'Cashless Treatment', desc: 'Walk into any of our 5000+ network hospitals for hassle-free cashless treatment.' },
        { icon: '⚡', title: 'Instant Processing', desc: 'Claims processed within 24 hours with our AI-powered automated system.' },
        { icon: '🛡️', title: 'Comprehensive Cover', desc: 'From hospitalization to day care procedures, we have you fully covered.' },
        { icon: '👨‍👩‍👧‍👦', title: 'Family Floater', desc: 'Cover your entire family under a single plan at affordable premiums.' },
        { icon: '📱', title: 'Digital First', desc: 'Manage your policies, file claims, and track status all online.' },
        { icon: '🤝', title: 'Dedicated Support', desc: '24/7 customer support with dedicated relationship managers.' }
    ];

    plans = [
        { name: 'Basic Individual', price: '₹5,000/yr', coverage: '₹3 Lakh Coverage', features: ['Single person cover', 'Hospitalization expenses', 'Ambulance charges', 'Day care procedures'], color: '#0891b2' },
        { name: 'Family Floater', price: '₹15,000/yr', coverage: '₹10 Lakh Coverage', features: ['Up to 4 family members', 'Maternity cover included', 'Annual health checkups', 'No room rent capping'], color: '#f59e0b', popular: true },
        { name: 'Senior Citizen', price: '₹20,000/yr', coverage: '₹5 Lakh Coverage', features: ['Pre-existing diseases', 'No upper age limit', 'Day care procedures', 'Domiciliary treatment'], color: '#10b981' }
    ];

    testimonials = [
        { name: 'Priya Sharma', role: 'Family Plan Holder', text: 'InsureWell made the claim process incredibly smooth. When my father was hospitalized, the cashless facility worked perfectly. Highly recommended!' },
        { name: 'Rajesh Kumar', role: 'Individual Plan Holder', text: 'The premium calculator helped me find the perfect plan within my budget. The online policy management is very convenient.' },
        { name: 'Ananya Patel', role: 'Senior Citizen Plan', text: 'As a senior citizen, finding good health insurance was difficult. InsureWell covered my pre-existing conditions without any hassle.' }
    ];
}
