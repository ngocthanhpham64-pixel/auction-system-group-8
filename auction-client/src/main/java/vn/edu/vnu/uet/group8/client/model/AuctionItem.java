package vn.edu.vnu.uet.group8.client.model;

import javafx.beans.property.*;
public class AuctionItem {
   // Khai báo bằng Property thay vì các dữ liệu nguyên thủy
    private final StringProperty id;
    private final StringProperty name;
    private final DoubleProperty currentPrice;
    private final StringProperty status;// Trạng thái "Live", "UpComing", "Ended"
    private final StringProperty imagePath;

    // Constructor khởi tạo dữ liệu
    public AuctionItem(String id,String name,double currentPrice,String status,String imagePath){
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.currentPrice = new SimpleDoubleProperty(currentPrice);
        this.status = new SimpleStringProperty(status);
        this.imagePath = new SimpleStringProperty(imagePath);
    }
    //Getters thông thường (Lấy ra giá trị thực)
    public String getId() { return id.get(); }
    public String getName() { return name.get(); }
    public double getCurrentPrice() { return currentPrice.get(); }
    public String getStatus() { return status.get(); }
    public String getImagePath() { return imagePath.get(); }
    //Setters (Dùng khi có người đặt giá mới hoặc đổi trạng thái)
    public void setCurrentPrice(double price) { this.currentPrice.set(price); }
    public void setStatus(String newStatus) { this.status.set(newStatus); }
    //Property Getter: Trả về đối tượng (Object: Kiểu Property) vì the nó có sẵn phương thức cua JavaFx như blind() và addListener() để UI gọi ra và sử dụng
    public DoubleProperty currentPriceProperty(){return currentPrice;}
    public StringProperty nameProperty(){return name;}
    public StringProperty statusProperty(){return status;}
}
