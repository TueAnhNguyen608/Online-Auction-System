package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import common.User;

import java.util.function.Consumer;

/**
 * Sidebar dùng chung cho các màn hình lịch sử / profile.
 * Gọi: SidebarHelper.build(user, menuItems, activeIndex, onMenuClick)
 */
public class SidebarHelper {

    private static final String BG_SIDEBAR = "#0c1032";
    private static final String BORDER     = "#2d3270";
    private static final String TEXT       = "#f0f4ff";
    private static final String MUTED      = "#8899bb";
    private static final String BLUE     = "#2563eb";
    private static final String GOLD       = "#f59e0b";

    /**
     * @param user        thông tin user
     * @param menuItems   mảng [icon, label] ví dụ {{"📋","Lịch sử đấu giá"},{"🏆","Sản phẩm đã thắng"}}
     * @param activeIndex index menu đang active
     * @param onMenuClick callback khi click menu
     */
    public static VBox build(User user, String[][] menuItems, int activeIndex,
                             Consumer<Integer> onMenuClick) {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(210);
        sidebar.setMinWidth(210);
        sidebar.setStyle("-fx-background-color:" + BG_SIDEBAR + ";"
                + "-fx-border-color:" + BORDER + ";-fx-border-width:0 1 0 0;");

        // Logo
        HBox logo = LoginApp.buildLogo();
        logo.setPadding(new Insets(20, 14, 16, 16));
        logo.setStyle("-fx-border-color:transparent transparent " + BORDER + " transparent;"
                + "-fx-border-width:0 0 1 0;");

        // Menu
        VBox menu = new VBox(3);
        menu.setPadding(new Insets(14, 8, 0, 8));

        for (int i = 0; i < menuItems.length; i++) {
            final int idx = i;
            String icon  = menuItems[i][0];
            String label = menuItems[i][1];

            Button btn = new Button(icon + "  " + label);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(42);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setFont(Font.font("Segoe UI", idx == activeIndex ? FontWeight.BOLD : FontWeight.NORMAL, 13));
            btn.setCursor(Cursor.HAND);

            if (idx == activeIndex) {
                btn.setStyle(activeStyle());
            } else {
                btn.setStyle(idleStyle());
                btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle()));
                btn.setOnMouseExited(e -> btn.setStyle(idleStyle()));
            }

            btn.setOnAction(e -> onMenuClick.accept(idx));
            menu.getChildren().add(btn);
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + BORDER + ";");
        sep.setPadding(new Insets(0, 10, 0, 10));

        // Avatar card
        VBox avatarCard = buildAvatarCard(user);

        sidebar.getChildren().addAll(logo, menu, spacer, sep, avatarCard);
        return sidebar;
    }

    private static VBox buildAvatarCard(User user) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 10, 16, 10));

        // Avatar
        StackPane avCircle = new StackPane();
        avCircle.setPrefSize(64, 64); avCircle.setMaxSize(64, 64);
        avCircle.setStyle("-fx-background-color:linear-gradient(to bottom right,#2d1b69,#2563eb);"
                + "-fx-background-radius:32;");

        ImageView avImg = LoginApp.loadImage("/images/avatar.png", 50, 50);
        if (avImg != null) avCircle.getChildren().add(avImg);
        else { Label emo = new Label("👤"); emo.setFont(Font.font(26)); avCircle.getChildren().add(emo); }

        String uname = user != null ? user.getUsername() : "Người dùng";
        Label lblName = new Label(uname);
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblName.setTextFill(Color.web(TEXT));

        Label vip = new Label("⭐  Thành viên VIP");
        vip.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        vip.setTextFill(Color.web(GOLD));
        vip.setStyle("-fx-background-color:rgba(245,158,11,0.12);-fx-background-radius:20;-fx-padding:2 8;");

        // Stats
        HBox stats = new HBox(10);
        stats.setAlignment(Pos.CENTER);
        stats.setPadding(new Insets(6, 0, 0, 0));
        stats.getChildren().addAll(
                stat("128", "Đấu giá"),
                new Separator(javafx.geometry.Orientation.VERTICAL) {{ setStyle("-fx-background-color:#2d3270;"); }},
                stat("23",  "Đã thắng"),
                new Separator(javafx.geometry.Orientation.VERTICAL) {{ setStyle("-fx-background-color:#2d3270;"); }},
                stat("4.8", "Đánh giá")
        );

        card.getChildren().addAll(avCircle, lblName, vip, stats);
        return card;
    }

    private static VBox stat(String num, String lbl) {
        Label n = new Label(num); n.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); n.setTextFill(Color.web("#f0f4ff"));
        Label l = new Label(lbl); l.setFont(Font.font("Segoe UI", 10)); l.setTextFill(Color.web("#8899bb"));
        VBox v = new VBox(1, n, l); v.setAlignment(Pos.CENTER); return v;
    }

    private static String activeStyle() {
        return "-fx-background-color:rgba(124,58,237,0.18);-fx-text-fill:#a78bfa;"
                + "-fx-background-radius:10;-fx-border-color:transparent;"
                + "-fx-alignment:CENTER_LEFT;-fx-padding:0 14;";
    }

    private static String idleStyle() {
        return "-fx-background-color:transparent;-fx-text-fill:#8899bb;"
                + "-fx-background-radius:10;-fx-border-color:transparent;"
                + "-fx-alignment:CENTER_LEFT;-fx-padding:0 14;";
    }

    private static String hoverStyle() {
        return "-fx-background-color:rgba(255,255,255,0.05);-fx-text-fill:#f0f4ff;"
                + "-fx-background-radius:10;-fx-border-color:transparent;"
                + "-fx-alignment:CENTER_LEFT;-fx-padding:0 14;";
    }
}
