package vn.edu.vnu.uet.group8.server.service.item;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import vn.edu.vnu.uet.group8.common.dto.model.AuctionItemDTO;
import vn.edu.vnu.uet.group8.server.dao.FavoriteDAO;

public class FavoriteService {
  private final FavoriteDAO favoriteDAO;
  private final ItemQueryService itemQueryService;

  public FavoriteService(FavoriteDAO favoriteDAO, ItemQueryService itemQueryService) {
    this.favoriteDAO = favoriteDAO;
    this.itemQueryService = itemQueryService;
  }

  public void addFavorite(int userId, int itemId) throws SQLException {
    favoriteDAO.addFavorite(userId, itemId);
  }

  public void removeFavorite(int userId, int itemId) throws SQLException {
    favoriteDAO.removeFavorite(userId, itemId);
  }

  public List<AuctionItemDTO> getFavoriteItems(int userId) throws SQLException {
    List<Integer> itemIds = favoriteDAO.getFavoriteItemIds(userId);
    List<AuctionItemDTO> favoriteItems = new ArrayList<>();
    
    // Duyệt qua danh sách ID và dùng ItemQueryService để lấy thông tin chi tiết
    for (int itemId : itemIds) {
      try {
        AuctionItemDTO dto = itemQueryService.getItemDetail(itemId);
        favoriteItems.add(dto);
      } catch (Exception e) {
        // Bỏ qua nếu item không tồn tại hoặc lỗi (ví dụ item bị xóa)
      }
    }
    return favoriteItems;
  }
}
