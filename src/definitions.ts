export interface EpsonUSBPrinterInfo {
  productId?: number;
  productName: string;
  connected: boolean;
}

export interface EpsonUSBPrinterLineEntry {
  lineText?: string;
  lineStyleList?: string[];
  lineCommandList?: string[];
}

export interface EpsonUSBPrinterPlugin {
  getPrinterList(): Promise<{ printerList: EpsonUSBPrinterInfo[]; }>;

  connectToPrinter(options: { productId: number }): Promise<{ connected: boolean; }>;

  print(options: { printObject: string; lineFeed?: number }): Promise<void>;
}