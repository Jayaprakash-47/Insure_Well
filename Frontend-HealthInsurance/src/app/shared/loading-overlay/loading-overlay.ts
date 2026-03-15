import { Component } from '@angular/core';
import {LoadingService} from '../../core/services/loading.service';
import {AsyncPipe} from '@angular/common';

@Component({
  selector: 'app-loading-overlay',
  imports: [
    AsyncPipe
  ],
  templateUrl: './loading-overlay.html',
  styleUrl: './loading-overlay.css',
})
export class LoadingOverlay {
  constructor(public loading: LoadingService) {}
}
