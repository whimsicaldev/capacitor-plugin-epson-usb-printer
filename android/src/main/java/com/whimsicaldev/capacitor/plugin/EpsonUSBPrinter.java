package com.whimsicaldev.capacitor.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public class EpsonUSBPrinter {
    private final Context context;
    private final String actionString;
    private final UsbManager manager;

    public EpsonUSBPrinter(Context context) {
        this.context = context;
        this.actionString = this.context.getPackageName() + ".USB_PERMISSION";
        manager =  (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
    }

    public List<PrinterInfo> getPrinterList() {
        List<PrinterInfo> printerList = new ArrayList<>();
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this.context, 0, new
                Intent(actionString), PendingIntent.FLAG_IMMUTABLE);

        HashMap<String, UsbDevice> deviceList = this.manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();

            manager.requestPermission(device, mPermissionIntent);
            printerList.add(new PrinterInfo(device.getProductId() + "", device.getProductName()));
        }

        return printerList;
    }
}
