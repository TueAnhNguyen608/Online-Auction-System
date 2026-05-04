module ten.project.cua.ban {
    // 1. Module hệ thống Java
    requires java.sql;
    requires java.base;

    // 2. Module JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // 3. Thư viện bên ngoài
    requires com.google.gson;

    // 4. Cấp quyền mở package cho JavaFX và Gson
    opens view to javafx.fxml, javafx.graphics;
    opens view.admin to javafx.fxml, javafx.graphics;  // THÊM DÒNG NÀY
    opens server to com.google.gson;
    opens common to com.google.gson;

    // 5. Xuất các package để hệ thống chạy được
    exports view;
    exports view.admin;  // THÊM DÒNG NÀY
    exports server;
    exports common;
}

