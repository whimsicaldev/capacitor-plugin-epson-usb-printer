package com.whimsicaldev.capacitor.plugin;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;

@CapacitorPlugin(name = "EpsonUSBPrinter")
public class EpsonUSBPrinterPlugin extends Plugin {

    private EpsonUSBPrinter implementation;

    @Override
    public void load() {
        implementation = new EpsonUSBPrinter(getContext());
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void getPrinterList(PluginCall call) {
        JSObject jsObject = new JSObject();
        JSONArray jsonArray = new JSONArray(implementation.getPrinterList());
        jsObject.put("printerList", jsonArray);
        call.resolve(jsObject);
    }

    @PluginMethod
    public void connectToPrinter(PluginCall call) {
        if(!call.hasOption("productId")) {
            call.reject("Product id is not provided.");
        } else if(call.hasOption("productId") && call.getInt("productId") == null) {
            call.reject("Product id is of incorrect type, please provide an integer.");
        }
        
        int productId = call.getInt("productId");
        try {
            JSObject jsObject = new JSObject();
            jsObject.put("connected", implementation.connectToPrinter(productId));
            call.resolve(jsObject);
        } catch(Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void print(PluginCall call) {
        if(!call.hasOption("printObject")) {
            call.reject("Print Object is not provided.");
        } else if(call.hasOption("printObject") && call.getString("printObject") == null) {
            call.reject("Print object is of incorrect type, please provide a string.");
        }

        String printObject = call.getString("printObject");
        int lineFeed = call.getInt("lineFeed", 0);
        try {
            implementation.print(printObject, lineFeed);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }
}