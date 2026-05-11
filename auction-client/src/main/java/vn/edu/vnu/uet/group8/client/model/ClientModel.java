package vn.edu.vnu.uet.group8.client.model;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import vn.edu.vnu.uet.group8.common.dto.LoginResponse;
import vn.edu.vnu.uet.group8.common.entity.Item;
import vn.edu.vnu.uet.group8.common.dto.NotificationDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * ClientModel - Singleton quản lý toàn bộ trạng thái của ứng dụng.
 * Vai trò chính:Lưu trữ tập trung dữ lệu: user hiện tại, danh sách sp, thông báo, yêu thích...
 * Đảm bảo thread-safe: mọi thay đổi dữ liệu đều được chuyển về FX Application Thread.
 * Các phương thức real-time: cập nhật giá, thêm thông báo mới.
 * Cung cấp JavaFX properties để UI binding tự động cập nhật.
 */
public class ClientModel {
    private static final ClientModel INSTANCE = new ClientModel();

    private final ObjectProperty<LoginResponse> currentUser = new SimpleObjectProperty<>();
    private final ListProperty<Item> auctionItems = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<NotificationDTO> notifications = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Item> favoriteItems = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final IntegerProperty unreadNotificationCount = new SimpleIntegerProperty(0);
    private final BooleanProperty loggedIn = new SimpleBooleanProperty(false);
    private final ObjectProperty<Item> currentAuctionItem = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> balance = new SimpleObjectProperty<>(BigDecimal.ZERO);
    // Constructor private cho Singleton
    private ClientModel(){}

    public static ClientModel getInstance(){ return INSTANCE;}

    // Thread-safe setters (chạy trên FX thread nhờ runOnFX)

    /**
     * Cập nhật người dùng hiện tại
     * @param user đối tượng User mới(có thể null khi logout)
     */
    public void setCurrentUser(LoginResponse user) { runOnFX(()->currentUser.set(user));}

