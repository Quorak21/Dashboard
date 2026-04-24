export function formatTime(timestamp: number | undefined | null): string {
    if (!timestamp) return '--:--';

    const date = new Date(timestamp);

    // On récupère les heures et minutes et on s'assure d'avoir 2 chiffres (ex: 09:05)
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');

    return `${hours}:${minutes}`;
}
