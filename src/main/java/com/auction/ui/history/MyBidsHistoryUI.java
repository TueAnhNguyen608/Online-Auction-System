package com.auction.ui.history;

import com.auction.client.AuctionClient;
import com.auction.ui.bidding.MainDashboardUI;
import com.auction.common.model.communication.CommandType;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.auction.common.model.user.User;
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
import com.auction.ui.components.SidebarHelper;
import com.auction.ui.auth.SessionManager;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class MyBidsHistoryUI {

    private static final String BG_DEEP    = "#07101f";
    private static final String BG_MAIN    = "#0a1625";
    private static final String BG_CARD    = "#0d1e35";
    private static final String BG_ROW_ALT = "#0b1a2e";
    private static final String BG_HEADER  = "#0a1829";
    private static final String BORDER     = "#1a3055";
    private static final String GOLD       = "#F5C518";
    private static final String TEXT_PRI   = "#f0f6ff";
    private static final String TEXT_SEC   = "#c5d5e8";
    private static final String TEXT_MUTED = "#6b84a8";
    private static final String GREEN      = "#22c55e";
    private static final String GREEN_DIM  = "#4ade80";
    private static final String RED        = "#ef4444";
    private static final String AMBER      = "#f59e0b";
    private static final String HOVER_ROW  = "#1a3a5c";

    private AuctionClient client;
    private User          currentUser;
    private Stage         stage;
    private VBox          tableBox;
    private final Gson    gson = new Gson();

    private List<Map<String,Object>> allRows     = new ArrayList<>();
    private String                   currentFilter = "Tất cả";
    private String                   currentSearch  = "";

    private Label statTotal, statLead, statLose, statAuto;

    public void show(AuctionClient client, User user) {
        show(new Stage(), client, user);
    }

    public void show(Stage stage, AuctionClient client, User user) {
        this.stage       = stage;
        this.client      = client;
        this.currentUser = user;
        stage.setTitle("Lịch sử đấu giá của tôi");

        HBox root = new HBox(0);
        root.setStyle("-fx-background-color:" + BG_MAIN + ";");

        String[][] menu = {
                {"📋","Lịch sử đấu giá"},
                {"🏆","Sản phẩm đã thắng"},
                {"📈","Biểu đồ giá"}
        };
        VBox sidebar = SidebarHelper.build(user, client, menu, 0, idx -> {
            if (idx == 1) new WonProductsUI().show(stage, client, user);
            else if (idx == 2) new PriceChartUI().show(stage, client, "", "Chọn sản phẩm", user);
        });

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:" + BG_MAIN + ";");
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox topContent = new HBox();
        topContent.setAlignment(Pos.CENTER_LEFT);
        topContent.setPadding(new Insets(0, 28, 0, 28));
        topContent.setStyle("-fx-background-color:" + BG_DEEP
                + ";-fx-border-color:transparent transparent " + BORDER
                + " transparent;-fx-border-width:0 0 1 0;-fx-min-height:52;-fx-pref-height:52;");
        Button btnBack = buildBackButton();
        Region spTop = new Region(); HBox.setHgrow(spTop, Priority.ALWAYS);
        Label userLbl = new Label(user != null ? "👤  " + user.getUsername() : "");
        userLbl.setStyle("-fx-text-fill:" + TEXT_MUTED + ";-fx-font-size:12px;");
        topContent.getChildren().addAll(btnBack, spTop, userLbl);

        VBox main = new VBox(16);
        main.setPadding(new Insets(24, 28, 24, 28));
        VBox.setVgrow(main, Priority.ALWAYS);

        HBox iconBox = new HBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setStyle("-fx-background-color:rgba(245,197,24,0.10);"
                + "-fx-background-radius:8;-fx-border-color:rgba(245,197,24,0.25);"
                + "-fx-border-radius:8;-fx-padding:8;-fx-pref-width:40;-fx-pref-height:40;");
        iconBox.getChildren().add(new Label("📋") {{ setStyle("-fx-font-size:18px;"); }});
        Label titleLbl = new Label("Lịch sử đấu giá của tôi");
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        titleLbl.setTextFill(Color.web(TEXT_PRI));
        Label subLbl = new Label("Tất cả các phiên đấu giá bạn đã tham gia");
        subLbl.setFont(Font.font("Segoe UI", 12));
        subLbl.setTextFill(Color.web(TEXT_MUTED));
        VBox titleBox = new VBox(3, titleLbl, subLbl);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        HBox pageHeader = new HBox(12, iconBox, titleBox);
        pageHeader.setAlignment(Pos.CENTER_LEFT);

        HBox statsRow = buildStatsRow();

        Label badgeNote = new Label("🤖 = Auto-bid (hệ thống tự đặt)   👤 = Thủ công (bạn tự đặt)");
        badgeNote.setFont(Font.font("Segoe UI", 11));
        badgeNote.setTextFill(Color.web(TEXT_MUTED));

        HBox filterRow = buildFilterRow();

        tableBox = buildTableShell();
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        main.getChildren().addAll(pageHeader, statsRow, badgeNote, filterRow, tableBox);
        content.getChildren().addAll(topContent, main);
        root.getChildren().addAll(sidebar, content);

        double w = stage.getScene() != null ? stage.getScene().getWidth()  : 1280;
        double h = stage.getScene() != null ? stage.getScene().getHeight() : 720;
        stage.setScene(new Scene(root, w, h));
        if (!stage.isShowing()) { stage.setMaximized(true); stage.show(); }

        loadData();
    }

    private HBox buildStatsRow() {
        statTotal = new Label("--");
        statLead  = new Label("--");
        statLose  = new Label("--");
        statAuto  = new Label("--");
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
                mkStatCard("Tổng lần đấu",   statTotal, GOLD),
                mkStatCard("Đang dẫn đầu",   statLead,  GREEN),
                mkStatCard("Bị vượt / Thua", statLose,  RED),
                mkStatCard("Auto-bid",        statAuto,  AMBER)
        );
        return row;
    }

    private VBox mkStatCard(String label, Label valLbl, String color) {
        valLbl.setStyle("-fx-text-fill:" + color + ";-fx-font-size:22px;-fx-font-weight:bold;");
        Label lblLbl = new Label(label);
        lblLbl.setStyle("-fx-text-fill:" + TEXT_MUTED + ";-fx-font-size:11px;-fx-font-weight:bold;");
        VBox card = new VBox(4, lblLbl, valLbl);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color:" + BG_CARD + ";-fx-background-radius:10;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:10;"
                + "-fx-padding:14 20 14 20;-fx-pref-width:170;");
        return card;
    }

    private void loadData() {
        new Thread(() -> {
            List<Map<String,Object>> localRows = new ArrayList<>();
            if (currentUser != null) {
                for (SessionManager.AutoBidHistoryEntry e :
                        SessionManager.getAutoBidHistory(currentUser.getUsername())) {
                    Map<String,Object> m = new HashMap<>();
                    m.put("itemName",   e.itemName);
                    m.put("itemId",     e.itemId);
                    m.put("amount",     stripNum(e.bidAmount));
                    m.put("finalPrice", stripNum(e.currentPrice));
                    m.put("bidTime",    e.bidTime);
                    m.put("autoBid",    "true");
                    m.put("itemStatus", "ACTIVE");
                    localRows.add(m);
                }
            }
            List<Map<String,Object>> serverRows = new ArrayList<>();
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_MY_BID_HISTORY, null);
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    List<Map<String,Object>> parsed = parseList(resp.getData());
                    if (parsed != null) serverRows = parsed;
                }
            } catch (Exception e) { System.out.println("⚠ " + e.getMessage()); }

            Set<String> localIds = new HashSet<>();
            for (Map<String,Object> lr : localRows) {
                Object id = lr.get("itemId"); if (id != null) localIds.add(id.toString());
            }
            List<Map<String,Object>> combined = new ArrayList<>(localRows);
            for (Map<String,Object> sr : serverRows) {
                String sid = getStr(sr, "itemId", "item_id", "id");
                if (!localIds.contains(sid)) combined.add(sr);
            }
            final List<Map<String,Object>> fc = combined;
            Platform.runLater(() -> {
                if (fc.isEmpty()) showEmpty("Bạn chưa tham gia đấu giá nào.");
                else { allRows = fc; applyFilter(); }
            });
        }).start();
    }

    private String stripNum(String s) { return s == null ? "0" : s.replaceAll("[^0-9]", ""); }

    private void applyFilter() {
        List<Map<String,Object>> filtered = allRows.stream().filter(r -> {
            String status = deriveStatus(r);
            String name   = getStr(r, "itemName", "name").toLowerCase();
            boolean mt;
            switch (currentFilter) {
                case "Đang diễn ra": mt = status.equals("Đang dẫn đầu") || status.equals("Bị vượt giá"); break;
                case "Đã kết thúc":  mt = status.equals("Đã thắng") || status.equals("Thua"); break;
                default: mt = true;
            }
            return mt && (currentSearch.isEmpty() || name.contains(currentSearch.toLowerCase()));
        }).collect(Collectors.toList());
        updateStats(allRows);
        if (filtered.isEmpty()) showEmpty("Không có dữ liệu phù hợp.");
        else renderRows(filtered);
    }

    private void updateStats(List<Map<String,Object>> rows) {
        if (statTotal == null) return;
        long total = rows.size();
        long lead  = rows.stream().filter(r -> { String s = deriveStatus(r); return s.equals("Đang dẫn đầu") || s.equals("Đã thắng"); }).count();
        long lose  = rows.stream().filter(r -> { String s = deriveStatus(r); return s.equals("Bị vượt giá") || s.equals("Thua"); }).count();
        long auto  = rows.stream().filter(r -> {
            Object ia = r.get("autoBid"); if (ia == null) ia = r.get("isAutoBid"); if (ia == null) ia = r.get("auto_bid");
            return ia != null && (Boolean.TRUE.equals(ia) || "true".equalsIgnoreCase(ia.toString()) || "AUTO".equalsIgnoreCase(ia.toString()));
        }).count();
        statTotal.setText(String.valueOf(total));
        statLead.setText(String.valueOf(lead));
        statLose.setText(String.valueOf(lose));
        statAuto.setText(String.valueOf(auto));
    }

    private void showEmpty(String msg) {
        tableBox.getChildren().clear();
        tableBox.getChildren().add(buildTableHeader());
        Label l = new Label("📭  " + msg);
        l.setFont(Font.font("Segoe UI", 13)); l.setTextFill(Color.web(TEXT_MUTED)); l.setPadding(new Insets(32));
        tableBox.getChildren().add(l);
    }

    private void renderRows(List<Map<String,Object>> rows) {
        tableBox.getChildren().clear();
        tableBox.getChildren().add(buildTableHeader());
        for (int i = 0; i < rows.size(); i++) {
            Map<String,Object> r = rows.get(i);
            String name   = getStr(r, "itemName", "name");
            String finalP = fmtP(r, "finalPrice", "final_price", "currentPrice");
            String mine   = fmtP(r, "amount", "bidAmount");
            String status = deriveStatus(r);
            Object ia = r.get("autoBid"); if (ia == null) ia = r.get("isAutoBid"); if (ia == null) ia = r.get("auto_bid");
            boolean isAuto = ia != null && (Boolean.TRUE.equals(ia) || "true".equalsIgnoreCase(ia.toString()) || "AUTO".equalsIgnoreCase(ia.toString()));
            String time = formatTime(getStr(r, "bidTime", "createdAt", "bid_time"));
            tableBox.getChildren().add(buildRow(pickIcon(name), name, finalP, mine, isAuto ? "🤖" : "👤", status, time, i % 2 == 1));
        }
    }

    private VBox buildTableShell() {
        VBox box = new VBox(0);
        box.setStyle("-fx-background-color:" + BG_CARD + ";-fx-background-radius:12;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:12;");
        box.getChildren().add(buildTableHeader());
        Label loading = new Label("⏳  Đang tải...");
        loading.setTextFill(Color.web(TEXT_MUTED)); loading.setFont(Font.font("Segoe UI", 13)); loading.setPadding(new Insets(24));
        box.getChildren().add(loading); return box;
    }

    private HBox buildTableHeader() {
        HBox h = new HBox(); h.setPadding(new Insets(11, 16, 11, 16));
        h.setStyle("-fx-background-color:" + BG_HEADER + ";-fx-border-color:transparent transparent "
                + BORDER + " transparent;-fx-border-width:0 0 1 0;");
        h.getChildren().addAll(hc("Sản phẩm",220),hc("Giá cuối",115),hc("Giá bạn đặt",145),hc("Kiểu",50),hc("Trạng thái",120),hc("Thời gian",110));
        return h;
    }

    private Label hc(String t, double w) {
        Label l = new Label(t); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(Color.web(TEXT_MUTED)); l.setPrefWidth(w); return l;
    }

    private HBox buildRow(String icon, String name, String finalP, String mine,
                          String badgeType, String status, String time, boolean alt) {
        HBox row = new HBox(); row.setPadding(new Insets(11, 16, 11, 16)); row.setAlignment(Pos.CENTER_LEFT);
        String base = "-fx-background-color:" + (alt ? BG_ROW_ALT : BG_CARD)
                + ";-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;";
        row.setStyle(base); row.setCursor(Cursor.HAND);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:" + HOVER_ROW
                + ";-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle(base));

        Label ico = new Label(icon); ico.setFont(Font.font(18));
        StackPane iconWrap = new StackPane(ico); iconWrap.setPrefSize(36,36); iconWrap.setMinSize(36,36);
        iconWrap.setStyle("-fx-background-color:#0f2040;-fx-background-radius:8;-fx-border-color:" + BORDER + ";-fx-border-radius:8;");
        Label nameLbl = new Label(name); nameLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        nameLbl.setTextFill(Color.web(TEXT_PRI)); nameLbl.setWrapText(true);
        VBox nameBox = new VBox(nameLbl); nameBox.setPrefWidth(170); nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox prodCol = new HBox(10, iconWrap, nameBox); prodCol.setAlignment(Pos.CENTER_LEFT); prodCol.setPrefWidth(220);

        Label fPrice = new Label(finalP); fPrice.setFont(Font.font("Segoe UI", 12));
        fPrice.setTextFill(Color.web(TEXT_SEC)); fPrice.setPrefWidth(115);

        String priceColor;
        switch (status) {
            case "Đã thắng": case "Đang dẫn đầu": priceColor = GREEN; break;
            case "Thua":     priceColor = RED;   break;
            default:         priceColor = AMBER; break;
        }
        Label mPrice = new Label(mine); mPrice.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        mPrice.setTextFill(Color.web(priceColor)); mPrice.setPrefWidth(145);

        Label bLbl = new Label(badgeType); bLbl.setFont(Font.font("Segoe UI", 13));
        bLbl.setPrefWidth(50); bLbl.setAlignment(Pos.CENTER);

        Label sBadge = buildStatusBadge(status); sBadge.setPrefWidth(120);

        Label timeLbl = new Label(time); timeLbl.setFont(Font.font("Segoe UI", 11));
        timeLbl.setTextFill(Color.web(TEXT_MUTED)); timeLbl.setPrefWidth(110); timeLbl.setWrapText(true);

        row.getChildren().addAll(prodCol, fPrice, mPrice, bLbl, sBadge, timeLbl); return row;
    }

    private Label buildStatusBadge(String status) {
        String fg, bg, bc;
        switch (status) {
            case "Đã thắng":      fg=GREEN;     bg="rgba(34,197,94,0.12)";  bc="rgba(34,197,94,0.28)";  break;
            case "Đang dẫn đầu": fg=GREEN_DIM; bg="rgba(74,222,128,0.08)"; bc="rgba(74,222,128,0.2)";  break;
            case "Bị vượt giá":  fg=AMBER;     bg="rgba(245,158,11,0.10)"; bc="rgba(245,158,11,0.25)"; break;
            case "Thua":          fg=RED;       bg="rgba(239,68,68,0.10)";  bc="rgba(239,68,68,0.22)";  break;
            default:              fg=TEXT_MUTED;bg="rgba(107,132,168,0.1)"; bc="rgba(107,132,168,0.2)"; break;
        }
        Label dot = new Label("●"); dot.setStyle("-fx-text-fill:" + fg + ";-fx-font-size:8px;");
        Label lbl = new Label(status); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); lbl.setTextFill(Color.web(fg));
        HBox box = new HBox(5, dot, lbl); box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:20;-fx-border-color:" + bc + ";-fx-border-radius:20;-fx-padding:3 10 3 10;");
        Label wrap = new Label(); wrap.setGraphic(box); return wrap;
    }

    private HBox buildFilterRow() {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup tg = new ToggleGroup();
        for (String lbl : new String[]{"Tất cả","Đang diễn ra","Đã kết thúc"}) {
            ToggleButton tb = new ToggleButton(lbl); tb.setToggleGroup(tg);
            tb.setFont(Font.font("Segoe UI", 12)); tb.setCursor(Cursor.HAND); tb.setStyle(tabIdle());
            tb.selectedProperty().addListener((o,a,n) -> tb.setStyle(n ? tabActive() : tabIdle()));
            tb.setOnAction(e -> { currentFilter = lbl; if (!allRows.isEmpty()) applyFilter(); });
            row.getChildren().add(tb);
        }
        ((ToggleButton) row.getChildren().get(0)).setSelected(true);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        TextField tf = new TextField(); tf.setPromptText("🔍  Tìm sản phẩm..."); tf.setPrefWidth(200); tf.setPrefHeight(34);
        tf.setStyle("-fx-background-color:" + BG_CARD + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;-fx-text-fill:" + TEXT_PRI + ";-fx-prompt-text-fill:#3a5070;-fx-font-size:12;-fx-padding:0 12;");
        tf.textProperty().addListener((obs,ov,nv) -> { currentSearch = nv.trim(); if (!allRows.isEmpty()) applyFilter(); });
        row.getChildren().addAll(sp, tf); return row;
    }

    private Button buildBackButton() {
        Button btn = new Button("← Quay lại");
        btn.setFont(Font.font("Segoe UI", 12)); btn.setCursor(Cursor.HAND);
        String idle  = "-fx-background-color:transparent;-fx-text-fill:" + TEXT_MUTED + ";-fx-border-color:" + BORDER + ";-fx-border-radius:7;-fx-background-radius:7;-fx-padding:5 14;";
        String hover = "-fx-background-color:" + BG_CARD + ";-fx-text-fill:" + TEXT_PRI  + ";-fx-border-color:" + BORDER + ";-fx-border-radius:7;-fx-background-radius:7;-fx-padding:5 14;";
        btn.setStyle(idle);
        btn.setOnMouseEntered(e -> btn.setStyle(hover)); btn.setOnMouseExited(e -> btn.setStyle(idle));
        btn.setOnAction(e -> MainDashboardUI.show(stage,
                currentUser != null ? currentUser.getUsername() : "User"));
        return btn;
    }

    private String deriveStatus(Map<String,Object> r) {
        String itemStatus = getStr(r, "itemStatus", "item_status");
        try {
            Object ma = r.get("amount"); if (ma == null) ma = r.get("bidAmount");
            Object fp = r.get("finalPrice"); if (fp == null) fp = r.get("final_price"); if (fp == null) fp = r.get("currentPrice");
            if (ma != null && fp != null) {
                boolean lead = Double.parseDouble(ma.toString()) >= Double.parseDouble(fp.toString()) - 1.0;
                if ("ENDED".equalsIgnoreCase(itemStatus)) return lead ? "Đã thắng" : "Thua";
                else return lead ? "Đang dẫn đầu" : "Bị vượt giá";
            }
        } catch (Exception ignored) {}
        Object wo = r.get("winning"); if (wo == null) wo = r.get("isWinning");
        boolean win = wo != null && (Boolean.TRUE.equals(wo) || "true".equalsIgnoreCase(wo.toString()));
        if ("ENDED".equalsIgnoreCase(itemStatus)) return win ? "Đã thắng" : "Thua";
        return win ? "Đang dẫn đầu" : "Bị vượt giá";
    }

    private String formatTime(String raw) {
        if (raw == null || raw.equals("--")) return "--";
        try { if (raw.contains("T")) { String[] p=raw.split("T"); String[] d=p[0].split("-"); String t=p[1].length()>=5?p[1].substring(0,5):p[1]; return d[2]+"/"+d[1]+"/"+d[0]+"\n"+t; } return raw.replace(" ","\n"); } catch (Exception e) { return raw; }
    }
    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> parseList(Object data) { try { Type t = new TypeToken<List<Map<String,Object>>>(){}.getType(); return gson.fromJson(gson.toJson(data), t); } catch (Exception e) { return new ArrayList<>(); } }
    private String getStr(Map<String,Object> m, String... keys) { for (String k:keys){Object v=m.get(k);if(v!=null)return v.toString();} return "--"; }
    private String fmtP(Map<String,Object> m, String... keys) { for (String k:keys){Object v=m.get(k);if(v!=null){try{return String.format("%,.0f đ",Double.parseDouble(v.toString()));}catch(Exception e){return v.toString();}}} return "--"; }
    private String pickIcon(String n) { if(n==null)return"📦"; String l=n.toLowerCase(); if(l.contains("iphone")||l.contains("samsung")||l.contains("pixel"))return"📱"; if(l.contains("macbook")||l.contains("laptop")||l.contains("dell")||l.contains("asus")||l.contains("gram"))return"💻"; if(l.contains("canon")||l.contains("camera"))return"📷"; if(l.contains("watch"))return"⌚"; if(l.contains("playstation")||l.contains("xbox"))return"🎮"; if(l.contains("airpods")||l.contains("sony")||l.contains("bose"))return"🎧"; if(l.contains("ipad")||l.contains("tab"))return"📱"; if(l.contains("keyboard")||l.contains("mouse")||l.contains("magic")||l.contains("logitech"))return"⌨️"; return"📦"; }
    private String tabActive() { return "-fx-background-color:rgba(245,197,24,0.12);-fx-text-fill:"+GOLD+";-fx-background-radius:7;-fx-border-color:"+GOLD+";-fx-border-width:1;-fx-padding:6 14;-fx-cursor:hand;"; }
    private String tabIdle()   { return "-fx-background-color:"+BG_CARD+";-fx-text-fill:"+TEXT_MUTED+";-fx-background-radius:7;-fx-border-color:"+BORDER+";-fx-border-width:1;-fx-padding:6 14;-fx-cursor:hand;"; }
}