export interface MetricRow {
    label: string;
    value: string;
    colorClass?: string;
    variation?: number;
    ratio?: string;
}

export interface MetricCard {
    title: string;
    metrics: MetricRow[];

}