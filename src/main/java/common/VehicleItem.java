package common;

public class VehicleItem extends Item {
    private int year; // năm sản xuất
    private double mileage; //

    public VehicleItem(String name, String description, double startingPrice, long durationSeconds, int year, double mileage) {
        super(name, description, startingPrice, durationSeconds);
        this.year = year;
        this.mileage = mileage;
    }
}