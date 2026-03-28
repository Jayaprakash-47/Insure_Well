import { Component, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
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
export class ChatbotComponent implements AfterViewChecked {
  @ViewChild('scrollRef') private scrollRef!: ElementRef;

  isOpen = false;
  loading = false;
  userInput = '';
  private shouldScroll = false;

  quickChips = [
    { icon: 'receipt_long',  label: 'File a Claim',       text: 'How do I file a claim?' },
    { icon: 'calculate',     label: 'Get a Quote',         text: 'Can you help me get a premium quote?' },
    { icon: 'assignment',    label: 'Apply for Policy',    text: 'How do I apply for a new insurance policy?' },
    { icon: 'list_alt',      label: 'View Plans',          text: 'What insurance plans are available?' },
    { icon: 'help_outline',  label: 'How it Works',        text: 'How does InsureWell work?' },
  ];

  messages: ChatMessage[] = [
    {
      role: 'assistant',
      content: "Hi! I'm InsureBot, your InsureWell AI assistant. I can help you file claims, get a premium quote, apply for a policy, and more. How can I help you today?",
      time: new Date()
    }
  ];

  constructor(private api: ApiService, private sanitizer: DomSanitizer) {}

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
    // 1. Strip any remaining raw ** or __ bold markers (show text without them)
    const cleaned = text
      .replace(/\*\*(.*?)\*\*/g, '$1')   // **bold** → plain text
      .replace(/__(.*?)__/g, '$1')        // __bold__ → plain text
      .replace(/^#{1,3}\s+/gm, '')        // ## headings → plain
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
      s.replace(/\*(.*?)\*/g, '<em>$1</em>');  // only *italic* kept

    for (const raw of lines) {
      const line = raw.trimEnd();

      // Numbered list: optional leading spaces, digit(s), dot, space
      const olMatch = line.match(/^\s*(\d+)[.)]\s+(.+)$/);
      // Bullet list: optional leading spaces, then * - • + followed by space
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
    // Snapshot history BEFORE adding current user message
    // Strip leading assistant messages (Gemini requires first msg to be 'user')
    const rawHistory = this.messages.map(m => ({ role: m.role, content: m.content }));
    const firstUserIdx = rawHistory.findIndex(m => m.role === 'user');
    const historyToSend = (firstUserIdx >= 0 ? rawHistory.slice(firstUserIdx) : []).slice(-9);

    this.messages.push({ role: 'user', content: msg, time: new Date() });
    this.userInput = '';
    this.loading = true;
    this.shouldScroll = true;

    this.api.sendChatMessage(msg, historyToSend).subscribe({
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
  }

  onKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.send();
    }
  }
}
