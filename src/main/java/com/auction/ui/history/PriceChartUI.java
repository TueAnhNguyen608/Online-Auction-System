package com.auction.ui.history;

import com.auction.client.AuctionClient;
import com.auction.ui.bidding.MainDashboardUI;
import com.auction.common.model.communication.CommandType;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.user.User;
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
import com.auction.ui.components.SidebarHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PriceChartUI {

    private static final String BG     = "#0a0e27";
    private static final String PANEL  = "#0e1232";
    private static final String BG_IN  = "#1a1f45";
    private static final String BORDER = "#2d3270";
    private static final String TEXT   = "#f0f4ff";
    private static final String MUTED  = "#8899bb";
    private static final String BLUE   = "#7c3aed";
    private static final String GREEN  = "#10b981";
    private static final String GOLD   = "#f59e0b";
    private static final String RED    = "#ef4444";

    private AuctionClient client;
    private User currentUser;
    private String selectedItemId, selectedItemName;
    private Stage stage;
    private Gson gson = new Gson();

    private Canvas chartCanvas;
    private double[] priceData = {};
    private String[] xLabels   = {};
    private Label lblChartTitle, lblChartSub, lblHigh, lblLow, lblAvg, lblCount;
    private final LinkedHashMap<String, String> biddedItems = new LinkedHashMap<>();
    private VBox productListBox;

    public void show(AuctionClient client, String itemId, String itemName, User user) {
        show(new Stage(), client, itemId, itemName, user);
    }

    public void show(Stage stage, AuctionClient client, String itemId, String itemName, User user) {
        this.stage            = stage;
        this.client           = client;
        this.currentUser      = user;
        this.selectedItemId   = itemId;
        this.selectedItemName = itemName;
        stage.setTitle("Biểu đồ giá");

        HBox root = new HBox(0);
        root.setStyle("-fx-background-color:" + BG + ";");

        String[][] menu = {
                {"📋","Lịch sử đấu giá"},
                {"🏆","Sản phẩm đã thắng"},
                {"📈","Biểu đồ giá"}
        };
        VBox sidebar = SidebarHelper.build(user, client, menu, 2, idx -> {
            if (idx == 0) new MyBidsHistoryUI().show(stage, client, user);
            else if (idx == 1) new WonProductsUI().show(stage, client, user);
        });

        VBox content = buildContent();
        HBox.setHgrow(content, Priority.ALWAYS);
        root.getChildren().addAll(sidebar, content);

        double w = stage.getScene() != null ? stage.getScene().getWidth()  : 1280;
        double h = stage.getScene() != null ? stage.getScene().getHeight() : 720;
        stage.setScene(new Scene(root, w, h));
        if (!stage.isShowing()) { stage.setMaximized(true); stage.show(); }

        loadBiddedProducts();
    }

    private VBox buildContent() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color:" + BG + ";");
        panel.setPadding(new Insets(20));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(topRow, new Insets(0, 0, 16, 0));

        Button btnBack = buildBackBtn();
        Label lblTitle = new Label("📈  Biểu đồ giá");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web(TEXT));
        Label lblSub = new Label("Chọn sản phẩm để xem biểu đồ biến động giá");
        lblSub.setFont(Font.font("Segoe UI", 12));
        lblSub.setTextFill(Color.web(MUTED));
        VBox titleBox = new VBox(3, lblTitle, lblSub);
        topRow.getChildren().addAll(btnBack, titleBox);

        HBox body = new HBox(16);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox leftPanel = buildLeftPanel();
        leftPanel.setPrefWidth(300); leftPanel.setMinWidth(260); leftPanel.setMaxWidth(320);
        VBox rightPanel = buildRightPanel();
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        body.getChildren().addAll(leftPanel, rightPanel);
        panel.getChildren().addAll(topRow, body);
        return panel;
    }

    private VBox buildLeftPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color:" + PANEL + ";-fx-background-radius:12;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:12;");

        HBox header = new HBox();
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;");
        Label lbl = new Label("🛒  Sản phẩm đã đấu giá");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(TEXT));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnR = new Button("🔄"); btnR.setFont(Font.font(13)); btnR.setCursor(Cursor.HAND);
        btnR.setStyle("-fx-background-color:transparent;-fx-text-fill:" + MUTED + ";-fx-border-color:transparent;-fx-padding:2 6;");
        btnR.setOnAction(e -> loadBiddedProducts());
        header.getChildren().addAll(lbl, sp, btnR);

        productListBox = new VBox(0);
        productListBox.setStyle("-fx-background-color:transparent;");
        Label loading = new Label("⏳  Đang tải...");
        loading.setFont(Font.font("Segoe UI", 13)); loading.setTextFill(Color.web(MUTED)); loading.setPadding(new Insets(20, 16, 20, 16));
        productListBox.getChildren().add(loading);

        ScrollPane scroll = new ScrollPane(productListBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;-fx-border-color:transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, scroll);
        return panel;
    }

    private VBox buildRightPanel() {
        VBox panel = new VBox(16);
        panel.setStyle("-fx-background-color:" + PANEL + ";-fx-background-radius:12;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:12;");
        panel.setPadding(new Insets(20));

        lblChartTitle = new Label("Chọn sản phẩm để xem biểu đồ");
        lblChartTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblChartTitle.setTextFill(Color.web(TEXT));
        lblChartSub = new Label("Biểu đồ biến động giá theo từng lượt đặt");
        lblChartSub.setFont(Font.font("Segoe UI", 12));
        lblChartSub.setTextFill(Color.web(MUTED));

        HBox statsRow = new HBox(0);
        statsRow.setStyle("-fx-background-color:" + BG_IN + ";-fx-background-radius:10;");
        statsRow.setPadding(new Insets(10, 0, 10, 0));
        lblHigh = new Label("--"); lblLow = new Label("--");
        lblAvg  = new Label("--"); lblCount = new Label("--");
        statsRow.getChildren().addAll(
                statCell("Giá cao nhất",  lblHigh,  GOLD),  divV(),
                statCell("Giá thấp nhất", lblLow,   MUTED), divV(),
                statCell("Giá trung bình",lblAvg,   BLUE),  divV(),
                statCell("Lượt đặt giá",  lblCount, GREEN)
        );

        chartCanvas = new Canvas(600, 320);
        drawChart();
        StackPane chartWrap = new StackPane(chartCanvas);
        chartWrap.setStyle("-fx-background-color:" + BG_IN + ";-fx-background-radius:10;");
        chartWrap.setPadding(new Insets(12));
        VBox.setVgrow(chartWrap, Priority.ALWAYS);
        chartWrap.widthProperty().addListener((o,old,n) -> { chartCanvas.setWidth(n.doubleValue()-24); drawChart(); });
        chartWrap.heightProperty().addListener((o,old,n) -> { chartCanvas.setHeight(n.doubleValue()-24); drawChart(); });

        panel.getChildren().addAll(lblChartTitle, lblChartSub, statsRow, chartWrap);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private void loadBiddedProducts() {
        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_MY_BID_HISTORY, null);
                Platform.runLater(() -> {
                    productListBox.getChildren().clear();
                    if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                        try {
                            Type t = new TypeToken<List<Map<String,Object>>>(){}.getType();
                            List<Map<String,Object>> bids = gson.fromJson(gson.toJson(resp.getData()), t);
                            if (bids != null) {
                                biddedItems.clear();
                                for (Map<String,Object> b : bids) {
                                    String id = str(b,"itemId"); String name = str(b,"itemName","name"); double amt = dbl(b,"amount","bidAmount");
                                    if (!id.equals("--") && !biddedItems.containsKey(id)) { biddedItems.put(id, name); addProductCard(id, name, amt, b); }
                                }
                                if (biddedItems.isEmpty()) {
                                    Label empty = new Label("📭  Chưa đặt giá sản phẩm nào");
                                    empty.setFont(Font.font("Segoe UI", 12)); empty.setTextFill(Color.web(MUTED)); empty.setPadding(new Insets(20,16,20,16));
                                    productListBox.getChildren().add(empty);
                                } else {
                                    String firstId = biddedItems.keySet().iterator().next();
                                    selectProduct(firstId, biddedItems.get(firstId));
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void addProductCard(String id, String name, double myAmt, Map<String,Object> bidData) {
        double finPr = dbl(bidData,"finalPrice","final_price","currentPrice");
        String itemStatus = str(bidData,"itemStatus","item_status");
        boolean leading = myAmt >= finPr - 1.0;
        String statusText, statusColor;
        if ("ENDED".equalsIgnoreCase(itemStatus)) { statusText = leading ? "Đã thắng" : "Thua"; statusColor = leading ? GREEN : RED; }
        else { statusText = leading ? "Dẫn đầu" : "Bị vượt"; statusColor = leading ? GREEN : GOLD; }

        VBox card = new VBox(4); card.setPadding(new Insets(12,16,12,16)); card.setCursor(Cursor.HAND);
        String baseStyle  = "-fx-background-color:transparent;-fx-border-color:transparent transparent "+BORDER+" transparent;-fx-border-width:0 0 1 0;";
        String hoverStyle = "-fx-background-color:rgba(124,58,237,0.1);-fx-border-color:transparent transparent "+BORDER+" transparent;-fx-border-width:0 0 1 0;";
        String activeStyle= "-fx-background-color:rgba(124,58,237,0.18);-fx-border-color:transparent transparent "+BORDER+" transparent;-fx-border-width:0 0 1 0;";
        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> { if (!id.equals(selectedItemId)) card.setStyle(hoverStyle); });
        card.setOnMouseExited(e  -> { if (!id.equals(selectedItemId)) card.setStyle(baseStyle); else card.setStyle(activeStyle); });
        card.setOnMouseClicked(e -> {
            productListBox.getChildren().forEach(n -> { if (n instanceof VBox) n.setStyle(baseStyle); });
            card.setStyle(activeStyle); selectProduct(id, name);
        });

        Label emoLbl = new Label(pickIcon(name)); emoLbl.setFont(Font.font(16));
        StackPane iconWrap = new StackPane(emoLbl); iconWrap.setPrefSize(32,32); iconWrap.setMinSize(32,32);
        iconWrap.setStyle("-fx-background-color:#1a1f45;-fx-background-radius:8;");
        Label nameLbl = new Label(name); nameLbl.setFont(Font.font("Segoe UI",FontWeight.BOLD,12)); nameLbl.setTextFill(Color.web(TEXT)); nameLbl.setWrapText(true);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);
        HBox nameRow = new HBox(8,iconWrap,nameLbl); nameRow.setAlignment(Pos.CENTER_LEFT);

        Label amtLbl = new Label(fmt(myAmt)); amtLbl.setFont(Font.font("Segoe UI",11)); amtLbl.setTextFill(Color.web(MUTED));
        Region sp = new Region(); HBox.setHgrow(sp,Priority.ALWAYS);
        Label stLbl = new Label(statusText); stLbl.setFont(Font.font("Segoe UI",FontWeight.BOLD,11)); stLbl.setTextFill(Color.web(statusColor));
        stLbl.setStyle("-fx-background-color:rgba(0,0,0,0.2);-fx-background-radius:20;-fx-padding:2 8;");
        HBox infoRow = new HBox(8,amtLbl,sp,stLbl); infoRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(nameRow, infoRow);
        productListBox.getChildren().add(card);
    }

    private void selectProduct(String id, String name) {
        selectedItemId = id; selectedItemName = name;
        lblChartTitle.setText(name); lblChartSub.setText("Biểu đồ biến động giá – " + name);
        priceData = new double[]{}; xLabels = new String[]{};
        lblHigh.setText("--"); lblLow.setText("--"); lblAvg.setText("--"); lblCount.setText("--");
        drawChart(); loadPriceChart(id);
    }

    @SuppressWarnings("unchecked")
    private void loadPriceChart(String itemId) {
        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_PRICE_HISTORY, itemId);
                Platform.runLater(() -> {
                    if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                        try {
                            Type lt = new TypeToken<List<Map<String,Object>>>(){}.getType();
                            List<Map<String,Object>> points = gson.fromJson(gson.toJson(resp.getData()), lt);
                            if (points != null && !points.isEmpty()) {
                                List<Double> prices = new ArrayList<>(); List<String> labels = new ArrayList<>();
                                for (Map<String,Object> p : points) {
                                    Object a=p.get("price"); if(a==null)a=p.get("amount");
                                    Object ti=p.get("bidTime"); if(ti==null)ti=p.get("time");
                                    if (a!=null){prices.add(Double.parseDouble(a.toString()));labels.add(ti!=null?shortTime(ti.toString()):"");}
                                }
                                priceData=prices.stream().mapToDouble(Double::doubleValue).toArray();
                                xLabels=labels.toArray(new String[0]);
                                if (priceData.length>0){
                                    double high=priceData[0],low=priceData[0],sum=0;
                                    for(double v:priceData){if(v>high)high=v;if(v<low)low=v;sum+=v;}
                                    final double fH=high,fL=low,fA=sum/priceData.length;
                                    lblHigh.setText(fmt(fH));lblLow.setText(fmt(fL));lblAvg.setText(fmt(fA));lblCount.setText(String.valueOf(priceData.length));
                                }
                                drawChart();
                            } else drawChart();
                        } catch (Exception ignored) { drawChart(); }
                    }
                });
            } catch (Exception e) { Platform.runLater(this::drawChart); }
        }).start();
    }

    private void drawChart() {
        if (chartCanvas==null) return;
        GraphicsContext gc=chartCanvas.getGraphicsContext2D();
        double w=chartCanvas.getWidth(),h=chartCanvas.getHeight();
        if(w<=0||h<=0) return;
        gc.setFill(Color.web(BG_IN)); gc.fillRect(0,0,w,h);
        if(priceData==null||priceData.length==0){
            gc.setFill(Color.web(MUTED));gc.setFont(Font.font("Segoe UI",13));gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Chọn sản phẩm để xem biểu đồ",w/2,h/2); return;
        }
        double padL=60,padR=20,padT=20,padB=44,cW=w-padL-padR,cH=h-padT-padB;
        double maxV=priceData[0],minV=priceData[0];
        for(double d:priceData){if(d>maxV)maxV=d;if(d<minV)minV=d;}
        double range=maxV-minV; if(range==0)range=maxV*0.1;
        double lo=minV-range*0.15,hi=maxV+range*0.15; if(lo<0)lo=0;
        int ySteps=5; gc.setFont(Font.font("Segoe UI",10));
        for(int i=0;i<=ySteps;i++){
            double val=lo+(hi-lo)*i/ySteps,y=padT+cH-(val-lo)/(hi-lo)*cH;
            gc.setStroke(Color.web(BORDER,0.6));gc.setLineWidth(0.7);gc.strokeLine(padL,y,padL+cW,y);
            gc.setFill(Color.web(MUTED));gc.setTextAlign(TextAlignment.RIGHT);gc.fillText(fmtM(val),padL-6,y+4);
        }
        int n=priceData.length; double[] px=new double[n],py=new double[n];
        for(int i=0;i<n;i++){px[i]=padL+(n==1?cW/2:(double)i/(n-1)*cW);py[i]=padT+cH-(priceData[i]-lo)/(hi-lo)*cH;}
        gc.setFill(Color.web(MUTED));gc.setTextAlign(TextAlignment.CENTER);gc.setFont(Font.font("Segoe UI",10));
        int xStep=Math.max(1,n/6);
        for(int i=0;i<n;i+=xStep){if(xLabels!=null&&i<xLabels.length)gc.fillText(xLabels[i],px[i],padT+cH+18);}
        if(n==1){gc.setFill(Color.web(GOLD));gc.fillOval(px[0]-6,py[0]-6,12,12);}
        else{
            gc.beginPath();gc.moveTo(px[0],py[0]);
            for(int i=0;i<n-1;i++){double cp1x=px[i]+(px[i+1]-(i>0?px[i-1]:px[i]))/4.0,cp1y=py[i]+(py[i+1]-(i>0?py[i-1]:py[i]))/4.0,cp2x=px[i+1]-((i+2<n?px[i+2]:px[i+1])-px[i])/4.0,cp2y=py[i+1]-((i+2<n?py[i+2]:py[i+1])-py[i])/4.0;gc.bezierCurveTo(cp1x,cp1y,cp2x,cp2y,px[i+1],py[i+1]);}
            gc.lineTo(px[n-1],padT+cH);gc.lineTo(px[0],padT+cH);gc.closePath();
            gc.setFill(new LinearGradient(0,padT,0,padT+cH,false,CycleMethod.NO_CYCLE,new Stop(0,Color.web(BLUE,0.4)),new Stop(1,Color.web(BLUE,0.0))));gc.fill();
            gc.beginPath();gc.moveTo(px[0],py[0]);
            for(int i=0;i<n-1;i++){double cp1x=px[i]+(px[i+1]-(i>0?px[i-1]:px[i]))/4.0,cp1y=py[i]+(py[i+1]-(i>0?py[i-1]:py[i]))/4.0,cp2x=px[i+1]-((i+2<n?px[i+2]:px[i+1])-px[i])/4.0,cp2y=py[i+1]-((i+2<n?py[i+2]:py[i+1])-py[i])/4.0;gc.bezierCurveTo(cp1x,cp1y,cp2x,cp2y,px[i+1],py[i+1]);}
            gc.setStroke(Color.web(BLUE));gc.setLineWidth(2.5);gc.stroke();
            for(int i=0;i<n;i++){gc.setFill(Color.web(i==n-1?GOLD:BLUE));gc.fillOval(px[i]-4,py[i]-4,8,8);gc.setStroke(Color.WHITE);gc.setLineWidth(1.5);gc.strokeOval(px[i]-4,py[i]-4,8,8);}
        }
        double lx=px[n-1],ly=py[n-1];String tip=String.format("%,.0f đ",priceData[n-1]).replace(",",".");
        double ttW=140,ttH=30,ttX=lx-ttW-10,ttY=ly-ttH-8;if(ttX<padL)ttX=lx+10;if(ttY<padT)ttY=ly+8;
        gc.setFill(Color.web(BG_IN,0.96));gc.fillRoundRect(ttX,ttY,ttW,ttH,6,6);
        gc.setStroke(Color.web(GOLD));gc.setLineWidth(1);gc.strokeRoundRect(ttX,ttY,ttW,ttH,6,6);
        gc.setFill(Color.web(GOLD));gc.setFont(Font.font("Segoe UI",FontWeight.BOLD,11));gc.setTextAlign(TextAlignment.LEFT);gc.fillText(tip,ttX+8,ttY+19);
    }

    private Button buildBackBtn() {
        Button b=new Button("← Quay lại");b.setFont(Font.font("Segoe UI",12));b.setCursor(Cursor.HAND);
        String idle="-fx-background-color:transparent;-fx-text-fill:"+MUTED+";-fx-border-color:"+BORDER+";-fx-border-radius:8;-fx-background-radius:8;-fx-padding:5 14;";
        String hover="-fx-background-color:#11153a;-fx-text-fill:"+TEXT+";-fx-border-color:"+BORDER+";-fx-border-radius:8;-fx-background-radius:8;-fx-padding:5 14;";
        b.setStyle(idle);b.setOnMouseEntered(e->b.setStyle(hover));b.setOnMouseExited(e->b.setStyle(idle));
        b.setOnAction(e-> MainDashboardUI.show(stage,currentUser!=null?currentUser.getUsername():"User"));
        return b;
    }

    private VBox statCell(String label,Label valLbl,String color){valLbl.setFont(Font.font("Segoe UI",FontWeight.BOLD,13));valLbl.setTextFill(Color.web(color));Label lbl=new Label(label);lbl.setFont(Font.font("Segoe UI",10));lbl.setTextFill(Color.web(MUTED));VBox cell=new VBox(3,valLbl,lbl);cell.setAlignment(Pos.CENTER);cell.setPadding(new Insets(8,16,8,16));HBox.setHgrow(cell,Priority.ALWAYS);return cell;}
    private Region divV(){Region r=new Region();r.setPrefWidth(1);r.setMinWidth(1);r.setStyle("-fx-background-color:"+BORDER+";");return r;}
    private String str(Map<String,Object> m,String...keys){for(String k:keys){Object v=m.get(k);if(v!=null)return v.toString();}return "--";}
    private double dbl(Map<String,Object> m,String...keys){for(String k:keys){Object v=m.get(k);if(v!=null){try{return Double.parseDouble(v.toString());}catch(Exception x){}}}return 0;}
    private String fmt(double val){return String.format("%,.0f đ",val).replace(",",".");}
    private String fmtM(double val){if(val>=1_000_000){double m=val/1_000_000;return m==(long)m?(long)m+"M":String.format("%.1fM",m);}if(val>=1_000)return(long)(val/1_000)+"K";return String.format("%.0f",val);}
    private String shortTime(String s){if(s==null)return"";try{if(s.contains("T"))return s.split("T")[1].substring(0,5);if(s.contains(" "))return s.split(" ")[1].substring(0,5);}catch(Exception x){}return s;}
    private String pickIcon(String n){if(n==null)return"📦";String l=n.toLowerCase();if(l.contains("iphone")||l.contains("samsung")||l.contains("pixel"))return"📱";if(l.contains("macbook")||l.contains("laptop")||l.contains("dell")||l.contains("asus"))return"💻";if(l.contains("ipad")||l.contains("tab"))return"📱";if(l.contains("watch"))return"⌚";if(l.contains("airpods")||l.contains("sony")||l.contains("bose"))return"🎧";if(l.contains("playstation")||l.contains("xbox"))return"🎮";if(l.contains("keyboard")||l.contains("mouse")||l.contains("logitech"))return"⌨️";return"📦";}
}