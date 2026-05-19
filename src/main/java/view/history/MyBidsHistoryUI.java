package view.history;

import client.AuctionClient;
import common.CommandType;
import common.ResponseCode;
import common.ResponseMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import common.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

public class MyBidsHistoryUI {

    private static final String BG     = "#0a0e27";
    private static final String BG_ROW = "#0e1232";
    private static final String BG_ALT = "#111640";
    private static final String BORDER = "#2d3270";
    private static final String TEXT   = "#f0f4ff";
    private static final String MUTED  = "#8899bb";
    private static final String HEAD   = "#c4cfe8";
    private static final String BLUE   = "#2563eb";

    private AuctionClient client;
    private User currentUser;
    private VBox tableBox;
    private Gson gson = new Gson();

    // Không dùng dữ liệu mẫu — chỉ hiện data thật từ server

    public void show(AuctionClient client, User user) {
        this.client = client;
        this.currentUser = user;
        Stage stage = new Stage();
        stage.setTitle("Lịch sử đấu giá của tôi");

        HBox root = new HBox(0);
        root.setPrefSize(920, 600);
        root.setStyle("-fx-background-color:" + BG + ";");

        // Sidebar
        String[][] menu = {{"📋","Lịch sử đấu giá"},{"🏆","Sản phẩm đã thắng"},{"📈","Biểu đồ giá"}};
        VBox sidebar = SidebarHelper.build(currentUser, menu, 0, idx -> {
            if (idx == 1) { new WonProductsUI().show(client, user); stage.close(); }
            else if (idx == 2) { new PriceChartUI().show(client, "", "Chọn sản phẩm", user); stage.close(); }
        });

        // Content
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:" + BG + ";");
        content.setPadding(new Insets(24, 28, 24, 28));
        HBox.setHgrow(content, Priority.ALWAYS);

        // Header
        Label t = new Label("Lịch sử đấu giá của tôi");
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20)); t.setTextFill(Color.web(TEXT));
        Label sub = new Label("Xem lại tất cả các phiên đấu giá bạn đã tham gia");
        sub.setFont(Font.font("Segoe UI", 12)); sub.setTextFill(Color.web(MUTED));
        VBox.setMargin(sub, new Insets(3, 0, 18, 0));

        // Filter + search
        HBox filterRow = buildFilterRow();
        VBox.setMargin(filterRow, new Insets(0, 0, 14, 0));

        // Table
        tableBox = buildTableShell();
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        // Pagination
        HBox paging = buildPaging();
        VBox.setMargin(paging, new Insets(14, 0, 0, 0));

        content.getChildren().addAll(t, sub, filterRow, tableBox, paging);
        root.getChildren().addAll(sidebar, content);

        Scene scene = new Scene(root, 920, 600);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_MY_BID_HISTORY, null);
                Platform.runLater(() -> {
                    if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                        List<Map<String, Object>> rows = parseList(resp.getData());
                        if (rows != null && !rows.isEmpty()) {
                            refreshFromAPI(rows);
                        } else {
                            showEmpty("Bạn chưa tham gia đấu giá nào.");
                        }
                    } else {
                        showEmpty("Không thể tải dữ liệu. Vui lòng thử lại.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showEmpty("Lỗi kết nối server."));
            }
        }).start();
    }

    private void showEmpty(String msg) {
        tableBox.getChildren().clear();
        tableBox.getChildren().add(buildTableHeader());
        Label lbl = new Label("📭  " + msg);
        lbl.setFont(Font.font("Segoe UI", 13));
        lbl.setTextFill(Color.web(MUTED));
        lbl.setPadding(new Insets(32));
        tableBox.getChildren().add(lbl);
    }

    private void refreshFromAPI(List<Map<String, Object>> rows) {
        tableBox.getChildren().clear();
        tableBox.getChildren().add(buildTableHeader());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> r = rows.get(i);
            String name   = getStr(r, "itemName", "name");
            // finalPrice từ DB (i.current_price), fallback về amount nếu chưa có
            String final_ = fmtP(r, "finalPrice", "final_price", "currentPrice", "amount");
            String mine   = fmtP(r, "amount", "bidAmount");
            // Lấy trạng thái từ winning + itemStatus thay vì status/bidStatus
            String status = deriveBidStatus(r);
            String time   = getStr(r, "bidTime", "createdAt");
            String icon   = pickIcon(name);
            tableBox.getChildren().add(buildRow(new Object[]{icon, name, final_, mine, status, time}, i % 2 == 1));
        }
    }

    /**
     * Phân tích trạng thái bid dựa trên winning + itemStatus:
     * - ACTIVE + winning=true  → "Đang dẫn đầu"
     * - ACTIVE + winning=false → "Bị vượt giá"
     * - ENDED  + winning=true  → "Đã thắng"
     * - ENDED  + winning=false → "Thua"
     */
    private String deriveBidStatus(Map<String, Object> r) {
        Object winningObj = r.get("winning");
        boolean winning = winningObj != null &&
                (Boolean.TRUE.equals(winningObj) || "true".equalsIgnoreCase(winningObj.toString()));
        String itemStatus = getStr(r, "itemStatus", "item_status");
        if ("ENDED".equalsIgnoreCase(itemStatus)) {
            return winning ? "Đã thắng" : "Thua";
        } else {
            // ACTIVE hoặc chưa biết
            return winning ? "Đang dẫn đầu" : "Bị vượt giá";
        }
    }

    private HBox buildFilterRow() {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup tg = new ToggleGroup();
        for (String lbl : new String[]{"Tất cả","Đang diễn ra","Đã kết thúc","Đã hủy"}) {
            ToggleButton tb = new ToggleButton(lbl); tb.setToggleGroup(tg);
            tb.setFont(Font.font("Segoe UI", 12)); tb.setCursor(Cursor.HAND);
            tb.setStyle(tabIdle());
            tb.selectedProperty().addListener((o, a, n) -> tb.setStyle(n ? tabActive() : tabIdle()));
            row.getChildren().add(tb);
        }
        ((ToggleButton) row.getChildren().get(0)).setSelected(true);

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        TextField tf = new TextField(); tf.setPromptText("🔍  Tìm kiếm sản phẩm...");
        tf.setPrefWidth(180); tf.setPrefHeight(34);
        tf.setStyle("-fx-background-color:#11153a;-fx-border-color:" + BORDER
                + ";-fx-border-radius:8;-fx-background-radius:8;-fx-text-fill:" + TEXT
                + ";-fx-prompt-text-fill:#4a5a7a;-fx-font-size:12;-fx-padding:0 12;");
        row.getChildren().addAll(sp, tf);
        return row;
    }

    private VBox buildTableShell() {
        VBox box = new VBox(0);
        box.setStyle("-fx-background-color:#0e1232;-fx-background-radius:12;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:12;");
        box.getChildren().add(buildTableHeader());
        Label loading = new Label("⏳  Đang tải..."); loading.setTextFill(Color.web(MUTED));
        loading.setFont(Font.font("Segoe UI", 13)); loading.setPadding(new Insets(24));
        box.getChildren().add(loading);
        return box;
    }

    private HBox buildTableHeader() {
        HBox h = new HBox(); h.setPadding(new Insets(12, 16, 12, 16));
        h.setStyle("-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;");
        h.getChildren().addAll(hc("Sản phẩm",200),hc("Giá cuối cùng",130),hc("Giá bạn trả",130),hc("Trạng thái",110),hc("Thời gian",110));
        return h;
    }

    private Label hc(String t, double w) {
        Label l = new Label(t); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l.setTextFill(Color.web(HEAD)); l.setPrefWidth(w); return l;
    }

    private HBox buildRow(Object[] d, boolean alt) {
        HBox row = new HBox(); row.setPadding(new Insets(11, 16, 11, 16)); row.setAlignment(Pos.CENTER_LEFT);
        String base = "-fx-background-color:" + (alt ? BG_ALT : BG_ROW)
                + ";-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;";
        row.setStyle(base); row.setCursor(Cursor.HAND);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:rgba(37,99,235,0.07);"
                + "-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle(base));

        // Product col: icon + name
        Label emoLbl = new Label((String) d[0]); emoLbl.setFont(Font.font(22));
        StackPane iconWrap = new StackPane(emoLbl);
        iconWrap.setPrefSize(40, 40); iconWrap.setMinSize(40, 40);
        iconWrap.setStyle("-fx-background-color:#1a1f45;-fx-background-radius:8;");

        Label nameLbl = new Label((String) d[1]); nameLbl.setFont(Font.font("Segoe UI", 12));
        nameLbl.setTextFill(Color.web(TEXT)); nameLbl.setWrapText(true);
        VBox nameBox = new VBox(nameLbl); nameBox.setPrefWidth(150); nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox prodCol = new HBox(10, iconWrap, nameBox); prodCol.setAlignment(Pos.CENTER_LEFT); prodCol.setPrefWidth(200);

        Label fPrice = new Label((String) d[2]); fPrice.setFont(Font.font("Segoe UI", 12)); fPrice.setTextFill(Color.web(TEXT)); fPrice.setPrefWidth(130);

        String st = (String) d[4];
        Label mPrice = new Label((String) d[3]); mPrice.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); mPrice.setPrefWidth(130);
        switch(st) {
            case "Đã thắng":
            case "Đang dẫn đầu": mPrice.setTextFill(Color.web("#10b981")); break;
            case "Thua":         mPrice.setTextFill(Color.web("#ef4444")); break;
            default:             mPrice.setTextFill(Color.web("#f59e0b")); break;
        }

        Label badge = badge(st); badge.setPrefWidth(110);
        Label time = new Label((String) d[5]); time.setFont(Font.font("Segoe UI", 11)); time.setTextFill(Color.web(MUTED)); time.setPrefWidth(110); time.setWrapText(true);

        row.getChildren().addAll(prodCol, fPrice, mPrice, badge, time);
        return row;
    }

    private Label badge(String st) {
        Label l = new Label(st); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); l.setPadding(new Insets(3, 10, 3, 10));
        switch(st) {
            case "Đã thắng":        l.setTextFill(Color.web("#10b981")); l.setStyle("-fx-background-color:rgba(16,185,129,0.15);-fx-background-radius:20;"); break;
            case "Thua":            l.setTextFill(Color.web("#ef4444")); l.setStyle("-fx-background-color:rgba(239,68,68,0.15);-fx-background-radius:20;"); break;
            case "Đang dẫn đầu":   l.setTextFill(Color.web("#10b981")); l.setStyle("-fx-background-color:rgba(16,185,129,0.12);-fx-background-radius:20;"); break;
            case "Bị vượt giá":    l.setTextFill(Color.web("#f59e0b")); l.setStyle("-fx-background-color:rgba(245,158,11,0.15);-fx-background-radius:20;"); break;
            default:               l.setTextFill(Color.web(MUTED)); l.setStyle("-fx-background-color:rgba(136,153,187,0.15);-fx-background-radius:20;"); break;
        }
        return l;
    }

    private HBox buildPaging() {
        HBox row = new HBox(6); row.setAlignment(Pos.CENTER);
        ToggleGroup pg = new ToggleGroup();
        row.getChildren().addAll(pgBtn("‹"), pgNum("1",pg,true), pgNum("2",pg,false),
                pgNum("3",pg,false), new Label("..."){{ setTextFill(Color.web(MUTED)); }},
                pgNum("10",pg,false), pgBtn("›"));
        return row;
    }

    private Button pgBtn(String t) { Button b = new Button(t); b.setPrefSize(32,32); b.setFont(Font.font("Segoe UI",14)); b.setCursor(Cursor.HAND); b.setStyle("-fx-background-color:#11153a;-fx-border-color:"+BORDER+";-fx-border-radius:8;-fx-background-radius:8;-fx-text-fill:"+MUTED+";"); return b; }
    private ToggleButton pgNum(String t, ToggleGroup g, boolean sel) { ToggleButton b = new ToggleButton(t); b.setToggleGroup(g); b.setPrefSize(32,32); b.setFont(Font.font("Segoe UI",12)); b.setCursor(Cursor.HAND); b.setSelected(sel); String base="-fx-border-radius:8;-fx-background-radius:8;"; b.setStyle(sel?base+"-fx-background-color:"+BLUE+";-fx-text-fill:white;-fx-border-color:transparent;":base+"-fx-background-color:#11153a;-fx-text-fill:"+MUTED+";-fx-border-color:"+BORDER+";"); b.selectedProperty().addListener((o,a,n)->b.setStyle(n?base+"-fx-background-color:"+BLUE+";-fx-text-fill:white;-fx-border-color:transparent;":base+"-fx-background-color:#11153a;-fx-text-fill:"+MUTED+";-fx-border-color:"+BORDER+";")); return b; }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(Object data) { try { Type t = new TypeToken<List<Map<String,Object>>>(){}.getType(); return gson.fromJson(gson.toJson(data), t); } catch (Exception e) { return new ArrayList<>(); } }
    private String getStr(Map<String,Object> m, String... keys) { for(String k:keys){Object v=m.get(k);if(v!=null)return v.toString();} return "--"; }
    private String fmtP(Map<String,Object> m, String... keys) { for(String k:keys){Object v=m.get(k);if(v!=null){try{return String.format("%,.0f đ",Double.parseDouble(v.toString()));}catch(Exception e){return v.toString();}}} return "--"; }
    private String toVN(String s) { if(s==null)return "--"; switch(s.toUpperCase()){case"WON":case"WIN":return"Đã thắng";case"LOST":case"LOSE":return"Thua";case"OUTBID":return"Bị vượt giá";default:return s;} }
    private String pickIcon(String n) { if(n==null)return"📦"; String l=n.toLowerCase(); if(l.contains("iphone")||l.contains("samsung"))return"📱"; if(l.contains("macbook")||l.contains("laptop"))return"💻"; if(l.contains("canon")||l.contains("camera"))return"📷"; if(l.contains("rolex")||l.contains("watch"))return"⌚"; if(l.contains("playstation")||l.contains("xbox"))return"🎮"; return"📦"; }
    private String tabActive() { return "-fx-background-color:"+BLUE+";-fx-text-fill:white;-fx-background-radius:8;-fx-border-color:transparent;-fx-padding:6 14;-fx-cursor:hand;"; }
    private String tabIdle()   { return "-fx-background-color:#11153a;-fx-text-fill:"+MUTED+";-fx-background-radius:8;-fx-border-color:"+BORDER+";-fx-padding:6 14;-fx-cursor:hand;"; }
}
