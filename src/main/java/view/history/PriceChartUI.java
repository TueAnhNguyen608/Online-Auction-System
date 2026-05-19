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
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import view.SidebarHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PriceChartUI {

    private static final String BG     = "#0a0e27";
    private static final String PANEL  = "#0e1232";
    private static final String BG_IN  = "#1a1f45";
    private static final String BORDER = "#2d3270";
    private static final String TEXT   = "#f0f4ff";
    private static final String MUTED  = "#8899bb";
    private static final String BLUE = "#7c3aed";

    private AuctionClient client;
    private User currentUser;
    private String itemId;
    private String itemName;
    private String currentRange = "7 ngày";
    private Canvas chartCanvas;
    private Label lblStart, lblCurrent, lblHigh, lblTotal;
    private Gson gson = new Gson();

    private double[] priceData = {10,11.5,13,14,12.5,15,17,16,19,21,20,22.5,25,24,27,30,28,32,31,34,33,36,38,35,37,40,38,36,34,32,33,35,34,32.5};
    private String[] xLabels   = {"14/05","15/05","16/05","17/05","18/05","19/05","20/05"};

    public void show(AuctionClient client, String itemId, String itemName, User user) {
        this.currentUser = user;   // ← Fix: không còn null nữa
        this.client = client; this.itemId = itemId; this.itemName = itemName;
        Stage stage = new Stage();
        stage.setTitle("Biểu đồ giá – " + itemName);

        HBox root = new HBox(0);
        root.setPrefSize(980, 620);
        root.setStyle("-fx-background-color:" + BG + ";");

        String[][] menu = {{"📋","Lịch sử đấu giá"},{"🏆","Sản phẩm đã thắng"},{"📈","Biểu đồ giá"}};
        VBox sidebar = SidebarHelper.build(currentUser, menu, 2, idx -> {
            if (idx == 0) { new MyBidsHistoryUI().show(client, currentUser); stage.close(); }
            else if (idx == 1) { new WonProductsUI().show(client, currentUser); stage.close(); }
        });

        VBox content = buildContent();
        HBox.setHgrow(content, Priority.ALWAYS);

        root.getChildren().addAll(sidebar, content);

        Scene scene = new Scene(root, 980, 620);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        loadData();
    }

    private VBox buildContent() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color:" + BG + ";");
        panel.setPadding(new Insets(24, 28, 24, 28));

        // Header
        HBox header = new HBox(); header.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(header, new Insets(0,0,18,0));
        VBox titleBox = new VBox(3);
        Label t = new Label("Biểu đồ lịch sử giá"); t.setFont(Font.font("Segoe UI",FontWeight.BOLD,20)); t.setTextFill(Color.web(TEXT));
        Label sub = new Label("Theo dõi biến động giá của sản phẩm"); sub.setFont(Font.font("Segoe UI",12)); sub.setTextFill(Color.web(MUTED));
        titleBox.getChildren().addAll(t, sub);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // ComboBox - Load danh sách items thật từ server
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().add(itemName != null && !itemName.isEmpty() ? itemName : "Chọn sản phẩm");
        cb.setValue(itemName != null && !itemName.isEmpty() ? itemName : "Chọn sản phẩm");
        cb.setPrefHeight(36); cb.setPrefWidth(210);
        cb.setStyle("-fx-background-color:"+BG_IN+";-fx-border-color:"+BORDER+";-fx-border-radius:8;-fx-background-radius:8;-fx-font-size:12;");
        // Load items thật từ server vào combobox
        loadItemsIntoComboBox(cb, stage);
        header.getChildren().addAll(titleBox, sp, cb);

        // Range buttons
        HBox rangeRow = new HBox(4); rangeRow.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(rangeRow, new Insets(0,0,14,0));
        ToggleGroup tg = new ToggleGroup();
        for (String r : new String[]{"1 giờ","1 ngày","7 ngày","30 ngày","Tất cả"}) {
            ToggleButton btn = new ToggleButton(r); btn.setToggleGroup(tg);
            btn.setFont(Font.font("Segoe UI",12)); btn.setCursor(Cursor.HAND); btn.setPrefHeight(32);
            btn.setStyle(rIdle());
            btn.selectedProperty().addListener((o,a,n)->btn.setStyle(n?rActive():rIdle()));
            btn.setOnAction(e -> { currentRange=r; drawChart(chartCanvas); });
            if (r.equals("7 ngày")) btn.setSelected(true);
            rangeRow.getChildren().add(btn);
        }

        // Chart
        chartCanvas = new Canvas(714, 260);
        drawChart(chartCanvas);
        StackPane chartWrap = new StackPane(chartCanvas);
        chartWrap.setStyle("-fx-background-color:"+PANEL+";-fx-background-radius:12;-fx-border-color:"+BORDER+";-fx-border-radius:12;");
        chartWrap.setPadding(new Insets(16));
        VBox.setMargin(chartWrap, new Insets(0,0,16,0));

        // Stats
        HBox statsRow = buildStats();

        panel.getChildren().addAll(header, rangeRow, chartWrap, statsRow);
        return panel;
    }

    private void drawChart(Canvas canvas) {
        if (priceData == null || priceData.length == 0) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w=canvas.getWidth(), h=canvas.getHeight();
        gc.setFill(Color.web(PANEL)); gc.fillRect(0,0,w,h);

        double padL=55,padR=18,padT=18,padB=46;
        double cW=w-padL-padR, cH=h-padT-padB;

        double maxVal=0; for(double d:priceData) if(d>maxVal)maxVal=d;
        maxVal=Math.ceil(maxVal/10_000_000.0)*10_000_000; if(maxVal==0)maxVal=50_000_000;

        // Grid + Y labels
        int steps=5; gc.setFont(Font.font("Segoe UI",11));
        for(int i=0;i<=steps;i++){
            double tick=maxVal*i/steps; double y=padT+cH-(tick/maxVal)*cH;
            gc.setStroke(Color.web(BORDER)); gc.setLineWidth(0.7); gc.strokeLine(padL,y,padL+cW,y);
            gc.setFill(Color.web(MUTED)); gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(tick==0?"0":(long)(tick/1_000_000)+"M", padL-7, y+4);
        }

        // X labels
        if(xLabels!=null&&xLabels.length>0){
            gc.setFill(Color.web(MUTED)); gc.setTextAlign(TextAlignment.CENTER);
            for(int i=0;i<xLabels.length;i++){
                double x=padL+(i/(double)(xLabels.length-1))*cW;
                gc.fillText(xLabels[i],x,padT+cH+18);
            }
        }

        // Points
        int n=priceData.length;
        double[] px=new double[n], py=new double[n];
        for(int i=0;i<n;i++){
            px[i]=padL+(i/(double)(n-1))*cW;
            py[i]=padT+cH-(priceData[i]*1_000_000/maxVal)*cH;
        }

        // Area fill
        gc.beginPath(); gc.moveTo(px[0],py[0]);
        for(int i=1;i<n;i++) gc.lineTo(px[i],py[i]);
        gc.lineTo(px[n-1],padT+cH); gc.lineTo(px[0],padT+cH); gc.closePath();
        gc.setFill(new LinearGradient(0,padT,0,padT+cH,false,CycleMethod.NO_CYCLE,
                new Stop(0,Color.web(BLUE,0.45)),new Stop(1,Color.web(BLUE,0.0))));
        gc.fill();

        // Line
        gc.beginPath(); gc.moveTo(px[0],py[0]);
        for(int i=1;i<n;i++) gc.lineTo(px[i],py[i]);
        gc.setStroke(Color.web(BLUE)); gc.setLineWidth(2.5); gc.stroke();

        // Last point
        double lx=px[n-1],ly=py[n-1];
        gc.setFill(Color.web(BLUE)); gc.fillOval(lx-6,ly-6,12,12);
        gc.setStroke(Color.WHITE); gc.setLineWidth(2); gc.strokeOval(lx-6,ly-6,12,12);

        // Tooltip
        double ttW=150,ttH=48,ttX=lx-ttW-12,ttY=ly-ttH-8;
        if(ttX<padL)ttX=lx+12;
        gc.setFill(Color.web("#1a1f45",0.96)); gc.fillRoundRect(ttX,ttY,ttW,ttH,8,8);
        gc.setStroke(Color.web(BORDER)); gc.setLineWidth(1); gc.strokeRoundRect(ttX,ttY,ttW,ttH,8,8);
        gc.setFill(Color.web(MUTED)); gc.setFont(Font.font("Segoe UI",11)); gc.setTextAlign(TextAlignment.LEFT);
        String lastLbl=xLabels!=null&&xLabels.length>0?xLabels[xLabels.length-1]:"";
        gc.fillText(lastLbl+" 14:30",ttX+10,ttY+17);
        gc.setFill(Color.web(TEXT)); gc.setFont(Font.font("Segoe UI",FontWeight.BOLD,13));
        gc.fillText(String.format("%,.0f đ",priceData[n-1]*1_000_000),ttX+10,ttY+36);
    }

    private HBox buildStats() {
        HBox row = new HBox(0);
        row.setStyle("-fx-background-color:"+PANEL+";-fx-background-radius:12;-fx-border-color:"+BORDER+";-fx-border-radius:12;");
        String[][] data={{"Giá khởi điểm","--"},{"Giá hiện tại","--"},{"Giá cao nhất","--"},{"Tổng lượt trả giá","--"}};
        Label[] vals=new Label[4];
        for(int i=0;i<data.length;i++){
            VBox cell=new VBox(5); cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(16,20,16,20)); HBox.setHgrow(cell,Priority.ALWAYS);
            Label l=new Label(data[i][0]); l.setFont(Font.font("Segoe UI",11)); l.setTextFill(Color.web(MUTED));
            vals[i]=new Label(data[i][1]); vals[i].setFont(Font.font("Segoe UI",FontWeight.BOLD,15)); vals[i].setTextFill(Color.web(TEXT));
            cell.getChildren().addAll(l,vals[i]);
            if(i<3)cell.setStyle("-fx-border-color:transparent "+BORDER+" transparent transparent;-fx-border-width:0 1 0 0;");
            row.getChildren().add(cell);
        }
        lblStart=vals[0]; lblCurrent=vals[1]; lblHigh=vals[2]; lblTotal=vals[3];
        return row;
    }

    private void loadData() {
        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_PRICE_HISTORY, itemId);
                Platform.runLater(() -> {
                    if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                        parseAndDraw(resp.getData());
                    } else drawChart(chartCanvas);
                });
            } catch (Exception e) { Platform.runLater(() -> drawChart(chartCanvas)); }
        }).start();
    }

    /**
     * Load danh sách active items từ server để điền vào ComboBox.
     * Khi user chọn item khác → cập nhật itemId và vẽ lại chart.
     */
    @SuppressWarnings("unchecked")
    private void loadItemsIntoComboBox(ComboBox<String> cb, Stage stage) {
        // Map từ itemName → itemId để khi user chọn ta biết itemId
        java.util.Map<String, String> nameToId = new java.util.LinkedHashMap<>();

        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_ACTIVE_ITEMS, null);
                Platform.runLater(() -> {
                    if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                        try {
                            com.google.gson.reflect.TypeToken<List<Map<String,Object>>> tt =
                                    new com.google.gson.reflect.TypeToken<List<Map<String,Object>>>(){};
                            List<Map<String,Object>> items = gson.fromJson(gson.toJson(resp.getData()), tt.getType());
                            cb.getItems().clear();
                            if (items != null) {
                                for (Map<String, Object> item : items) {
                                    String id   = item.getOrDefault("id",   "").toString();
                                    String name = item.getOrDefault("name", "?").toString();
                                    cb.getItems().add(name);
                                    nameToId.put(name, id);
                                }
                            }
                            // Chọn item hiện tại nếu có
                            if (itemName != null && !itemName.isEmpty() && cb.getItems().contains(itemName)) {
                                cb.setValue(itemName);
                            } else if (!cb.getItems().isEmpty()) {
                                cb.setValue(cb.getItems().get(0));
                                itemId = nameToId.getOrDefault(cb.getValue(), itemId);
                                loadData();
                            }
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
        }).start();

        // Khi chọn item khác → load chart mới
        cb.setOnAction(e -> {
            String selected = cb.getValue();
            if (selected != null && nameToId.containsKey(selected)) {
                itemId = nameToId.get(selected);
                itemName = selected;
                stage.setTitle("Biểu đồ giá – " + itemName);
                loadData();
            }
        });
    }

    private void parseAndDraw(Object data) {
        try {
            String json = gson.toJson(data);
            Type mapT = new TypeToken<Map<String,Object>>(){}.getType();
            Map<String,Object> root = gson.fromJson(json, mapT);
            List<Map<String,Object>> bids = null;
            if (root.containsKey("bids")) {
                Type lt = new TypeToken<List<Map<String,Object>>>(){}.getType();
                bids = gson.fromJson(gson.toJson(root.get("bids")), lt);
                updateStat(lblStart,   root.get("startPrice"));
                updateStat(lblCurrent, root.get("currentPrice"));
                updateStat(lblHigh,    root.get("highestPrice"));
                if (root.containsKey("totalBids")) lblTotal.setText(root.get("totalBids").toString());
            } else {
                Type lt = new TypeToken<List<Map<String,Object>>>(){}.getType();
                bids = gson.fromJson(json, lt);
            }
            if (bids==null||bids.isEmpty()){drawChart(chartCanvas);return;}
            List<Double> prices=new ArrayList<>(); List<String> labels=new ArrayList<>();
            for(Map<String,Object> b:bids){
                Object a=b.get("amount"); if(a==null)a=b.get("bidAmount"); if(a==null)a=b.get("price");
                if(a!=null){prices.add(Double.parseDouble(a.toString()));Object ti=b.get("createdAt");if(ti==null)ti=b.get("bidTime");labels.add(ti!=null?shortDate(ti.toString()):"");}
            }
            priceData=prices.stream().mapToDouble(Double::doubleValue).toArray();
            xLabels=labels.toArray(new String[0]);
            drawChart(chartCanvas);
        } catch(Exception e){drawChart(chartCanvas);}
    }

    private void updateStat(Label l, Object v) { if(l!=null&&v!=null){try{l.setText(String.format("%,.0f đ",Double.parseDouble(v.toString())));}catch(Exception e){l.setText(v.toString());}} }
    private String shortDate(String s) { if(s==null)return""; if(s.contains("T"))s=s.split("T")[0]; String[] p=s.split("-"); return p.length>=3?p[2]+"/"+p[1]:s; }
    private String rActive() { return "-fx-background-color:"+BLUE+";-fx-text-fill:white;-fx-background-radius:8;-fx-border-color:transparent;-fx-padding:4 12;-fx-cursor:hand;"; }
    private String rIdle()   { return "-fx-background-color:transparent;-fx-text-fill:"+MUTED+";-fx-background-radius:8;-fx-border-color:transparent;-fx-padding:4 12;-fx-cursor:hand;"; }
}
