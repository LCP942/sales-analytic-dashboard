import { Component, inject, signal } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { LoadingService } from './core/services/loading.service';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MatProgressBarModule, SidebarComponent, MatIconModule, MatButtonModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  protected readonly loading = inject(LoadingService);
  protected readonly sidebarOpen = signal(false);

  constructor() {
    inject(Router).events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe(() => this.sidebarOpen.set(false));
  }

  protected toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }

  protected closeSidebar(): void {
    this.sidebarOpen.set(false);
  }
}
