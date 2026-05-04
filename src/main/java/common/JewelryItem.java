package common;

public class JewelryItem extends Item {
    private String material;       // Chất liệu (Vàng 24k, Kim cương, Bạc)
    private double gemstoneWeight; // Trọng lượng đá quý (Carat) hoặc vàng (Chỉ/Lượng)

    public JewelryItem(String name, String description, double startingPrice, long durationSeconds, String material, double gemstoneWeight) {
        super(name, description, startingPrice, durationSeconds);
        this.material = material;
        this.gemstoneWeight = gemstoneWeight;
    }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public double getGemstoneWeight() { return gemstoneWeight; }
    public void setGemstoneWeight(double gemstoneWeight) { this.gemstoneWeight = gemstoneWeight; }

    @Override
    public String toString() {
        return "JewelryItem{" + "material='" + material + '\'' + ", weight=" + gemstoneWeight + "} " + super.toString();
    }
}