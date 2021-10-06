package org.mifos.connector.slcb.dto;

public class AuthErrorDTO {

    private int code;
    private String description;
    private Object sourceData;

    public AuthErrorDTO() {
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getSourceData() {
        return sourceData;
    }

    public void setSourceData(Object sourceData) {
        this.sourceData = sourceData;
    }
}
