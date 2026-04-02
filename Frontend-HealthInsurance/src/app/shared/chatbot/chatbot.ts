import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  safeHtml?: SafeHtml;
  time: Date;
}

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.css'
})
export class ChatbotComponent implements OnInit, AfterViewChecked {
  @ViewChild('scrollRef') private scrollRef!: ElementRef;

  isOpen = false;
  loading = false;
  userInput = '';
  private shouldScroll = false;

  // ── Customer context data ──
  private customerContext = '';
  private contextLoaded = false;

  quickChips = [
    { icon: 'shield',        label: 'My Policies',        text: 'Show me my current policies and their status.' },
    { icon: 'receipt_long',  label: 'My Claims',           text: 'What is the status of my claims?' },
    { icon: 'savings',       label: 'Coverage Left',       text: 'How much insurance coverage do I have remaining?' },
    { icon: 'event',         label: 'Policy Expiry',       text: 'When do my policies expire?' },
    { icon: 'calculate',     label: 'Get a Quote',         text: 'Can you help me get a premium quote?' },
    { icon: 'assignment',    label: 'Apply for Policy',    text: 'How do I apply for a new insurance policy?' },
    { icon: 'credit_card',   label: 'My Payments',         text: 'Show me my payment history.' },
    { icon: 'help_outline',  label: 'How it Works',        text: 'How does InsureWell work?' },
  ];

