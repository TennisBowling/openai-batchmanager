package com.openai.batchmanager.model;

public class BatchRequestOutput {
    private String id;
    private String custom_id;
    private Response response;
    private Error error;

    public BatchRequestOutput() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustom_id() {
        return custom_id;
    }

    public void setCustom_id(String custom_id) {
        this.custom_id = custom_id;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public boolean isSuccess() {
        return response != null && error == null;
    }

    public boolean isError() {
        return error != null;
    }

    @Override
    public String toString() {
        return "BatchRequestOutput{" +
                "id='" + id + '\'' +
                ", custom_id='" + custom_id + '\'' +
                ", response=" + response +
                ", error=" + error +
                '}';
    }

    public static class Response {
        private int status_code;
        private String request_id;
        private Object body;

        public Response() {
        }

        public int getStatus_code() {
            return status_code;
        }

        public void setStatus_code(int status_code) {
            this.status_code = status_code;
        }

        public String getRequest_id() {
            return request_id;
        }

        public void setRequest_id(String request_id) {
            this.request_id = request_id;
        }

        public Object getBody() {
            return body;
        }

        public void setBody(Object body) {
            this.body = body;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "status_code=" + status_code +
                    ", request_id='" + request_id + '\'' +
                    ", body=" + body +
                    '}';
        }
    }

    public static class Error {
        private String code;
        private String message;

        public Error() {
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "Error{" +
                    "code='" + code + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}