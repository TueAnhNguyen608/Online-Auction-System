package common;

public class ElectronicItem extends Item {
    private String brand;
    private int warrantyMonths;

    public ElectronicItem(String name, String description, double startingPrice, long durationSeconds, String brand, int warrantyMonths) {
        // Truyền 4 tham số bắt buộc vào Constructor của Item
        super(name, description, startingPrice, durationSeconds);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    // Getter/Setter cho brand và warrantyMonths...
}
