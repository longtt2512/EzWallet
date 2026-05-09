package com.ezwallet.module.transfer.service;

import com.ezwallet.config.MinioProperties;
import com.ezwallet.exception.BusinessException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public String generateAndUpload(String qrId, String content) {
        try {
            byte[] pngBytes = generateQrPng(content, 300);
            String objectName = "qr/" + qrId + ".png";

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketPublic())
                    .object(objectName)
                    .stream(new ByteArrayInputStream(pngBytes), pngBytes.length, -1)
                    .contentType("image/png")
                    .build());

            return minioProperties.getEndpoint() + "/" + minioProperties.getBucketPublic() + "/" + objectName;
        } catch (Exception e) {
            throw new BusinessException("QR_GENERATION_FAILED",
                    "Không thể tạo mã QR: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private byte[] generateQrPng(String content, int size) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "PNG", out);
        return out.toByteArray();
    }
}