  messages: ChatMessage[] = [
    {
      role: 'assistant',
      content: "Hi! I'm InsureBot, your personal InsureWell AI assistant. I can answer questions about your policies, claims, payments, coverage — or anything else about InsureWell. How can I help you today?",
      time: new Date()
    }
  ];

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    // Pre-load customer data in the background so first interaction is instant
    this.loadCustomerContext();
  }

  /** Fetch all real-time customer data and build a compact context string for Gemini */
  private loadCustomerContext(): void {
    forkJoin({
      policies: this.api.getMyPolicies().pipe(catchError(() => of([]))),
      claims:   this.api.getMyClaims().pipe(catchError(() => of([]))),
      payments: this.api.getMyPayments().pipe(catchError(() => of([]))),
      profile:  this.api.getProfile().pipe(catchError(() => of(null))),
    }).subscribe(({ policies, claims, payments, profile }) => {
      const user = this.auth.getUser();
      const name = profile?.firstName || user?.firstName || 'Customer';
      const email = profile?.email || user?.email || '';

      // Format policies compactly
      const policyLines = (policies as any[]).map(p => {
        const expiry = p.endDate ? new Date(p.endDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : 'N/A';
        const remaining = p.remainingCoverage ?? p.coverageAmount ?? 0;
        return `  - Policy ${p.policyNumber} | Plan: ${p.planName} | Status: ${p.policyStatus} | Coverage: ₹${(p.coverageAmount||0).toLocaleString('en-IN')} | Remaining: ₹${remaining.toLocaleString('en-IN')} | Expiry: ${expiry}`;
      });

      // Format claims compactly
      const claimLines = (claims as any[]).map(c =>
        `  - Claim #${c.claimId} | Policy: ${c.policyNumber||'N/A'} | Amount: ₹${(c.claimAmount||0).toLocaleString('en-IN')} | Approved: ₹${(c.approvedAmount||0).toLocaleString('en-IN')} | Status: ${c.claimStatus} | Hospital: ${c.hospitalName||'N/A'}`
      );

      // Summarise payments
      const successPayments = (payments as any[]).filter((p: any) => p.paymentStatus === 'SUCCESS');
      const totalPaid = successPayments.reduce((s: number, p: any) => s + (p.amount || 0), 0);

      this.customerContext = [
        `Customer Name: ${name}`,
        `Email: ${email}`,
        `KYC Verified: ${profile?.aadhaarVerified ? 'Yes' : 'No'}`,
        '',
        `Policies (${(policies as any[]).length} total):`,
        ...(policyLines.length ? policyLines : ['  - No policies found']),
        '',
        `Claims (${(claims as any[]).length} total):`,
        ...(claimLines.length ? claimLines : ['  - No claims filed']),
        '',
        `Payments: ${(payments as any[]).length} total transactions | Total paid: ₹${totalPaid.toLocaleString('en-IN')} | Successful: ${successPayments.length}`,
      ].join('\n');

      this.contextLoaded = true;
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  private scrollToBottom(): void {
    try {
      const el = this.scrollRef?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch (_) {}
  }

  /**
   * Line-by-line markdown → HTML renderer.
   * Handles: numbered lists, dash/star bullets (including indented),
   * removes stray ** markers, wraps paragraphs, prevents overflow.
   */
  formatMessage(text: string): SafeHtml {
    const cleaned = text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')   // **bold** → <strong>
      .replace(/__(.*?)__/g, '<strong>$1</strong>')         // __bold__ → <strong>
      .replace(/^#{1,3}\s+/gm, '')                          // ## headings → plain
      .replace(/\r\n/g, '\n');

    const lines = cleaned.split('\n');
    let html = '';
    let inOl = false;
    let inUl = false;

    const closeList = () => {
      if (inOl) { html += '</ol>'; inOl = false; }
      if (inUl) { html += '</ul>'; inUl = false; }
    };

    const inlineFormat = (s: string) =>
      s.replace(/\*(.*?)\*/g, '<em>$1</em>');

    for (const raw of lines) {
      const line = raw.trimEnd();

      const olMatch = line.match(/^\s*(\d+)[.)]\s+(.+)$/);
      const ulMatch = line.match(/^\s*[-*•+]\s+(.+)$/);

      if (olMatch) {
        if (inUl) { html += '</ul>'; inUl = false; }
        if (!inOl) { html += '<ol class="chat-ol">'; inOl = true; }
        html += `<li class="chat-li">${inlineFormat(olMatch[2])}</li>`;

      } else if (ulMatch) {
        if (inOl) { html += '</ol>'; inOl = false; }
        if (!inUl) { html += '<ul class="chat-ul">'; inUl = true; }
        html += `<li class="chat-li">${inlineFormat(ulMatch[1])}</li>`;

      } else if (line.trim() === '') {
        closeList();
        html += '<div class="chat-spacer"></div>';

      } else {
        closeList();
        html += `<p class="chat-p">${inlineFormat(line)}</p>`;
      }
    }

    closeList();
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  send(): void {
    const msg = this.userInput.trim();
    if (!msg || this.loading) return;
    this.pushAndSend(msg);
  }

  sendQuick(text: string): void {
    if (this.loading) return;
    this.pushAndSend(text);
  }

  private pushAndSend(msg: string): void {
    const rawHistory = this.messages.map(m => ({ role: m.role, content: m.content }));
    const firstUserIdx = rawHistory.findIndex(m => m.role === 'user');
    const historyToSend = (firstUserIdx >= 0 ? rawHistory.slice(firstUserIdx) : []).slice(-9);

    this.messages.push({ role: 'user', content: msg, time: new Date() });
    this.userInput = '';
    this.loading = true;
    this.shouldScroll = true;

    // Refresh context if not loaded yet, then send
    const doSend = () => {
      this.api.sendChatMessage(msg, historyToSend, this.customerContext || undefined).subscribe({
        next: (res) => {
          const reply = res.reply || "I'm sorry, I didn't understand that. Could you rephrase?";
          this.messages.push({
            role: 'assistant',
            content: reply,
            safeHtml: this.formatMessage(reply),
            time: new Date()
          });
          this.loading = false;
          this.shouldScroll = true;
          // Refresh customer context after each interaction so data stays current
          this.loadCustomerContext();
        },
        error: () => {
          const errMsg = "I'm having trouble connecting right now. Please try again shortly.";
          this.messages.push({
            role: 'assistant',
            content: errMsg,
            safeHtml: this.formatMessage(errMsg),
            time: new Date()
          });
          this.loading = false;
          this.shouldScroll = true;
        }
      });
    };

    doSend();
  }

  onKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.send();
    }
  }
}
