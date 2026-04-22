import { Component, input } from '@angular/core';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  templateUrl: './skeleton-loader.component.html',
  styleUrls: ['./skeleton-loader.component.scss'],
})
export class SkeletonLoaderComponent {
  height = input<string>('100px');
}
