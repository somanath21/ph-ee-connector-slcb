package org.mifos.connector.slcb.camel.routes.file;

import org.mifos.connector.slcb.camel.routes.transfer.BaseSLCBRouteBuilder;
import org.mifos.connector.slcb.file.FileTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileOutputStream;
import static org.mifos.connector.slcb.camel.config.CamelProperties.LOCAL_FILE_PATH;
import static org.mifos.connector.slcb.camel.config.CamelProperties.SERVER_FILE_NAME;

@Component
public class FileRoute extends BaseSLCBRouteBuilder {

    @Autowired
    @Qualifier("awsStorage")
    private FileTransferService fileTransferService;

    @Value("${cloud.aws.bucket-name}")
    private String bucketName;

    @Override
    public void configure() throws Exception {

        /**
         * Downloads the file from cloud, stores in local and returns the file path
         * Input the file name through exchange variable: [SERVER_FILE_NAME]
         * Output the local file path through exchange variable: [LOCAL_FILE_PATH]
         */
        from("direct:download-file")
                .id("direct:download-file")
                .log("Started download-file route")
                .process(exchange -> {
                    String filename = exchange.getProperty(SERVER_FILE_NAME, String.class);

                    byte[] csvFile = fileTransferService.downloadFile(filename, bucketName);
                    File file = new File(filename);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(csvFile);
                    }
                    exchange.setProperty(LOCAL_FILE_PATH, file.getAbsolutePath());
                    logger.info("File downloaded");
                });

        /**
         * Uploads the file to cloud and returns the file name in cloud
         * Input the local file path through exchange variable: [LOCAL_FILE_PATH]
         * Output the server file name through exchange variable: [SERVER_FILE_NAME]
         */
        from("direct:upload-file")
                .id("direct:upload-file")
                .log("Uploading file")
                .process(exchange -> {
                    String filepath = exchange.getProperty(LOCAL_FILE_PATH, String.class);
                    String serverFileName = fileTransferService.uploadFile(new File(filepath), bucketName);
                    exchange.setProperty(SERVER_FILE_NAME, serverFileName);
                    logger.info("Uploaded file: {}", serverFileName);
                });
    }
}
