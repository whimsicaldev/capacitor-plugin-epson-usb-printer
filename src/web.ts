import { WebPlugin } from '@capacitor/core';

import type { EpsonUSBPrinterPlugin } from './definitions';

export class EpsonUSBPrinterWeb extends WebPlugin implements EpsonUSBPrinterPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
