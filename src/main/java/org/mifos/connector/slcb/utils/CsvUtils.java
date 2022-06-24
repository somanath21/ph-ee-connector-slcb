package org.mifos.connector.slcb.utils;

import org.mifos.connector.slcb.dto.PaymentRequestDTO;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.List;

public class CsvUtils {

    public static File createCSVFile(List<PaymentRequestDTO> paymentRequestDTOList) throws IOException {
        Long timestamp = System.currentTimeMillis();
        String filePath = String.format("src/%s.csv", timestamp);
        File csvFile = new File(filePath);
        PrintWriter out = new PrintWriter(csvFile);

        out.println(getCsvHeader(PaymentRequestDTO.class));
        for (PaymentRequestDTO paymentRequestDTO: paymentRequestDTOList) {
            out.println(addObjectRow(paymentRequestDTO));
        }
        out.close();

        return csvFile;
    }

    public static  <T> String getCsvHeader(Class<T> dtoClass) {
        java.lang.reflect.Field[] fields = dtoClass.getDeclaredFields();
        String[] nm = new String[fields.length];
        int i = 0;
        for (Field field :fields) {
            nm[i++] = field.getName();
        }
        StringBuilder header = new StringBuilder();
        for(String s: nm) {
            header.append(s).append(",");
        }
        header.deleteCharAt(header.length()-1);
        header.append("\n");



        return header.toString();
    }

    public static String addObjectRow(Object obj) {
        char separator = ',';
        StringBuilder csvRow = new StringBuilder();
        Field fields[] = obj.getClass().getDeclaredFields();
        boolean firstField = true;
        for (Field field : fields) {
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(obj);
                if(value == null)
                    value = "";
                if(firstField){
                    csvRow.append(value);
                    firstField = false;
                }
                else{
                    csvRow.append(separator).append(value);
                }
                field.setAccessible(false);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // error
            }
        }
        return csvRow.toString();
    }
}
