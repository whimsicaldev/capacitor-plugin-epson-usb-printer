package com.whimsicaldev.capacitor.plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpsonUSBPrinterLineEntry {
    private String lineText;
    private List<String> lineStyleList;
    private List<String> lineCommandList;

    public EpsonUSBPrinterLineEntry() {}

    public String getLineText() {
        return lineText;
    }

    public void setLineText(String lineText) {
        this.lineText = lineText;
    }

    public List<String> getLineStyleList() {
        return lineStyleList;
    }

    public void setLineStyleList(List<String> lineStyleList) {
        this.lineStyleList = lineStyleList;
    }

    public List<String> getLineCommandList() {
        return lineCommandList;
    }

    public void setLineCommandList(List<String> lineCommandList) {
        this.lineCommandList = lineCommandList;
    }
}