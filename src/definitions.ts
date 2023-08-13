export interface EpsonUSBPrinterPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
