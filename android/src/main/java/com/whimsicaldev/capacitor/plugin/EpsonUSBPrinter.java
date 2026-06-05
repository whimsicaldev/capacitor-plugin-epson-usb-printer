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

import java.nio.charset.StandardCharsets;

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
    private byte[] codePage;
    private String codePageInString;;

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
            this.codePage = getCodePageForModel();
            this.codePageInString = getCodePageForModelInString();
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
            sendDataWithRetry(usbEndpoint, RESET, 10000);
            sendDataWithRetry(usbEndpoint, this.codePage, 10000);
            if(lineEntry.getLineStyleList() != null) {
                for(String style: lineEntry.getLineStyleList()) {
                    byte[] styleValue = EpsonUSBPrinterConstant.EPSON_STYLE_LIST.get(style);
                    if(styleValue != null) {
                        sendDataWithRetry(usbEndpoint, styleValue, 10000);
                    }
                }
            }

            if(lineEntry.getLineText() != null) {
                String printData = lineEntry.getLineText();
                String[] splitData = printData.split("\\n");

                for (String print: splitData) {
                    byte[] printBytes = print.getBytes(this.codePageInString);
                    sendDataWithRetry(usbEndpoint, printBytes, 10000);
                    sendDataWithRetry(usbEndpoint, LN, 10000);
                }
            }

            if(lineEntry.getLineCommandList() != null) {
                for(String command: lineEntry.getLineCommandList()) {
                    byte[] commandValue = EpsonUSBPrinterConstant.EPSON_COMMAND_LIST.get(command);
                    if(commandValue != null) {
                        sendDataWithRetry(usbEndpoint, commandValue, 10000);
                    }
                }
            }
        }

        // line feed to push the prints beyond the printer cover
        for(int i = 0; i < lineFeed; i+=1) {
            sendDataWithRetry(usbEndpoint, LN, 10000);
        }

        this.connection.releaseInterface(this.usbInterface);
    }

    private void sendDataWithRetry(UsbEndpoint endpoint, byte[] data, int timeout) throws Exception {
        int totalBytesSent = 0;
        int maxRetries = 3;
        int retryCount = 0;

        while (totalBytesSent < data.length && retryCount < maxRetries) {
            // Send only remaining data from offset totalBytesSent
            int remainingLength = data.length - totalBytesSent;
            int bytesSent = this.connection.bulkTransfer(endpoint, data, totalBytesSent, remainingLength, timeout);
            
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
    }

    private byte[] getCodePageForModel() {
        if (this.printerModel != null) {
            String modelLower = this.printerModel.toLowerCase();
            
            // UB-U03II requires CP437 code page
            if (modelLower.contains("ub-u03") || modelLower.contains("ubu03")) {
                return new byte[]{(byte) 0x1B, (byte) 0x74, (byte) 0x00}; // CP437 (USA)
            }
            // TM-U220B works with multiple code pages, but UTF-8 compatible
            else if (modelLower.contains("tm-u220b") || modelLower.contains("u220b")) {
                return new byte[]{(byte) 0x1B, (byte) 0x74, (byte) 0x0B}; // UTF-8 code page
            }
        }
        
        // Default to CP437 for unknown models
        Log.w("EpsonUSBPrinter", "Unknown printer model, defaulting to CP437");
        return new byte[]{(byte) 0x1B, (byte) 0x74, (byte) 0x00};
    }

    private String getCodePageForModelInString() {
        if (this.printerModel != null) {
            String modelLower = this.printerModel.toLowerCase();
            
            // UB-U03II requires CP437 code page
            if (modelLower.contains("ub-u03") || modelLower.contains("ubu03")) {
                return "CP437";
            }
            // TM-U220B works with multiple code pages, but UTF-8 compatible
            else if (modelLower.contains("tm-u220b") || modelLower.contains("u220b")) {
                return StandardCharsets.UTF_8;
            }
        }
        
        // Default to CP437 for unknown models
        Log.w("EpsonUSBPrinter", "Unknown printer model, defaulting to CP437");
        return "CP437";
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}