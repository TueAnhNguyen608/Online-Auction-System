package common;

public class ArtItem extends Item {
    private String artist;
    private String medium; // Ví dụ: Sơn dầu, Gỗ, Đá...

    public ArtItem(String name, String description, double startingPrice, long durationSeconds, String artist, String medium) {
        super(name, description, startingPrice, durationSeconds);
        this.artist = artist;
        this.medium = medium;
    }
}