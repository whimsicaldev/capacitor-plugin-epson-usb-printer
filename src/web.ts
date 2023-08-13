import { WebPlugin } from '@capacitor/core';

import type { EpsonUSBPrinterPlugin, EpsonUSBPrinterInfo } from './definitions';

/// <reference types="w3c-web-usb" />

export class EpsonUSBPrinterWeb extends WebPlugin implements EpsonUSBPrinterPlugin {
  private deviceList: USBDevice[] = [] as USBDevice[];
  private selectedDevice!: USBDevice;

  async getPrinterList(): Promise<{ printerList: EpsonUSBPrinterInfo[]}> {
    if(!navigator.usb) {
      return Promise.reject('USB is not supported in this browser.');
    }
    const device = await (navigator as Navigator).usb.requestDevice({ filters: []});
    this.deviceList.push(device);
    
    return {
      printerList: [{
        productId: device.productId as unknown as number,
        productName: device.productName as unknown as string,
        connected: false
      }] as EpsonUSBPrinterInfo[]
    };
  }

  async connectToPrinter(options: { productId: number }): Promise<{ connected: boolean; }> {
    if(!navigator.usb) {
      return Promise.reject('USB is not supported in this browser.');
    }

    this.selectedDevice = this.deviceList.find(device => device.productId === options.productId) as unknown as USBDevice;

    if(!this.selectedDevice) {
      return Promise.reject(`Device with product id ${options.productId} is not found.`);
    }

    try {
      await this.selectedDevice.open();
      const configValue = this.selectedDevice.configuration?.configurationValue
      const interfaceValue = this.selectedDevice.configuration?.interfaces[0].interfaceNumber
      await this.selectedDevice.selectConfiguration(configValue as unknown as number);
      await this.selectedDevice.claimInterface(interfaceValue as unknown as number);

      return { connected: true };
    } catch(e) {
      console.error(e);
      return Promise.reject(`Failed to establish a connection to device: ${options.productId}`);
    } 
  }

  
  print(options: { printObject: string; }): Promise<void> {
    console.log(options);
    throw new Error('Method not implemented.');
  }
}