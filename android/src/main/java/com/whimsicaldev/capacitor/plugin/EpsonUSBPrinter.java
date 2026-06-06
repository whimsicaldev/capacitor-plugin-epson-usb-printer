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
    private UsbDevice currentDevice;
    private String printerModel;
    private final byte[] codePage = new byte[]{(byte) 0x1B, (byte) 0x74, (byte) 0x00};
    private final String codePageInString = "CP437";
    private List<byte[]> printDataList = new ArrayList<>();
    private boolean isPrinting = false;

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

    public List<Map> getPrinterList() throws Exception {
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
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(j);
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
            this.currentDevice = selectedDevice;
            this.printerModel = selectedDevice.getProductName();
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
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(j);
                if(UsbConstants.USB_ENDPOINT_XFER_BULK == usbEndpoint.getType() && UsbConstants.USB_DIR_OUT == usbEndpoint.getDirection()) {
                    this.usbInterface = usbInterface;
                    this.usbEndpoint = usbEndpoint;
                    return;
                }
            }
        }
    }

    public void print(String printObject, int lineFeed) throws Exception {
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
            this.queuePrintingData(RESET);
            if(lineEntry.getLineStyleList() != null) {
                for(String style: lineEntry.getLineStyleList()) {
                    byte[] styleValue = EpsonUSBPrinterConstant.EPSON_STYLE_LIST.get(style);
                    if(styleValue != null) {
                        this.queuePrintingData(styleValue);
                    }
                }
            }

            if(lineEntry.getLineText() != null) {
                String printData = lineEntry.getLineText();
                String[] splitData = printData.split("\\n");

                for (String print: splitData) {
                    this.printDiagnostic(print);
                }
            }

            if(lineEntry.getLineCommandList() != null) {
                for(String command: lineEntry.getLineCommandList()) {
                    byte[] commandValue = EpsonUSBPrinterConstant.EPSON_COMMAND_LIST.get(command);
                    if(commandValue != null) {
                        this.queuePrintingData(commandValue);
                    }
                }
            }
        }

        // line feed to push the prints beyond the printer cover
        for(int i = 0; i < lineFeed; i+=1) {
            this.queuePrintingData(LN);
        }

        this.connection.releaseInterface(this.usbInterface);
    }
    
    private void printDiagnostic(String text) throws Exception {
        byte[] LN = EpsonUSBPrinterConstant.EPSON_COMMAND_LIST.get(EpsonUSBPrinterConstant.LN);
        char[] chars = text.toCharArray();

        for (int j = 0; j < chars.length; j++) {
            String singleChar = String.valueOf(chars[j]);
            byte[] data = singleChar.getBytes(this.codePageInString);

            byte[] combined = new byte[this.codePage.length + data.length];
            System.arraycopy(this.codePage, 0, combined, 0, this.codePage.length);
            System.arraycopy(data, 0, combined, this.codePage.length, data.length);

            this.queuePrintingData(this.codePage);
            this.queuePrintingData(combined);
        }

        this.queuePrintingData(LN);
    }

    private void queuePrintingData(byte[] data) throws Exception {
        this.printDataList.add(this.codePage);
        this.printDataList.add(data);
        this.sendDataWithRetry();
    }

    private void sendDataWithRetry() throws Exception {
        if(this.isPrinting) {
            return;
        }

        if(this.printDataList.isEmpty()) {
            return;
        }

        byte[] data = this.printDataList.remove(0);
        this.isPrinting = true;
        int timeout = 10000;

        int totalBytesSent = 0;
        int maxRetries = 3;
        int retryCount = 0;

        while (totalBytesSent < data.length && retryCount < maxRetries) {
            // Send only remaining data from offset totalBytesSent
            int remainingLength = data.length - totalBytesSent;
            int bytesSent = this.connection.bulkTransfer(this.usbEndpoint, data, totalBytesSent, remainingLength, timeout);

            if (bytesSent < 0) {
                throw new Exception("USB transfer failed. Error code: " + bytesSent);
            }

            if (bytesSent == 0) {
                // No bytes sent, retry with delay
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                retryCount++;
            } else {
                totalBytesSent += bytesSent;
                retryCount = 0; // Reset retry counter on successful transfer
            }
        }

        if (totalBytesSent < data.length) {
            throw new Exception("Failed to send all data. Sent " + totalBytesSent + " of " + data.length + " bytes");
        }

        this.isPrinting = false;
        this.sendDataWithRetry();
    }
}