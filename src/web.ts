import { WebPlugin } from '@capacitor/core';

import type { EpsonUSBPrinterPlugin, PrinterInfo } from './definitions';

/// <reference types="w3c-web-usb" />

const deviceList = [];

export class EpsonUSBPrinterWeb extends WebPlugin implements EpsonUSBPrinterPlugin {
  async getPrinterList(): Promise<{ printerList: PrinterInfo[]}> {
      if(!navigator.usb) {
        return Promise.reject('USB is not supported in this browser.');
      } else {
        const device = await (navigator as Navigator).usb.requestDevice({ filters: []});
        deviceList.push(device);
        return {
          printerList: [{
            productId: device.productId as unknown as string,
            productName: device.productName as unknown as string
          }] as PrinterInfo[]
        }
      }
  }
}
