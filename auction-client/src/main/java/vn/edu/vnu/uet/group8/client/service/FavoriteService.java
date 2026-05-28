package vn.edu.vnu.uet.group8.client.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import vn.edu.vnu.uet.group8.client.model.ClientModel;
import vn.edu.vnu.uet.group8.client.networking.AuctionClient;
import vn.edu.vnu.uet.group8.client.util.SessionManager;
import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.common.enums.ActionType;
import vn.edu.vnu.uet.group8.common.util.GsonUtil;

/**
 * Service quản lý danh sách yêu thích của người dùng. Tất cả callback đều chạy trên UI Thread (đảm
 * bảo bởi AuctionClient).
 */
public final class FavoriteService {
  private FavoriteService() {}

  /**
   * Tải toàn bộ danh sách yêu thích từ server và cập nhật ClientModel.
   *
   * @param onSuccess callback nhận danh sách yêu thích (chạy trên UI Thread)
   * @param onFailure callback nhận thông báo lỗi
   */
  public static void loadAll(
      Consumer<List<AuctionItemDTO>> onSuccess, Consumer<String> onFailure) {
    if (!SessionManager.isLoggedIn()) {
      if (onFailure != null) onFailure.accept("Bạn cần đăng nhập");
      return;
    }
    if (!AuctionClient.getInstance().isConnected()) {
      if (onFailure != null) onFailure.accept("Không có kết nối đến server");
      return;
    }
    AuctionClient.getInstance()
        .sendAuthenticatedRequest(
            ActionType.FAVORITE_LIST,
            null,
            response -> {
              if (response.isSuccess()) {
                List<AuctionItemDTO> items =
                    GsonUtil.toList(response.getData(), AuctionItemDTO.class);
                ClientModel.getInstance().setFavoriteItems(items);
                if (onSuccess != null) onSuccess.accept(items);
              } else {
                if (onFailure != null) onFailure.accept(response.getMessage());
              }
            });
  }

  /**
   * Thêm sản phẩm vào danh sách yêu thích Sau khi thành công, tự động reload danh sách để cập nhật
   * ClientModel.
   *
   * @param itemId ID sản phẩm cần thêm
   * @param onSuccess callback khi thành công (chạy trên UI Thread)
   * @param onFailure callback nhận thông báo lỗi
   */
  public static void add(int itemId, Runnable onSuccess, Consumer<String> onFailure) {
    if (!SessionManager.isLoggedIn()) {
      if (onFailure != null) onFailure.accept("Bạn cần đăng nhập");
      return;
    }
    AuctionClient.getInstance()
        .sendAuthenticatedRequest(
            ActionType.FAVORITE_ADD,
            java.util.Map.of("itemId", itemId),
            response -> {
              if (response.isSuccess()) {
                loadAll(
                    items -> {
                      if (onSuccess != null) onSuccess.run();
                    },
                    onFailure);
              } else {
                if (onFailure != null) onFailure.accept(response.getMessage());
              }
            });
  }

  /**
   * Xóa sản phẩm khỏi danh sách yêu thích. Sau khi thành công, tự động reload danh sách để cập nhật
   * ClientModel.
   *
   * @param itemId ID sản phẩm cần xóa
   * @param onSuccess callback khi thành công (chạy trên UI Thread)
   * @param onFailure callback nhận thông báo lỗi
   */
  public static void remove(int itemId, Runnable onSuccess, Consumer<String> onFailure) {
    if (!SessionManager.isLoggedIn()) {
      if (onFailure != null) onFailure.accept("Bạn cần đăng nhập");
      return;
    }
    AuctionClient.getInstance()
        .sendAuthenticatedRequest(
            ActionType.FAVORITE_REMOVE,
            Map.of("itemId", itemId),
            response -> {
              if (response.isSuccess()) {
                loadAll(
                    items -> {
                      if (onSuccess != null) onSuccess.run();
                    },
                    onFailure);
              } else {
                if (onFailure != null) onFailure.accept(response.getMessage());
              }
            });
  }
}
