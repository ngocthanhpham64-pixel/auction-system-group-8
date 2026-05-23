package vn.edu.vnu.uet.group8.server.service.user;

import java.sql.SQLException;
import java.util.List;

import vn.edu.vnu.uet.group8.common.dto.model.CommentDTO;
import vn.edu.vnu.uet.group8.common.dto.model.ReviewDTO;
import vn.edu.vnu.uet.group8.common.exception.ValidationException;
import vn.edu.vnu.uet.group8.server.dao.CommentDAO;
import vn.edu.vnu.uet.group8.server.dao.RatingDAO;
import vn.edu.vnu.uet.group8.server.dao.UserDAO;

public class RatingService {
  private final RatingDAO ratingDAO;
  private final UserDAO userDAO;
  private final CommentDAO commentDAO;


  public RatingService(RatingDAO ratingDAO, UserDAO userDAO, CommentDAO commentDAO) {
    this.ratingDAO = ratingDAO;
    this.userDAO = userDAO;
    this.commentDAO = commentDAO;
  }

  public void rateSeller(int raterId, String raterUsername, int sellerId, int score, String comment) throws SQLException {
    if (raterId == sellerId) {
      throw new ValidationException("Bạn không thể tự đánh giá chính mình.");
    }
    if (score < 1 || score > 5) {
      throw new ValidationException("Điểm đánh giá phải từ 1 đến 5 sao.");
    }
    if (!ratingDAO.hasBoughtFrom(raterId, sellerId)) {
      throw new ValidationException("Bạn chỉ có thể đánh giá người bán sau khi thắng ít nhất một phiên đấu giá của họ.");
    }
    if (ratingDAO.hasRated(raterId, sellerId)) {
      throw new ValidationException("Bạn đã đánh giá người bán này rồi.");
    }
    ratingDAO.insertRating(raterId, raterUsername, sellerId, score, comment);
    userDAO.updateSellerRating(sellerId); // Cập nhật ngay điểm trung bình (AVG) vào bảng Users
  }

  public List<ReviewDTO> getSellerReviews(int sellerId) throws SQLException {
    return ratingDAO.getSellerReviews(sellerId);
  }

  public List<CommentDTO> getCommentsForUser(int user_id) {
    return commentDAO.getCommentsForUser(user_id);
  }
}
