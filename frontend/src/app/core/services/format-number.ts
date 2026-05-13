export function formatNumber(num: number | undefined | null): string {
    if (num === null || num === undefined) return '0';

    const absNum = Math.abs(num);

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
