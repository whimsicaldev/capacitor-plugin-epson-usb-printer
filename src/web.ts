import { WebPlugin } from '@capacitor/core';

import type { EpsonUSBPrinterPlugin, EpsonUSBPrinterInfo, EpsonUSBPrinterLineEntry } from './definitions';

/// <reference types="w3c-web-usb" />

export class EpsonUSBPrinterWeb extends WebPlugin implements EpsonUSBPrinterPlugin {
  private deviceList: USBDevice[] = [] as USBDevice[];
  private selectedDevice!: USBDevice;
  private endpointNumber!: number;
  private interfaceNumber!: number;

  private EPSON_STYLE_LIST: Map<string, Uint8Array> = new Map<string, Uint8Array>([
    ["ALIGN_LEFT", new Uint8Array([27, 97, 0])],
    ["ALIGN_CENTER", new Uint8Array([27, 97, 1])],
    ["ALIGN_RIGHT", new Uint8Array([27, 97, 2])],
    ["ALIGN_JUSTIFIED", new Uint8Array([27, 97, 1])],
    ["WIDE", new Uint8Array([27, 33, 4])],
    ["EMPHASIZED", new Uint8Array([27, 33, 8])],
    ["STRETCHED_WIDTH", new Uint8Array([27, 33, 16])],
    ["STRETCHED_HEIGHT", new Uint8Array([27, 33, 32])],
    ["UNDERLINED", new Uint8Array([27, 33, 128])],
    ["RESET", new Uint8Array([27, 64])],
    ["PARTIAL_CUT", new Uint8Array([27, 105])],
    ["FULL_CUT", new Uint8Array([27, 109])],
    ["LINE_BREAK", new Uint8Array([10])],
  ]);

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
      await this.selectedDevice.selectConfiguration(configValue as unknown as number);
      await this.selectInterfaceAndEnpoint();
      // const interfaceValue = this.selectedDevice.configuration?.interfaces[0].interfaceNumber
      // await this.selectedDevice.claimInterface(interfaceValue as unknown as number);
      // await this.selectedDevice.releaseInterface(interfaceValue as unknown as number);

      return { connected: true };
    } catch(e) {
      console.error(e);
      return Promise.reject(`Failed to establish a connection to device: ${options.productId}`);
    } 
  }

  private async selectInterfaceAndEnpoint(): Promise<void> {
    try {
      const altList: USBInterface[] = this.selectedDevice.configuration?.interfaces as unknown as USBInterface[];
      altList.forEach(alt => {
        const endpointList = alt.alternate.endpoints;

        endpointList.forEach(endpoint => {
          if(endpoint.type === 'bulk' && endpoint.direction === 'out') {
            this.endpointNumber = endpoint.endpointNumber;
            this.interfaceNumber = alt.interfaceNumber;
          }
        });
      });

      await this.selectedDevice.claimInterface(this.interfaceNumber);
      await this.selectedDevice.releaseInterface(this.interfaceNumber);

    } catch(e) {
      console.error(e);
      return Promise.reject(`Failed to find suitable interface`);
    }
  }

  
  async print(options: { printObject: string; lineFeed?: number }): Promise<void> {
    const encoder: TextEncoder = new TextEncoder();
    await this.selectedDevice.claimInterface(this.interfaceNumber);
    
    const LN: Uint8Array = this.EPSON_STYLE_LIST.get("LINE_BREAK") as unknown as Uint8Array;
    const RESET: Uint8Array = this.EPSON_STYLE_LIST.get("RESET") as unknown as Uint8Array;
    const printoutEntries = JSON.parse(options.printObject);

    printoutEntries.forEach((lineEntry: EpsonUSBPrinterLineEntry) => {
      this.selectedDevice.transferOut(this.endpointNumber, RESET);
      if(lineEntry.lineStyleList != null) {
        lineEntry.lineStyleList.forEach((style: string) => {
          const styleValue: Uint8Array = this.EPSON_STYLE_LIST.get(style)  as unknown as Uint8Array;
          if(style) {
            this.selectedDevice.transferOut(this.endpointNumber, styleValue);
          }
        });
      }

      if(lineEntry.lineText != null) {
          const printData: string = lineEntry.lineText;
          const splitData: string[] = printData.split("\\n");

          splitData.forEach(data => {
            this.selectedDevice.transferOut(this.endpointNumber, encoder.encode(data));
            this.selectedDevice.transferOut(this.endpointNumber, LN);
          });
      }
    });

    if(options.lineFeed) {
      for(let i = 0; i < options.lineFeed; i += 1) {
        this.selectedDevice.transferOut(this.endpointNumber, LN);
      }
    }
    
    await this.selectedDevice.releaseInterface(this.interfaceNumber);
  }
}