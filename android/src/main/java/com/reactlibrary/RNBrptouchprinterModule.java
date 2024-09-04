
package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.uimanager.IllegalViewOperationException;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Canvas;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.Environment;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterInfo.ErrorCode;
import com.brother.ptouch.sdk.PrinterInfo.Model;
import com.brother.ptouch.sdk.PrinterStatus;
import com.brother.ptouch.sdk.NetPrinter;

import com.brother.sdk.lmprinter.Channel;
import com.brother.sdk.lmprinter.PrinterDriverGenerator;
import com.brother.sdk.lmprinter.PrinterDriverGenerateResult;
import com.brother.sdk.lmprinter.PrintError;
import com.brother.sdk.lmprinter.Log;
import com.brother.sdk.lmprinter.OpenChannelError;
import com.brother.sdk.lmprinter.PrinterDriver;
import com.brother.sdk.lmprinter.PrinterModel;
import com.brother.sdk.lmprinter.setting.QLPrintSettings;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Iterator;
import com.google.gson.Gson;
import java.io.File;

public class RNBrptouchprinterModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    Bitmap ImageToPrint;

    PrinterStatus printResult;

    public RNBrptouchprinterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNBrptouchprinter";
    }

    protected class PrinterThread extends Thread {
        @Override
        public void run() {
            // print
            // tobe removed
            Printer myPrinter = new Printer();
            printResult = new PrinterStatus();
            // call startCommunication prior to PrintImage or other print functions to open
            // a socket connection
            // and keep the number connections to one
            myPrinter.startCommunication();
            // call printImage to print a Bitmap
            printResult = myPrinter.printImage(ImageToPrint);
            if (printResult.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
                // add an alert message if you want
            }
            // When done printing call endCommunication
            myPrinter.endCommunication();
        }
    }

    public Bitmap textAsBitmap(String text, float textSize, int textColor) {
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent(); // ascent is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setColor(textColor);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }

    @ReactMethod
    public void print(String macAddress, String pdfPath, String workPath, String labelType, boolean isAutoCut,
            boolean isCutAtEnd,
            final Promise promise) {
        Thread th = new Thread(() -> {
            try {
                // Printer myPrinter = new Printer();
                // javax.print.attribute.standard.PrinterInfo myPrinterInfo = new PrinterInfo();
                // // set printer information
                // myPrinterInfo = myPrinter.getPrinterInfo();

                // myPrinterInfo.printerModel = PrinterInfo.Model.QL_820NWB;
                // myPrinter.setBluetooth(BluetoothAdapter.getDefaultAdapter());
                // myPrinterInfo.port = PrinterInfo.Port.BLUETOOTH;

                // myPrinterInfo.macAddress = macAddress;
                // myPrinterInfo.labelNameIndex = LabelInfo.QL700.valueOf(labelType).ordinal();
                // myPrinterInfo.orientation = PrinterInfo.Orientation.PORTRAIT;
                // myPrinterInfo.printMode = PrinterInfo.PrintMode.FIT_TO_PAPER;
                // myPrinterInfo.halftone = PrinterInfo.Halftone.THRESHOLD;
                // myPrinterInfo.isAutoCut = isAutoCut;
                // myPrinterInfo.isCutAtEnd = isCutAtEnd;
                // myPrinterInfo.workPath = pdfPath.toString();

                // promise.resolve(myPrinterInfo.toString());
                // myPrinter.setPrinterInfo(myPrinterInfo);

                // myPrinter.startCommunication();
                // int pages = getPdfPages(myPrinter, pdfPath);

                // for (int i = 1; i <= pages; i++) {
                // printResult = myPrinter.printPdfFile(pdfPath, i);
                // if (printResult.errorCode != PrinterInfo.ErrorCode.ERROR_NONE) {
                // // add an alert message if you want
                // promise.reject(printResult.errorCode.toString());
                // }
                // }

                // // //When done printing call endCommunication
                // myPrinter.endCommunication();
                // promise.resolve("Printing done!");

                // ==================================

                Channel channel = Channel.newBluetoothChannel(macAddress, BluetoothAdapter.getDefaultAdapter());

                PrinterDriverGenerateResult result = PrinterDriverGenerator.openChannel(channel);

                if (result.getError().getCode() != OpenChannelError.ErrorCode.NoError) {
                    promise.reject("Error - Open Channel: " + result.getError().getCode());
                    return;
                }

                PrinterDriver printerDriver = result.getDriver();

                QLPrintSettings printSettings = new QLPrintSettings(PrinterModel.QL_820NWB);

                printSettings.setLabelSize(QLPrintSettings.LabelSize.DieCutW62H29);
                printSettings.setAutoCut(true);
                printSettings.setWorkPath(workPath);

                PrintError printError = printerDriver.printPDF(pdfPath, printSettings);

                if (printError.getCode() != PrintError.ErrorCode.NoError) {
                    promise.reject("Error - Print Image: " + printError.getCode());
                } else {
                    promise.resolve("Success - Print Image");
                }

                printerDriver.closeChannel();

            } catch (Exception e) {
                promise.reject("error", e);
            }

        });
        th.start();
    }

    @ReactMethod
    public void getConnectedPrinters(Promise promise) {
        Gson gson = new Gson();
        Printer myPrinter = new Printer();
        NetPrinter[] net_printers = myPrinter.getNetPrinters("QL-720NW");
        try {
            WritableArray array = new WritableNativeArray();
            for (NetPrinter net_printer : net_printers) {
                WritableMap netPrinterDictionary = convertJsonToMap(new JSONObject(gson.toJson(net_printer)));
                array.pushMap(netPrinterDictionary);
            }
            promise.resolve(array);
        } catch (JSONException e) {
            promise.reject("", e);
        }
    }

    @ReactMethod
    public void concatStr(
            String string1,
            String string2,
            Promise promise) {
        promise.resolve(string1 + " jhj" + string2);
    }

    private WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
                if (("option_values").equals(key)) {
                    map.putArray("options", convertJsonToArray((JSONArray) value));
                }
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    private WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String) {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }

    private int getPdfPages(Printer printer, String pdfPath) {
        return printer.getPDFFilePages(pdfPath);
    }

    private void printLabels(String pdfPath, Printer printer) {
        int pages = getPdfPages(printer, pdfPath);

        try {
            if (printer.startCommunication()) {
                for (int i = 0; i < pages; i++) {
                    printer.printPdfFile(pdfPath, i);
                }
                printer.endCommunication();
            }
        } catch (Exception e) {
            throw e;
        }
    }
}
