package common.model.communication;

import java.io.Serializable;

public class ResponseMessage implements Serializable {
    private ResponseCode code;
    private String message;
    private Object data;

    public ResponseMessage(ResponseCode code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public ResponseCode getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }

    public static ResponseMessage success(String message, Object data) {
        return new ResponseMessage(ResponseCode.SUCCESS, message, data);
    }

    public static ResponseMessage error(String message) {
        return new ResponseMessage(ResponseCode.ERROR, message, null);
    }

    public static ResponseMessage authFailed(String message) {
        return new ResponseMessage(ResponseCode.AUTH_FAILED, message, null);
    }
}