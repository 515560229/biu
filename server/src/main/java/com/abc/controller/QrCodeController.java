package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.exception.MessageRuntimeException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@PermInfo(value = "二维码模块", pval = "a:qrCode:接口")
@RestController
@RequestMapping("/qrCode")
public class QrCodeController {
    // 条码类型说明 http://www.cnaidc.com/tech/3467.html
    //1. 二维码   2. 条码
    private static Map<String, Class<? extends Writer>> writerMap = new HashMap<>();

    static {
        writerMap.put("qrCode", QRCodeWriter.class);
        writerMap.put("Code128", Code128Writer.class);
    }

    private static Map<String, BarcodeFormat> barcodeFormatMap = new HashMap<>();

    static {
        barcodeFormatMap.put("qrCode", BarcodeFormat.QR_CODE);
        barcodeFormatMap.put("Code128", BarcodeFormat.CODE_128);
    }

    @PermInfo("生成二维码")
    @RequiresPermissions("a:qrCode:generate")
    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE, value = "generate")
    public byte[] generate(@RequestParam String content,
                           @RequestParam(defaultValue = "300") int width,
                           @RequestParam(defaultValue = "300") int height,
                           @RequestParam(defaultValue = "qrCode", value = "style") String style) throws IOException, WriterException {
        Writer writer;
        try {
            writer = writerMap.get(style).newInstance();
        } catch (Exception e) {
            throw new MessageRuntimeException("不支持的条码样式。");
        }
        BitMatrix bitMatrix = writer.encode(content, barcodeFormatMap.get(style), width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    private static byte[] getQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    public static void main(String[] args) {
        int codeWidth = 3 + // start guard
                (7 * 6) + // left bars
                5 + // middle guard
                (7 * 6) + // right bars
                3; // end guard
        System.out.println(codeWidth);
    }
}
