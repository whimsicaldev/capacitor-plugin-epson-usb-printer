export interface EpsonUSBPrinterPlugin {
  getPrinterList(): Promise<{ printerList: PrinterInfo[] }>;
}

export interface PrinterInfo {
  productId: string;
  productName: string;
}