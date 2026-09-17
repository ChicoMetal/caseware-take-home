import { Component } from '@angular/core';
import { EngagementListComponent } from './components/engagement-list/engagement-list.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [EngagementListComponent],
  template: '<app-engagement-list />',
})
export class AppComponent {}
