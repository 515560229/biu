package com.abc.controller;

import com.abc.annotation.PermInfo;
import com.abc.vo.Json;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@PermInfo(value = "二维码模块", pval = "a:qrCode:接口")
@RestController
@RequestMapping("/qrCode")
public class QrCodeController {
    @PermInfo("生成二维码")
    @RequiresPermissions("a:qrCode:generate")
    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE, value = "generate")
    public byte[] generate(@RequestParam String content,
                      @RequestParam int width,
                      @RequestParam int height) throws IOException, WriterException {
        return getQRCodeImage(content, width, height);
    }

    private static byte[] getQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

}
