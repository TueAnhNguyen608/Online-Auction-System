package view;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.model.communication.CommandType;
import common.model.communication.ResponseCode;
import common.model.communication.ResponseMessage;
import common.model.user.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarHelper {

    private static final String BG_SIDEBAR = "#0c1032";
    private static final String BORDER     = "#2d3270";
    private static final String TEXT       = "#f0f4ff";
    private static final String MUTED      = "#8899bb";

    // ── Overload không cần client ────────────────────────────────
    public static VBox build(User user, String[][] menuItems, int activeIndex,
                             Consumer<Integer> onMenuClick) {
        return build(user, null, menuItems, activeIndex, onMenuClick);
    }

    public static VBox build(User user, AuctionClient client,
                             String[][] menuItems, int activeIndex,
                             Consumer<Integer> onMenuClick) {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(210);
        sidebar.setMinWidth(210);
        sidebar.setStyle("-fx-background-color:" + BG_SIDEBAR + ";"
                + "-fx-border-color:" + BORDER + ";-fx-border-width:0 1 0 0;");

        // Logo
        HBox logo = buildLogo();
        logo.setPadding(new Insets(20, 14, 16, 16));
        logo.setStyle("-fx-border-color:transparent transparent " + BORDER
                + " transparent;-fx-border-width:0 0 1 0;");

        // Menu
        VBox menu = new VBox(3);
        menu.setPadding(new Insets(14, 8, 0, 8));
        for (int i = 0; i < menuItems.length; i++) {
            final int idx = i;
            Button btn = new Button(menuItems[i][0] + "  " + menuItems[i][1]);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(42);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setFont(Font.font("Segoe UI",
                    idx == activeIndex ? FontWeight.BOLD : FontWeight.NORMAL, 13));
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
        Label lblBidCount = new Label("--");
        Label lblWonCount = new Label("--");
        VBox avatarCard = buildAvatarCard(user, lblBidCount, lblWonCount);

        sidebar.getChildren().addAll(logo, menu, spacer, sep, avatarCard);

        // Fetch số liệu thật
        if (client != null) fetchStats(client, lblBidCount, lblWonCount);

        return sidebar;
    }

    // ── Logo tự build ────────────────────────────────────────────
    private static HBox buildLogo() {
        HBox logo = new HBox(10);
        logo.setAlignment(Pos.CENTER_LEFT);
        ImageView gv = loadImage("/images/gavel.png", 36, 36);
        Label icon = gv != null ? new Label("", gv)
                : new Label("⚖") {{ setFont(Font.font(22)); }};
        VBox textBox = new VBox(1);
        Label l1 = new Label("HỆ THỐNG");
        l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l1.setTextFill(Color.web("#f59e0b"));
        Label l2 = new Label("ĐẤU GIÁ TRỰC TUYẾN");
        l2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        l2.setTextFill(Color.web("#c4cfe8"));
        textBox.getChildren().addAll(l1, l2);
        logo.getChildren().addAll(icon, textBox);
        return logo;
    }

    // ── Load ảnh ─────────────────────────────────────────────────
    private static ImageView loadImage(String path, double w, double h) {
        try {
            var stream = SidebarHelper.class.getResourceAsStream(path);
            if (stream == null) return null;
            ImageView iv = new ImageView(new Image(stream));
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Fetch stats từ server ────────────────────────────────────
    private static void fetchStats(AuctionClient client,
                                   Label lblBidCount, Label lblWonCount) {
        Gson gson = new Gson();
        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_MY_BID_HISTORY, null);
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    var t = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> list = gson.fromJson(gson.toJson(resp.getData()), t);
                    int count = list != null ? list.size() : 0;
                    Platform.runLater(() -> lblBidCount.setText(String.valueOf(count)));
                }
            } catch (Exception ignored) {}
        }).start();

        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_WON_PRODUCTS, null);
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    var t = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> list = gson.fromJson(gson.toJson(resp.getData()), t);
                    int count = list != null ? list.size() : 0;
                    Platform.runLater(() -> lblWonCount.setText(String.valueOf(count)));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    // ── Avatar card ──────────────────────────────────────────────
    private static VBox buildAvatarCard(User user,
                                        Label lblBidCount, Label lblWonCount) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 10, 16, 10));

        // Avatar circle
        StackPane avCircle = new StackPane();
        avCircle.setPrefSize(64, 64);
        avCircle.setMaxSize(64, 64);
        avCircle.setStyle("-fx-background-color:linear-gradient(to bottom right,#2d1b69,#2563eb);"
                + "-fx-background-radius:32;");
        ImageView avImg = loadImage("/images/avatar.png", 50, 50);
        if (avImg != null) {
            avCircle.getChildren().add(avImg);
        } else {
            Label emo = new Label("👤");
            emo.setFont(Font.font(26));
            avCircle.getChildren().add(emo);
        }

        // Tên
        String uname = (user != null && user.getUsername() != null)
                ? user.getUsername() : "Người dùng";
        Label lblName = new Label(uname);
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblName.setTextFill(Color.web(TEXT));

        // ✅ FIX: Chuyển UserRole thành String
        String roleStr = "";
        if (user != null && user.getRole() != null) {
            roleStr = user.getRole().name();
        }
        Label lblRole = new Label(formatRole(roleStr));
        lblRole.setFont(Font.font("Segoe UI", 11));
        lblRole.setTextFill(Color.web(MUTED));
        lblRole.setStyle("-fx-background-color:rgba(37,99,235,0.12);"
                + "-fx-background-radius:20;-fx-padding:2 8;");

        // Stats
        lblBidCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblBidCount.setTextFill(Color.web(TEXT));
        Label lblBidLabel = new Label("Đấu giá");
        lblBidLabel.setFont(Font.font("Segoe UI", 10));
        lblBidLabel.setTextFill(Color.web(MUTED));
        VBox bidBox = new VBox(2, lblBidCount, lblBidLabel);
        bidBox.setAlignment(Pos.CENTER);

        Separator divider = new Separator(javafx.geometry.Orientation.VERTICAL);
        divider.setStyle("-fx-background-color:#2d3270;");

        lblWonCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblWonCount.setTextFill(Color.web(TEXT));
        Label lblWonLabel = new Label("Đã thắng");
        lblWonLabel.setFont(Font.font("Segoe UI", 10));
        lblWonLabel.setTextFill(Color.web(MUTED));
        VBox wonBox = new VBox(2, lblWonCount, lblWonLabel);
        wonBox.setAlignment(Pos.CENTER);

        HBox stats = new HBox(10, bidBox, divider, wonBox);
        stats.setAlignment(Pos.CENTER);
        stats.setPadding(new Insets(4, 0, 0, 0));

        card.getChildren().addAll(avCircle, lblName, lblRole, stats);
        return card;
    }

    private static String formatRole(String role) {
        if (role == null || role.isEmpty()) return "";
        switch (role.toUpperCase()) {
            case "ADMIN":
                return "Quản trị viên";
            case "SELLER":
                return "Người bán";
            case "BIDDER":
                return "Người đấu giá";
            default:
                return role;
        }
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