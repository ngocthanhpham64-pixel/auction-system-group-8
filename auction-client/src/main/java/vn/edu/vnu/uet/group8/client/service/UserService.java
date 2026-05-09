package vn.edu.vnu.uet.group8.client.service;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.*;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service quản lý thông tin người dùng: profile, ví tiền, lịch sử đấu giá.
 * -Tất cả callback chạy trên FX Thread - controller có thể update UI trực tiếp
 * -Service không hiển thị dialog/alert - controller tự quyết định
 */
public final class UserService{
    //Gson hỗ trợ Instant - bắt buộc vì Record có trường Instant placedAt
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    private UserService(){}
    // Profile
    /**
     * Tải thông tin profile của user hiện tại( trả về LoginResponse)
     * Nếu thành công: cập nhật (ClientModel.setCurrentUser(LoginResponse) và SessionManager phòng khi thông tin thay đổi
     * Nếu thất bại: callback nhận null
     * @param onResult callback nhận (LoginResponse) (null nếu lỗi)
     */
    public static void loadProfile(Consumer<LoginResponse> onResult){
        AuctionClient.getInstance().sendAuthenticatedRequest(ActionType.USER_PROFILE,null,
                response -> {
            if(response.isSuccess()){
                LoginResponse user = GsonUtil.toObject(response.getData(),LoginResponse.class);
                if(user != null){
                    ClientModel.getInstance().setCurrentUser(user);
                    SessionManager.setSession(SessionManager.getAuthToken(),
                            user.getUserId(),
                            user.getUsername(),
                            user.getFullName(),
                            user.getRole());
                    LOGGER.fine("Profile loaded: " + user.getUsername());
                } else{
                    LOGGER.warning("loadProfile received null data despite success response");
                }
                if(onResult != null) onResult.accept(user);
            } else{
                String err = response.getMessage() != null ? response.getMessage() : "Unknown error";
                LOGGER.log(Level.WARNING,"loadProfile failed: {0}" , err);
                if(onResult != null) onResult.accept(null);
            }
        });
    }
    //Ví tiền
    /**
     * Nạp tiền vào tài khoản.
     * Sau khi thành công, cập nhật số dư mới vào(ClientModel)
     * @param amount số tiền nạp(phải>0)
     * @param onResult callback nhận true nếu thành công, false nếu thất bại
     */
    public static void deposit(BigDecimal amount, Consumer<Boolean> onResult){
        if(amount == null || amount.compareTo(BigDecimal.ZERO)<=0){
            LOGGER.warning("Deposit rejected: invalid amount");
            if(onResult != null) onResult.accept(false);
            return;
        }
        if(! SessionManager.isLoggedIn()){
            LOGGER.warning("deposit called while not logged in");
            if(onResult != null) onResult.accept(false);
            return;
        }
        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.USER_DEPOSIT,amount,
                response -> {
                    if (response.isSuccess()) {
                        //Server trả về newBalance trong data
                        BigDecimal newBalance = GsonUtil.toObject(response.getData(), BigDecimal.class);
                        if (newBalance != null) {
                            ClientModel.getInstance().updateBalance(newBalance);
                            LOGGER.info("Deposit successful, new balance: " + newBalance);
                        } else {
                            LOGGER.warning("Deposit successful but newBalance is null");
                        }
                        if (onResult != null) onResult.accept(true);
                    } else {
                        String err = response.getMessage() != null ? response.getMessage() : "Nạp tiền thất bại";
                        LOGGER.log(Level.WARNING, "deposit failed: {0}", err);
                        if (onResult != null) onResult.accept(false);
                    }
                });
    }
    /**
     * Lấy danh sách lịch sử đấu giá của user hiện tại
     */
    public static void loadMyBids(Consumer<List<BidRecord>> onResult){
        AuctionClient.getInstance().sendAuthenticatedRequest(
                ActionType.USER_BIDS,null,
                response -> {
                    List<BidRecord> bids = GsonUtil.toList(response.getData(), BidRecord.class);
                    if (bids == null) {
                        LOGGER.warning("loadMyBids returned null data");
                        bids = List.of();
                    }
                    if (onResult != null) onResult.accept(bids);
                });
    }
}



