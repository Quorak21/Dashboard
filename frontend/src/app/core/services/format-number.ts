export function formatNumber(value: number | string | undefined | null): string {
    if (value === null || value === undefined || value === '') return '0';

    const num = typeof value === 'string' ? Number(value) : value;

    if (Number.isNaN(num)) return '—';

    const absNum = Math.abs(num);

    if (absNum >= 1.0e+12) {
        return (num / 1.0e+12).toFixed(2).replace(/\.00$/, '') + 'T';
    }
    if (absNum >= 1.0e+9) {
        return (num / 1.0e+9).toFixed(2).replace(/\.00$/, '') + 'B';
    }
    if (absNum >= 1.0e+6) {
        return (num / 1.0e+6).toFixed(2).replace(/\.00$/, '') + 'M';
    }
    if (absNum >= 1.0e+3) {
        return (num / 1.0e+3).toFixed(2).replace(/\.00$/, '') + 'K';
    }

    return num.toFixed(2).replace(/\.00$/, '');
}

export function formatCurrency(
  amount: number,
  currencyCode: string,
  locale = 'fr-FR',
): string {
  if (!Number.isFinite(amount)) {
    return formatNumber(amount);
  }
  const code = currencyCode?.trim() ?? '';
  if (!code) {
    return formatNumber(amount);
  }
  try {
    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: code,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${formatNumber(amount)} ${code}`;
  }
}
