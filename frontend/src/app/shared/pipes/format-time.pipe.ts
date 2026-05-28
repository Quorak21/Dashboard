import { Pipe, PipeTransform } from '@angular/core';
import { formatTime } from '../../core/services/format-dates';

@Pipe({
  name: 'formatTime',
})
export class FormatTimePipe implements PipeTransform {
  transform(value: number | undefined | null): string {
    return formatTime(value);
  }
}
