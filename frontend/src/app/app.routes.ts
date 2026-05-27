import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: '',
        loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard)
    },
    {
        path: 'hype',
        loadComponent: () => import('./features/crypto/hype/hype').then(m => m.Hype)
    },
    {
        path: 'inveb',
        loadComponent: () => import('./features/stocks/inveb/inveb').then(m => m.Inveb)
    },
    // La route pour la 404, retour au dashboard
    {
        path: '**',
        redirectTo: '/'
    }
];
