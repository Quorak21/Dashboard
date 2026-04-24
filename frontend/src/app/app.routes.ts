import { Routes } from '@angular/router';
import { Dashboard } from './features/dashboard/dashboard';
import { Hype } from './features/crypto/hype/hype';


export const routes: Routes = [
    {
        path: '', component: Dashboard
    },
    {
        path: 'hype', component: Hype
    }

];
