package view.history;

import client.AuctionClient;
import common.CommandType;
import common.ResponseCode;
import common.ResponseMessage;
import common.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import view.SidebarHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WonProductsUI {

    private static final String BG     = "#0a0e27";
    private static final String CARD   = "#0e1232";
    private static final String IMGBG  = "#1a1f45";
    private static final String BORDER = "#2d3270";
    private static final String TEXT   = "#f0f4ff";
    private static final String MUTED  = "#8899bb";
    private static final String GREEN  = "#10b981";

    private AuctionClient client;
    private User currentUser;
    private GridPane grid;
    private Stage stage;
    private Gson gson = new Gson();

    // ── Gọi với stage mới (tương thích ngược) ────────────────────
    public void show(AuctionClient client, User user) {
        show(new Stage(), client, user);
    }

    // ── Gọi với stage có sẵn (dùng chung 1 cửa sổ) ───────────────
    public void show(Stage stage, AuctionClient client, User user) {
        this.stage       = stage;
        this.client      = client;
        this.currentUser = user;
        stage.setTitle("Sản phẩm đã thắng");

        HBox root = new HBox(0);
        root.setStyle("-fx-background-color:" + BG + ";");

        String[][] menu = {
                {"📋","Lịch sử đấu giá"},
                {"🏆","Sản phẩm đã thắng"},
                {"📈","Biểu đồ giá"}
        };
        VBox sidebar = SidebarHelper.build(user, client, menu, 1, idx -> {
            if (idx == 0) new MyBidsHistoryUI().show(stage, client, user);
            else if (idx == 2) new PriceChartUI().show(stage, client, "", "Chọn sản phẩm", user);
        });

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:" + BG + ";");
        content.setPadding(new Insets(24, 28, 24, 28));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Nút quay lại
        Button btnBack = buildBackBtn();
        HBox backRow = new HBox(btnBack);
        VBox.setMargin(backRow, new Insets(0, 0, 12, 0));

        Label t = new Label("Sản phẩm đã thắng");
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        t.setTextFill(Color.web(TEXT));

        Button btnRefresh = new Button("🔄  Làm mới");
        btnRefresh.setFont(Font.font("Segoe UI", 12));
        btnRefresh.setCursor(Cursor.HAND);
        btnRefresh.setStyle("-fx-background-color:#1a1f45;-fx-text-fill:" + MUTED
                + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;"
                + "-fx-background-radius:8;-fx-padding:5 14;");
        btnRefresh.setOnAction(e -> loadData());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox titleRow = new HBox(12, t, sp, btnRefresh);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label sub = new Label("Danh sách các sản phẩm bạn đã thắng đấu giá");
        sub.setFont(Font.font("Segoe UI", 12));
        sub.setTextFill(Color.web(MUTED));
        VBox.setMargin(sub, new Insets(3, 0, 20, 0));

        grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(33.33);
        grid.getColumnConstraints().addAll(cc,
                new ColumnConstraints() {{ setPercentWidth(33.33); }},
                new ColumnConstraints() {{ setPercentWidth(33.33); }});
        VBox.setVgrow(grid, Priority.ALWAYS);

        Label loading = new Label("⏳  Đang tải sản phẩm đã thắng...");
        loading.setFont(Font.font("Segoe UI", 13));
        loading.setTextFill(Color.web(MUTED));
        grid.add(loading, 0, 0);

        content.getChildren().addAll(backRow, titleRow, sub, grid);
        root.getChildren().addAll(sidebar, content);

        double w = stage.getScene() != null ? stage.getScene().getWidth()  : 1280;
        double h = stage.getScene() != null ? stage.getScene().getHeight() : 720;
        stage.setScene(new Scene(root, w, h));
        if (!stage.isShowing()) { stage.setMaximized(true); stage.show(); }

        loadData();
    }

    private Button buildBackBtn() {
        Button b = new Button("← Quay lại");
        b.setFont(Font.font("Segoe UI", 12)); b.setCursor(Cursor.HAND);
        String idle  = "-fx-background-color:transparent;-fx-text-fill:" + MUTED
                + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;-fx-padding:5 14;";
        String hover = "-fx-background-color:#11153a;-fx-text-fill:" + TEXT
                + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;-fx-padding:5 14;";
        b.setStyle(idle);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(idle));
        b.setOnAction(e -> view.bidding.MainDashboardUI.show(stage,
                currentUser != null ? currentUser.getUsername() : "User"));
        return b;
    }

    private void loadData() {
        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_WON_PRODUCTS, null);
                Platform.runLater(() -> {
                    grid.getChildren().clear();
                    if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                        List<Map<String, Object>> products = parseList(resp.getData());
                        if (products != null && !products.isEmpty()) {
                            for (int i = 0; i < products.size(); i++) {
                                Map<String, Object> p = products.get(i);
                                String name  = getStr(p, "itemName", "name");
                                String price = fmtP(p, "finalPrice", "currentPrice", "winPrice", "amount");
                                String date  = shortDate(getStr(p, "wonAt", "endTime", "createdAt"));
                                grid.add(buildCard(pickIcon(name), name, price, date), i % 3, i / 3);
                            }
                        } else {
                            Label empty = new Label("📭  Bạn chưa thắng sản phẩm nào.");
                            empty.setFont(Font.font("Segoe UI", 13));
                            empty.setTextFill(Color.web(MUTED));
                            grid.add(empty, 0, 0);
                        }
                    } else {
                        Label err = new Label("📭  Không thể tải dữ liệu. Vui lòng thử lại.");
                        err.setFont(Font.font("Segoe UI", 13));
                        err.setTextFill(Color.web(MUTED));
                        grid.add(err, 0, 0);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private VBox buildCard(String icon, String name, String price, String date) {
        VBox card = new VBox(0);
        String base  = "-fx-background-color:" + CARD + ";-fx-background-radius:12;-fx-border-color:" + BORDER + ";-fx-border-radius:12;-fx-cursor:hand;";
        String hover = "-fx-background-color:#131840;-fx-background-radius:12;-fx-border-color:#1d4ed8;-fx-border-radius:12;-fx-cursor:hand;";
        card.setStyle(base); card.setCursor(Cursor.HAND);
        card.setOnMouseEntered(e -> card.setStyle(hover));
        card.setOnMouseExited(e  -> card.setStyle(base));

        StackPane imgArea = new StackPane();
        imgArea.setPrefHeight(120);
        imgArea.setStyle("-fx-background-color:" + IMGBG + ";-fx-background-radius:12 12 0 0;");
        Label emoLbl = new Label(icon); emoLbl.setFont(Font.font(44));
        imgArea.getChildren().add(emoLbl);

        VBox info = new VBox(5);
        info.setPadding(new Insets(12, 14, 14, 14));
        Label lName  = new Label(name);  lName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); lName.setTextFill(Color.web(TEXT)); lName.setWrapText(true);
        Label lPrice = new Label(price); lPrice.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); lPrice.setTextFill(Color.web(GREEN));
        Label lDate  = new Label("Thắng ngày " + date); lDate.setFont(Font.font("Segoe UI", 11)); lDate.setTextFill(Color.web(MUTED));
        info.getChildren().addAll(lName, lPrice, lDate);

        card.getChildren().addAll(imgArea, info);
        return card;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(Object data) { try { Type t = new TypeToken<List<Map<String,Object>>>(){}.getType(); return gson.fromJson(gson.toJson(data), t); } catch (Exception e) { return new ArrayList<>(); } }
    private String getStr(Map<String,Object> m, String... keys) { for(String k:keys){Object v=m.get(k);if(v!=null)return v.toString();} return "--"; }
    private String fmtP(Map<String,Object> m, String... keys) { for(String k:keys){Object v=m.get(k);if(v!=null){try{return String.format("%,.0f đ",Double.parseDouble(v.toString()));}catch(Exception e){return v.toString();}}} return "--"; }
    private String shortDate(String s) { if(s==null||s.equals("--"))return"--"; if(s.contains("T"))s=s.split("T")[0]; String[] p=s.split("-"); return p.length>=3?p[2]+"/"+p[1]+"/"+p[0]:s; }
    private String pickIcon(String n) { if(n==null)return"📦"; String l=n.toLowerCase(); if(l.contains("iphone")||l.contains("samsung")||l.contains("pixel"))return"📱"; if(l.contains("macbook")||l.contains("laptop")||l.contains("dell")||l.contains("asus")||l.contains("gram"))return"💻"; if(l.contains("canon")||l.contains("camera"))return"📷"; if(l.contains("watch"))return"⌚"; if(l.contains("playstation")||l.contains("xbox"))return"🎮"; if(l.contains("airpods")||l.contains("headphone")||l.contains("sony")||l.contains("bose"))return"🎧"; if(l.contains("ipad")||l.contains("tab"))return"📱"; if(l.contains("keyboard")||l.contains("mouse")||l.contains("magic")||l.contains("logitech"))return"⌨️"; return"📦"; }
}
