package com.whimsicaldev.capacitor.plugin;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "EpsonUSBPrinter")
public class EpsonUSBPrinterPlugin extends Plugin {

    private EpsonUSBPrinter implementation;

    @Override
    public void load() {
        implementation = new EpsonUSBPrinter(getContext());
    }

    @PluginMethod
    public void getPrinterList(PluginCall call) {
        JSObject jsObject = new JSObject();
        jsObject.put("printerList", implementation.getPrinterList());
        call.resolve(jsObject);
    }
}
