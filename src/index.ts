import { registerPlugin } from '@capacitor/core';

import type { EpsonUSBPrinterPlugin } from './definitions';

const EpsonUSBPrinter = registerPlugin<EpsonUSBPrinterPlugin>('EpsonUSBPrinter', {
  web: () => import('./web').then(m => new m.EpsonUSBPrinterWeb()),
});

export * from './definitions';
export { EpsonUSBPrinter };
