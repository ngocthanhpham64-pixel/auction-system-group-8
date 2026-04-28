package vn.edu.vnu.uet.group8.common.dto;

import java.util.Locale;

public final class ServerResponse {
    private final String requestId; // sao chép lại requestId từ request gốc. CLient gửi request có requestId, server trả về response với cùng resquestId để client biết response này dành chp request nào
    private final boolean success; // kiểm tra thao tác thành công hay không?
    private final String message; // Thông báo mô tả kết quả, dành cho người dùng hoặc log
    private final Object data; // Dữ liệu trả về cụ thể, thay đổi theo từng loại request

    private ServerResponse(Builder b){
        this.requestId = b.requestId;
        this.success = b.success;
        this.message = b.message;
        this.data = b.data;
    }

    public String getResquestId(){ return requestId;}
    public boolean isSuccess(){ return success;}
    public String getMessage(){ return message;}
    public Object getData(){ return data;}

    public static Builder builder(String resquestId){ return new Builder(resquestId);}

    public static class Builder{
        private final String requestId;
        private boolean success;
        private String message;
        private Object data;

        private Builder(String requestId){ this.requestId = requestId;}
        public Builder success(boolean s){ this.success = s;return this;}
        public Builder message(String m){ this.message = m; return this;}
        public Builder data( Object d){ this.data = d; return this;}
        public ServerResponse build(){ return new ServerResponse(this);}
    }
}
