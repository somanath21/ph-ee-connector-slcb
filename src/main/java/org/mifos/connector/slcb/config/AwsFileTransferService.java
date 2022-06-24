package org.mifos.connector.slcb.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.io.File;

@Service
@Qualifier("awsStorage")
public class AwsFileTransferService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AmazonS3 s3Client;

    public AwsFileTransferService(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(File file, String bucketName) {
        String fileName = System.currentTimeMillis() + "_" + file.getName();
        s3Client.putObject(new PutObjectRequest(bucketName, fileName, file));
        file.delete();
        return fileName;
    }
}
