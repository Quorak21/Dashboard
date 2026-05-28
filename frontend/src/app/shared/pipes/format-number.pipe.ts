import { Pipe, PipeTransform } from '@angular/core';
import { formatNumber } from '../../core/services/format-number';

@Pipe({
  name: 'formatNumber',
})
export class FormatNumberPipe implements PipeTransform {
  transform(value: number | string | undefined | null): string {
    return formatNumber(value);
  }
}
