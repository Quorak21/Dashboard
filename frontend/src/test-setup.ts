import { vi } from 'vitest';

vi.setConfig({ testTimeout: 30000 });

// Chart.js touches canvas APIs that jsdom does not implement.
if (typeof HTMLCanvasElement !== 'undefined') {
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
}

// Chart.js responsive mode relies on ResizeObserver, absent in jsdom.
class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

vi.stubGlobal('ResizeObserver', ResizeObserverMock);
