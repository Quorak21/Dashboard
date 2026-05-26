// Affichage des data avec label
export interface MetricRow {
    label: string;
    value: string;
    colorClass?: string;
    variation?: number;
    ratio?: string;
}

// Titre + data
export interface MetricCard {
    title: string;
    metrics: MetricRow[];

}