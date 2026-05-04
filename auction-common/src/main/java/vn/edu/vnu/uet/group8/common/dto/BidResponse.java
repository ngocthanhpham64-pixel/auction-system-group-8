package vn.edu.vnu.uet.group8.common.dto;

/**
 * Phản hồi cho yêu cầu đặt giá
 */
public final class BidResponse {
    private boolean success;
    private String message;

    private BidResponse() {}

    public BidResponse(boolean success,String message){
        this.message = message;
        this.success = success;
    }

    public boolean isSuccess(){ return success;}
    public String getMessage(){ return message;}

    @Override
    public String toString(){
        return "BidResponse{success=" + success + ", message='" + message + "'}";
    }
}

