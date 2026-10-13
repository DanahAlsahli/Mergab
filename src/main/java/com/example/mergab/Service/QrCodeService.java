package com.example.mergab.Service;



import com.example.mergab.Api.ApiException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class QrCodeService {

    // Generate a QR code image from the visitor's unique token
    public byte[] generateQrCode(String qrToken) {

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    qrToken,
                    BarcodeFormat.QR_CODE,
                    300,
                    300
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new ApiException("Failed to generate QR code");
        }
    }
}

