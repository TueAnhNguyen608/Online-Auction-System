package common;

public class AntiqueItem extends Item {
    private int originYear; // Năm sản xuất/nguồn gốc
    private String era;       // Triều đại hoặc thời kỳ (ví dụ: Nhà Minh, Phục Hưng)

    // Hàm khởi tạo (Constructor)
    public AntiqueItem(String name, String description, double startingPrice, long durationSeconds, int originYear, String era) {
        // Gửi thông tin chung lên cho lớp Item xử lý
        super(name, description, startingPrice, durationSeconds);
        // Lưu thông tin riêng vào túi của lớp AntiqueItem
        this.originYear = originYear;
        this.era = era;
    }

    // Getter và Setter
    public int getOriginYear() { return originYear; }
    public void setOriginYear(int originYear) { this.originYear = originYear; }

    public String getEra() { return era; }
    public void setEra(String era) { this.era = era; }

    @Override
    public String toString() {
        return "AntiqueItem{" + "originYear=" + originYear + ", era='" + era + '\'' + "} " + super.toString();
    }
}
