import { vi } from 'vitest';

// Chart.js touches canvas APIs that jsdom does not implement.
vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
