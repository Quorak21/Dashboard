import { Component, input } from '@angular/core';

const CHART_EMPTY_MESSAGE = 'Pas de données actuellement disponible';

@Component({
  selector: 'app-chart-empty-state',
  templateUrl: './chart-empty-state.html',
})
export class ChartEmptyState {
  message = input<string>(CHART_EMPTY_MESSAGE);
}