    /**
     * Cập nhật danh sách sản phẩm đang đấu giá.
     * @param items List<item> bất kỳ (không cần ObservableList)
     */
    public void setAuctionItems(List<Item> items){
        runOnFX(()->{
            auctionItems.clear();
            if (items!=null) auctionItems.addAll(items);
        });
    }
    /**
     * Cập nật danh sách thông báo.
     * @param list List<NotificationDTO> bất kỳ
     */
    public void setNotifications(List<NotificationDTO> list){
        runOnFX(()->{
            notifications.clear();
            if(list != null) notifications.addAll(list);
        });
    }
    /**
     * Cập nhật danh sách sản phẩm yêu thích.
     * @ param items List<Item> bất kỳ
     */
    public void setFavoriteItems(List<Item> items){
        runOnFX(()->{
            favoriteItems.clear();
            if(items != null) favoriteItems.addAll(items);
        });
    }
    /**
     * Đặt số lượng thông báo chưa đọc.
     * @ param count số nguyên >=0
     */
    public void setUnreadNotificationCount(int count){
        runOnFX(()-> unreadNotificationCount.set(count));
    }
    /**
     * Đặt trạng thái đăng nhập.
     * @ param value true nếu đã đăng nhập, false nếu chưa
     */
    public void setLoggedIn(boolean value){
        runOnFX(()-> loggedIn.set(value));
    }
    /**
     * Cập nhật giá hiện tại của một item(real-time)
     * @ param itemId id của sản phẩm cần cập nhật
     * @ param newPrice giá mới
     */
    public void updateItemCurrentPrice(int itemId, BigDecimal newPrice){
        runOnFX(()->{
            // Luồng xử lý theo stream, rõ ràng và an toàn
            auctionItems.stream()
                    .filter(item->item.getId() == itemId)
                    .findFirst()
                    .ifPresent(item->item.setCurrentPrice(newPrice));
            favoriteItems.stream()
                    .filter(item->item.getId()==itemId)
                    .findFirst()
                    .ifPresent(item->item.setCurrentPrice(newPrice));
        });
    }
    public void updateBalance(BigDecimal newBalance){
        runOnFX(()->balance.set(newBalance));
    }
    /**
     * Thêm một thông báo mới vào đầu danh sách(real-time)
     * @ param notification thông báo từ server push
     */
    public void addNotification(NotificationDTO notification){
        runOnFX(()->{
            notifications.add(0,notification);// thêm vào đầu - mới nhất lên trên
            unreadNotificationCount.set(unreadNotificationCount.get() + 1);
        });
    }
    /**
     * Đánh sấu một thông báo là đã đọc và giảm số lượng chưa đọc.
     * @ param notificationId id của thông báo cần đánh dấu
     */
    public void markNotificationRead(int notificationId){
        runOnFX(()->{
            for(int i =0;i<notifications.size();i++){
                NotificationDTO n = notifications.get(i);
                if(n.getId() == notificationId && !n.isRead()){
                    // Do NotificationDTO là immutable, tạo object mới với isRead = true
                    NotificationDTO updated = NotificationDTO.builder()
                            .id(n.getId())
                            .userId(n.getUserId())
                            .message(n.getMessage())
                            .isRead(true)
                            .createdAt(n.getCreatedAt())
                            .type(n.getType())
                            .build();
                    notifications.set(i,updated);
                    unreadNotificationCount.set(unreadNotificationCount.get()-1);
                    break;
                }
            }
        });
    }
    /**
     * Cập nhật sản phẩm dang được xem chi tiết.
     * @param item item hiện tại (hoặc null nếu đóng màn hình)
     */
    public void setCurrentAuctionItem(Item item){
        runOnFX(()-> currentAuctionItem.set(item));
    }
    /**
     *  Quản lý danh sách yêu thích
     *  (an toàn, dùng theo id thay vì equals)
     */
    public void addFavorite(Item item){
        if(item == null) return;
        runOnFX(()->{
            boolean exists = favoriteItems.stream()
                    .anyMatch(f->f.getId() == item.getId());
            if(!exists){
                favoriteItems.add(item);
            }
        });
    }
    public void removeFavorite(Item item){
        if(item == null) return;
        runOnFX(()->{
            favoriteItems.removeIf(f->f.getId() == item.getId());
        });
    }
    /**
     * Kiểm tra một item có trong danh sách yêu thích hay không
     * Lưu ý: Phương thức này chỉ ann toàn khi gọi từ JavaFX Application Thread.
     * Nếu cần goi từ background thread, hãy dùng runOnFX/ Platform.runLater
     * @ param itemId id sản phẩm
     * @ return true nếu có
     */
    public boolean isFavorite(int itemId){
        // Chỉ dùng từ FX Thread - không bảo vệ khi dùng thread khác
        return favoriteItems.stream().anyMatch(i->i.getId() == itemId);
    }
    /**
     * Xóa toàn bộ dữ liệu phiên làm việc (dùng khi logout)
     * Reset tất cả về trạng thái ban đầu.
     * Đưa model về trạng thái chưa đăng nhập
     */
    public void clearSession(){
        runOnFX(()->{
            currentUser.set(null);
            auctionItems.clear();
            notifications.clear();
            favoriteItems.clear();
            unreadNotificationCount.set(0);
            loggedIn.set(false);
            currentAuctionItem.set(null);
        });
    }
    // Getter và properties( cho UI binding)
    public LoginResponse getCurrentUser(){return currentUser.get();}
    public ObjectProperty<LoginResponse> currentUserProperty(){ return currentUser;}
    public ObservableList<Item> getAuctionItems(){return auctionItems.get();}
    public ListProperty<Item> auctionItemsProperty(){return auctionItems;}
    public ObservableList<NotificationDTO> getNotifications(){return notifications.get();}
    public ListProperty<NotificationDTO> notificationsProperty(){ return notifications;}
    public ObservableList<Item> getFavoriteItems(){ return favoriteItems.get();}
    public ListProperty<Item> favoriteItemsProperty(){ return favoriteItems;}
    public BigDecimal getBalance(){ return balance.get();}
    /**
     * @ return ReadOnlyIntegerProperty để binding số lượng yêu thích( ví dụ badge)
     */
    public ReadOnlyIntegerProperty favCountProperty(){
        return favoriteItems.sizeProperty();
    }
    public ReadOnlyObjectProperty<BigDecimal> balanceProperty(){
        return balance;
    }
    /** Tiện ích lấy số lượng yêu thích (không binding)*/
    public int getFavCount(){ return favoriteItems.size();}
    public int getUnreadNotificationCount(){ return unreadNotificationCount.get();}
    public IntegerProperty unreadNotificationCountProperty(){return unreadNotificationCount;}

    public boolean isLoggedIn(){ return loggedIn.get();}
    public BooleanProperty loggedInProperty(){return loggedIn;}
    public Item getCurrentAuctionItem(){return currentAuctionItem.get();}
    public ObjectProperty<Item> currentAuctionItemProperty(){ return currentAuctionItem;}
    // THREAD-SAFE HELPER
    /**
     * Đảm bảo một đoạn code được thực thi trên JavaFX Application Thread
     * Nếu đang ở FX Thread -> chạy ngay. Ngược lại -> gửi sang FX Thread qua Platform.runLater().
     * @ param action đoạn code đang chạy
     */
    private void runOnFX( Runnable action){
        if(Platform.isFxApplicationThread()){
            action.run();
        } else{
            Platform.runLater(action);
        }
    }
}
