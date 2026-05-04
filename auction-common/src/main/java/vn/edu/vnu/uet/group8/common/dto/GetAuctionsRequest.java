package vn.edu.vnu.uet.group8.common.dto;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import java.math.BigDecimal;

/**
 * DTO cho yêu cầu lấy danh sách phiên đấu giá với lọc và phân trang.
 *
 * Thiết kế theo pattern Builder để:
 *   1. Tất cả fields đều optional( lọc linh hoạt, không bắt buộc field nào)
 *   2.Validation tập trung trong build()
 *   3.Object immutable sau khi tạo
 *
 */
public final class GetAuctionsRequest {
    // Fields đều final -> immutable sau khi build()
    // null = không lọc theo danh mục(lấy tất cả)
    private final ItemCategory category;
    // null = không giới hạn
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;

    private final SortOption sortBy;

    private final int page;
    private final int pageSize;

    // Hằng số giới hạn

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Enum SortOption

    public enum SortOption{
        NEWEST, // Mới đăng
        ENDING_SOON, // Sắp kết thúc
        PRICE_ASC, // Giá tăng dần
        PRICE_DESC, // Giá giảm dần
    }

    // No-arg constructor: private, chỉ để GSON deserialize

    private GetAuctionsRequest(){
        this.category = null;
        this.minPrice = null;
        this.maxPrice = null;
        this.sortBy   = SortOption.NEWEST;
        this.page     = DEFAULT_PAGE;
        this.pageSize = DEFAULT_PAGE_SIZE;
    }
    // Constructor từ Builder: private, chỉ Builder gọi
    private GetAuctionsRequest(Builder b){
        this.category = b.category;
        this.minPrice = b.minPrice;
        this.maxPrice = b.maxPrice;
        this.sortBy   = b.sortBy;
        this.page     = b.page;
        this.pageSize = b.pageSize;
    }
    // Getters
    public ItemCategory getCategory() { return category; }
    public BigDecimal   getMinPrice() { return minPrice; }
    public BigDecimal   getMaxPrice() { return maxPrice; }
    public SortOption   getSortBy()   { return sortBy; }
    public int          getPage()     { return page; }
    public int          getPageSize() { return pageSize; }
    // Entry point tạo Builder
    public static Builder builder(){ return new Builder();}

    @Override
    public String toString() {
        return "GetAuctionsRequest{"
                + "category=" + category
                + ", minPrice=" + minPrice
                + ", maxPrice=" + maxPrice
                + ", sortBy=" + sortBy
                + ", page=" + page
                + ", pageSize=" + pageSize + '}';
    }
    // Builder
    public static class Builder{
        private ItemCategory category = null;
        private BigDecimal   minPrice = null;
        private BigDecimal   maxPrice = null;
        private SortOption   sortBy   = SortOption.NEWEST;
        private int          page     = DEFAULT_PAGE;
        private int          pageSize = DEFAULT_PAGE_SIZE;

        private Builder() {}

        public Builder category(ItemCategory category) {
            this.category = category;
            return this;
        }

        public Builder minPrice(BigDecimal minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public Builder maxPrice(BigDecimal maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public Builder sortBy(SortOption sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }
        public GetAuctionsRequest build(){
            if(page < 1){
                throw new IllegalArgumentException("Page phải >=1, nhận được: "+ page);}
            if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
                throw new IllegalArgumentException("PageSize phải trong khoảng [1, " + MAX_PAGE_SIZE +"],nhận được: "+ pageSize);}
            if(minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice)>0){
                throw new IllegalArgumentException( "minPrice (" + minPrice + ") không được lớn hơn maxPrice (" + maxPrice + ")");}

            if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("minPrice không được âm");
            }
            if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("maxPrice không được âm");
            }

            if (sortBy == null) {
                throw new IllegalArgumentException("sortBy không được null");
            }

            return new GetAuctionsRequest(this);
        }
    }
}
