package common;

import java.io.Serializable;

// Từ khóa 'abstract' giúp ngăn việc tạo trực tiếp 1 đối tượng Entity chung chung
public abstract class Entity implements Serializable {
    // Dùng 'protected' hoặc 'private' đều được. Ở đây mình dùng private cho chuẩn đóng gói.
    private String id;

    public Entity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
