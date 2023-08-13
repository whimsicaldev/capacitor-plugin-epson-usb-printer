package com.whimsicaldev.capacitor.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EpsonUSBPrinter {
    private final Context context;
    private final String actionString;
    private final UsbManager manager;
    private final List<UsbDevice> deviceList;
    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UsbDeviceConnection connection;

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }

    public EpsonUSBPrinter(Context context) {
        this.context = context;
        this.actionString = this.context.getPackageName() + ".USB_PERMISSION";
        this.manager =  (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        this.deviceList = new ArrayList<>();
    }

    public List<Map> getPrinterList() {
        List<Map> printerList = new ArrayList<>();
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this.context, 0, new
                Intent(actionString), PendingIntent.FLAG_IMMUTABLE);

        HashMap<String, UsbDevice> deviceList = this.manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice usbDevice = deviceIterator.next();

            if(isAPrinter(usbDevice)) {
                manager.requestPermission(usbDevice, mPermissionIntent);
                Map printerInfo = new HashMap();
                printerInfo.put("productId", usbDevice.getProductId());
                printerInfo.put("productName", usbDevice.getProductName());
                printerInfo.put("connected", false);
                printerList.add(printerInfo);
                this.deviceList.add(usbDevice);
            }
        }

        return printerList;
    }

    private boolean isAPrinter(UsbDevice usbDevice) {
        for(int i =0; i < usbDevice.getInterfaceCount(); i += 1) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(i);
                return UsbConstants.USB_ENDPOINT_XFER_BULK == usbEndpoint.getType() && UsbConstants.USB_DIR_OUT == usbEndpoint.getDirection();
            }
        }
        return false;
    }

    public boolean connectToPrinter(int productId) throws Exception {
        UsbDevice selectedDevice = null;
        for(UsbDevice device: this.deviceList) {
            if(productId == device.getProductId()) {
                selectedDevice = device;
                setUsbInterfaceAndEndpoint(selectedDevice);
                break;
            }
        }

        if(selectedDevice == null) {
            throw new Exception("Device with product id " + productId + " is not found.");
        }

        try {
            this.connection = this.manager.openDevice(selectedDevice);
            return true;
        } catch(Exception e) {
            this.connection = null;
            throw new Exception("Failed to establish connection to device " + productId + " due to " + e.getMessage());
        }
    }

    private void setUsbInterfaceAndEndpoint(UsbDevice usbDevice) {
        for(int i =0; i < usbDevice.getInterfaceCount(); i += 1) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(i);
                if(UsbConstants.USB_ENDPOINT_XFER_BULK == usbEndpoint.getType() && UsbConstants.USB_DIR_OUT == usbEndpoint.getDirection()) {
                    this.usbInterface = usbInterface;
                    this.usbEndpoint = usbEndpoint;
                    return;
                }
            }
        }
    }

    public void print(String printObject) throws Exception {
        if(this.connection == null) {
            throw new Exception("Currently not connected to a device.");
        } else if(this.usbInterface == null) {
            throw new Exception("Usb interface is not properly set.");
        }
        List<EpsonUSBPrinterLineEntry> printObjectList = this.objectMapper.readValue(printObject, new TypeReference<>() {});

        this.connection.claimInterface(this.usbInterface, true);
        byte[] LN = EpsonUSBPrinterConstant.EPSON_COMMAND_LIST.get(EpsonUSBPrinterConstant.LN);
        byte[] RESET = EpsonUSBPrinterConstant.EPSON_COMMAND_LIST.get(EpsonUSBPrinterConstant.RESET);

        for(EpsonUSBPrinterLineEntry lineEntry: printObjectList) {
            connection.bulkTransfer(usbEndpoint, RESET, RESET.length, 10000);
            if(lineEntry.getLineStyleList() != null) {
                for(String style: lineEntry.getLineStyleList()) {
                    byte[] styleValue = EpsonUSBPrinterConstant.EPSON_STYLE_LIST.get(style);
                    if(styleValue != null) {
                        connection.bulkTransfer(usbEndpoint, styleValue, styleValue.length, 10000);
                    }
                }
            }

            if(lineEntry.getLineText() != null) {
                String printData = lineEntry.getLineText();
                this.connection.bulkTransfer(this.usbEndpoint, printData.getBytes(), printData.getBytes().length, 10000);
                this.connection.bulkTransfer(this.usbEndpoint, LN, LN.length, 10000);
            }

            if(lineEntry.getLineCommandList() != null) {
                for(String command: lineEntry.getLineCommandList()) {
                    byte[] commandValue = EpsonUSBPrinterConstant.EPSON_COMMAND_LIST.get(command);
                    if(commandValue != null) {
                        connection.bulkTransfer(usbEndpoint, commandValue, commandValue.length, 10000);
                    }
                }
            }
        }

        // line feed to push the prints beyond the printer cover
        for(int i = 0; i < 6; i+=1) {
            this.connection.bulkTransfer(this.usbEndpoint, LN, LN.length, 10000);
        }

        this.connection.releaseInterface(this.usbInterface);
    }
}