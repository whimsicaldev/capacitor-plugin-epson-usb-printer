# capacitor-plugin-epson-usb-printer

Capacitor Plugin for printing to epson usb printer

## Install

```bash
npm install capacitor-plugin-epson-usb-printer
npx cap sync
```

## API

<docgen-index>

* [`getPrinterList()`](#getprinterlist)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### getPrinterList()

```typescript
getPrinterList() => Promise<{ printerList: PrinterInfo[]; }>
```

**Returns:** <code>Promise&lt;{ printerList: PrinterInfo[]; }&gt;</code>

--------------------


### Interfaces


#### PrinterInfo

| Prop              | Type                |
| ----------------- | ------------------- |
| **`productId`**   | <code>string</code> |
| **`productName`** | <code>string</code> |

</docgen-api>
