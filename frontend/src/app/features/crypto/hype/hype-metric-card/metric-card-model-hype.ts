// Affichage des data avec label
export interface MetricRow {
    label: string;
    value: string;
    colorClass?: string;
    variation?: number | null;
    ratio?: string | null;
}

// Titre + data
export interface MetricCard {
    title: string;
    metrics: MetricRow[];

}