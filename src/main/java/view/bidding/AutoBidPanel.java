package view.bidding;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import view.common.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AutoBidPanel implements Initializable {

    // ── Màu hệ thống (khớp với toàn bộ app) ────────────────────
    private static final String BG_DEEP    = "#07101f";
    private static final String BG_MAIN    = "#0a1625";
    private static final String BG_CARD    = "#0d1e35";
    private static final String BG_ROW_ALT = "#0b1a2e";
    private static final String BG_HEADER  = "#0a1829";
    private static final String BORDER     = "#1a3055";
    private static final String GOLD       = "#F5C518";
    private static final String GOLD_DIM   = "#c9a114";
    private static final String TEXT_PRI   = "#f0f6ff";
    private static final String TEXT_SEC   = "#c5d5e8";
    private static final String TEXT_MUTED = "#6b84a8";
    private static final String GREEN      = "#22c55e";
    private static final String RED        = "#ef4444";
    private static final String AMBER      = "#f59e0b";
    private static final String HOVER_ROW  = "#1a3a5c";

    // ── FXML ────────────────────────────────────────────────────
    @FXML private Label    lblUserName;
    @FXML private Button   btnTabDanhSach, btnTabDauGia, btnTabAutoBid;
    @FXML private Button   btnQuayLai;
    @FXML private ComboBox<String>  cboProduct;
    @FXML private Label             lblCurrentPrice;
    @FXML private TextField         txtMaxPrice;
    @FXML private Button            btnToggle;
    @FXML private TextField         txtStep;
    @FXML private TextField         txtMaxTimes;
    @FXML private TextField         txtDelay;
    @FXML private Label             lblStepError;
    @FXML private TableView<AutoBidRow>            tableAutoBid;
    @FXML private TableColumn<AutoBidRow, Integer> colStt;
    @FXML private TableColumn<AutoBidRow, String>  colProduct;
    @FXML private TableColumn<AutoBidRow, String>  colCurrentPrice;
    @FXML private TableColumn<AutoBidRow, String>  colMaxPrice;
    @FXML private TableColumn<AutoBidRow, String>  colStep;
    @FXML private TableColumn<AutoBidRow, String>  colStatus;
    @FXML private TableColumn<AutoBidRow, String>  colTimes;
    @FXML private TableColumn<AutoBidRow, String>  colTime;
    @FXML private TableColumn<AutoBidRow, Void>    colActions;
    @FXML private Button btnRefresh, btnHistory, btnCreate;

    private static final String TAB_INACTIVE =
            "-fx-background-color:#0a1625;-fx-text-fill:#8aadd0;-fx-font-weight:bold;" +
                    "-fx-font-size:13px;-fx-padding:8 16 8 16;-fx-background-radius:4 4 0 0;" +
                    "-fx-border-color:#1a3055;-fx-border-radius:4 4 0 0;-fx-border-width:1;-fx-cursor:hand;";
    private static final String TAB_HOVER =
            "-fx-background-color:" + GOLD + ";-fx-text-fill:#07101f;-fx-font-weight:bold;" +
                    "-fx-font-size:13px;-fx-padding:8 16 8 16;-fx-background-radius:4 4 0 0;" +
                    "-fx-border-color:" + GOLD + ";-fx-border-radius:4 4 0 0;-fx-border-width:1;-fx-cursor:hand;";

    private boolean autoBidOn = false;
    private int editingIndex = -1;
    private final List<Item> serverItems = new ArrayList<>();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();

    // ── Model ────────────────────────────────────────────────────
    public static class AutoBidRow {
        int stt;
        String product, currentPrice, maxPrice, step, status, times, time;
        String itemId;
        public AutoBidRow(int stt, String product, String currentPrice, String maxPrice,
                          String step, String status, String times, String time) {
            this.stt=stt; this.product=product; this.currentPrice=currentPrice;
            this.maxPrice=maxPrice; this.step=step; this.status=status;
            this.times=times; this.time=time;
        }
        public int    getStt()            { return stt; }
        public String getProduct()        { return product; }
        public String getCurrentPrice()   { return currentPrice; }
        public String getMaxPrice()       { return maxPrice; }
        public String getStep()           { return step; }
        public String getStatus()         { return status; }
        public String getTimes()          { return times; }
        public String getTime()           { return time; }
        public void   setStatus(String s) { this.status = s; }
    }

    // ── Initialize ───────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tableAutoBid.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) {
                java.net.URL css = getClass().getResource("/css/bidding_style.css");
                if (css != null) { String p=css.toExternalForm(); if(!scene.getStylesheets().contains(p)) scene.getStylesheets().add(p); }
            }
        });
        setupTableColumns();
        setupHovers();
        tableAutoBid.setItems(FXCollections.observableArrayList());
        styleHeaderTable();
        styleScrollBarAndCombo();
        loadProductsFromServer();
        loadAutoBidFromServer();
        syncToggleUI();
    }

    // ── Load products ────────────────────────────────────────────
    private void loadProductsFromServer() {
        new Thread(() -> {
            try {
                AuctionClient client = SessionManager.getClient();
                if (client == null) throw new Exception("Chưa có kết nối");
                ResponseMessage resp = client.getActiveItems();
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    java.lang.reflect.Type t = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(gson.toJson(resp.getData()), t);
                    if (items != null && !items.isEmpty()) {
                        serverItems.clear(); serverItems.addAll(items);
                        List<String> names = new ArrayList<>();
                        for (Item item : items) names.add(item.getName());
                        Platform.runLater(() -> { cboProduct.getItems().setAll(names); setupComboBoxListener(); });
                        return;
                    }
                }
            } catch (Exception e) { System.out.println("⚠ Dùng danh sách mẫu: "+e.getMessage()); }
            Platform.runLater(this::loadSampleProducts);
        }).start();
    }

    private void loadSampleProducts() {
        String[] P = {"iPhone 15 Pro Max 1TB","iPhone 15 Pro 512GB","Samsung Galaxy S24 Ultra","Xiaomi 14 Ultra",
                "Google Pixel 8 Pro","MacBook Pro M3 Max","Dell XPS 15","LG Gram 17","Asus ROG Zephyrus G14",
                "Sony WH-1000XM5","AirPods Pro 2","Bose QC Ultra","Apple Watch Ultra 2",
                "Samsung Galaxy Watch 6 Classic","Magic Keyboard","Logitech MX Master 3S",
                "iPad Pro 12.9 inch M2","Samsung Tab S9 Ultra"};
        long[] V = {25000000L,22000000L,20000000L,15000000L,18000000L,45000000L,35000000L,28000000L,
                32000000L,5000000L,4500000L,6000000L,15000000L,7000000L,3000000L,2000000L,28000000L,22000000L};
        cboProduct.getItems().setAll(P); serverItems.clear();
        for (int i=0;i<P.length;i++) { Item item=new Item(); item.setName(P[i]); item.setCurrentPrice(V[i]); serverItems.add(item); }
        setupComboBoxListener();
    }

    private void setupComboBoxListener() {
        cboProduct.getSelectionModel().selectedIndexProperty().addListener((obs,o,idx) -> {
            int i=idx.intValue();
            if (i>=0 && i<serverItems.size()) lblCurrentPrice.setText(formatPrice((long)serverItems.get(i).getCurrentPrice()));
        });
    }

    private void resetForm() {
        editingIndex=-1; cboProduct.getSelectionModel().clearSelection(); lblCurrentPrice.setText("0");
        txtMaxPrice.clear(); txtStep.clear(); txtMaxTimes.clear();
        if (txtDelay!=null) txtDelay.clear();
        if (lblStepError!=null) lblStepError.setText("");
        btnCreate.setText("＋  Tạo auto-bid mới");
    }

    private void styleScrollBarAndCombo() {
        Platform.runLater(() -> {
            cboProduct.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty||item==null){setText(null);setStyle("");return;}
                    setText(item);
                    setStyle("-fx-text-fill:"+TEXT_PRI+";-fx-background-color:"+BG_CARD+";-fx-font-size:13px;");
                }
                { setOnMouseEntered(e->setStyle("-fx-text-fill:"+TEXT_PRI+";-fx-background-color:"+HOVER_ROW+";-fx-font-size:13px;"));
                    setOnMouseExited(e->setStyle("-fx-text-fill:"+TEXT_PRI+";-fx-background-color:"+BG_CARD+";-fx-font-size:13px;")); }
            });
            cboProduct.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty); setText(empty||item==null?null:item);
                    setStyle("-fx-text-fill:"+TEXT_PRI+";-fx-background-color:transparent;-fx-font-size:13px;");
                }
            });
        });
    }

    // ── Table columns ────────────────────────────────────────────
    private void setupTableColumns() {
        tableAutoBid.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colStt.setCellValueFactory(d->new javafx.beans.property.SimpleIntegerProperty(d.getValue().getStt()).asObject());
        colStt.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(Integer v,boolean e) {
                super.updateItem(v,e); if(e||v==null){setText(null);setStyle("");return;}
                setText(String.valueOf(v));
                setStyle("-fx-alignment:CENTER;-fx-text-fill:"+TEXT_MUTED+";-fx-font-weight:bold;");
            }
        });

        colProduct.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().getProduct()));
        colProduct.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(String v,boolean e) {
                super.updateItem(v,e); if(e||v==null){setText(null);setGraphic(null);setStyle("");return;}
                // Badge auto-bid
                Label name=new Label(v);
                name.setStyle("-fx-text-fill:"+TEXT_PRI+";-fx-font-size:12px;-fx-font-weight:bold;");
                Label tag=new Label("🤖 Auto-bid");
                tag.setStyle("-fx-text-fill:"+GOLD+";-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-color:rgba(245,197,24,0.1);-fx-background-radius:4;" +
                        "-fx-border-color:rgba(245,197,24,0.25);-fx-border-radius:4;-fx-padding:1 5 1 5;");
                VBox box=new VBox(2,name,tag); box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box); setText(null);
                setStyle("-fx-alignment:CENTER_LEFT;-fx-padding:4 0 4 10;");
            }
        });

        colCurrentPrice.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().getCurrentPrice()));
        colCurrentPrice.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(String v,boolean e) {
                super.updateItem(v,e); if(e||v==null){setText(null);setStyle("");return;}
                setText(v); setStyle("-fx-text-fill:"+TEXT_SEC+";-fx-alignment:CENTER;-fx-font-size:12px;");
            }
        });

        colMaxPrice.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().getMaxPrice()));
        colMaxPrice.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(String v,boolean e) {
                super.updateItem(v,e); if(e||v==null){setText(null);setStyle("");return;}
                setText(v); setStyle("-fx-text-fill:"+GOLD+";-fx-font-weight:bold;-fx-alignment:CENTER;-fx-font-size:13px;");
            }
        });

        colStep.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().getStep()));
        colStep.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(String v,boolean e) {
                super.updateItem(v,e); if(e||v==null){setText(null);setStyle("");return;}
                setText(v); setStyle("-fx-text-fill:"+TEXT_MUTED+";-fx-alignment:CENTER;-fx-font-size:12px;");
            }
        });

        colStatus.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(String v,boolean e) {
                super.updateItem(v,e);
                if(e||v==null){setText(null);setGraphic(null);setStyle("");return;}
                String fg, bg, bc;
                switch(v) {
                    case "Đang hoạt động": fg=GREEN;  bg="rgba(34,197,94,0.10)"; bc="rgba(34,197,94,0.25)"; break;
                    case "Tạm dừng":       fg=AMBER;  bg="rgba(245,158,11,0.10)"; bc="rgba(245,158,11,0.25)"; break;
                    default:               fg=RED;    bg="rgba(239,68,68,0.10)";  bc="rgba(239,68,68,0.20)"; break;
                }
                Label dot=new Label("●"); dot.setStyle("-fx-text-fill:"+fg+";-fx-font-size:8px;");
                Label lbl=new Label(v);   lbl.setStyle("-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-font-weight:bold;");
                HBox badge=new HBox(5,dot,lbl); badge.setAlignment(Pos.CENTER);
                badge.setStyle("-fx-background-color:"+bg+";-fx-background-radius:20;" +
                        "-fx-border-color:"+bc+";-fx-border-radius:20;-fx-padding:3 10 3 10;");
                setGraphic(badge); setText(null); setStyle("-fx-alignment:CENTER;");
            }
        });

        colTimes.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().getTimes()));
        colTimes.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(String v,boolean e) {
                super.updateItem(v,e); if(e||v==null){setText(null);setStyle("");return;}
                setText(v); setStyle("-fx-text-fill:"+TEXT_SEC+";-fx-alignment:CENTER;-fx-font-size:12px;");
            }
        });

        colTime.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().getTime()));
        colTime.setCellFactory(c->new TableCell<>() {
            @Override protected void updateItem(String v,boolean e) {
                super.updateItem(v,e); if(e||v==null){setText(null);setStyle("");return;}
                setText(v); setStyle("-fx-text-fill:"+TEXT_MUTED+";-fx-alignment:CENTER;-fx-font-size:11px;");
            }
        });

        colActions.setCellFactory(c->new TableCell<>() {
            private final String SE="-fx-background-color:"+GOLD+";-fx-background-radius:6;-fx-border-color:transparent;-fx-cursor:hand;-fx-pref-width:28;-fx-pref-height:28;-fx-min-width:28;-fx-min-height:28;-fx-padding:0;";
            private final String SD="-fx-background-color:"+RED+";-fx-background-radius:6;-fx-border-color:transparent;-fx-cursor:hand;-fx-pref-width:28;-fx-pref-height:28;-fx-min-width:28;-fx-min-height:28;-fx-padding:0;";
            private final Button btnEdit=new Button(), btnDelete=new Button();
            private final HBox box=new HBox(5,btnEdit,btnDelete);
            {
                javafx.scene.image.ImageView ie=new javafx.scene.image.ImageView();
                ie.setFitWidth(14);ie.setFitHeight(14);ie.setPreserveRatio(true);
                try{java.net.URL u=getClass().getResource("/icons/edit.png");if(u!=null)ie.setImage(new javafx.scene.image.Image(u.toExternalForm()));}catch(Exception x){}
                btnEdit.setGraphic(ie);btnEdit.setStyle(SE);
                btnEdit.setOnMouseEntered(e->btnEdit.setStyle(SE.replace(GOLD,GOLD_DIM)));
                btnEdit.setOnMouseExited(e->btnEdit.setStyle(SE));

                javafx.scene.image.ImageView id=new javafx.scene.image.ImageView();
                id.setFitWidth(14);id.setFitHeight(14);id.setPreserveRatio(true);
                try{java.net.URL u=getClass().getResource("/icons/delete.png");if(u!=null)id.setImage(new javafx.scene.image.Image(u.toExternalForm()));}catch(Exception x){}
                btnDelete.setGraphic(id);btnDelete.setStyle(SD);
                btnDelete.setOnMouseEntered(e->btnDelete.setStyle(SD.replace(RED,"#dc2626")));
                btnDelete.setOnMouseExited(e->btnDelete.setStyle(SD));

                box.setStyle("-fx-alignment:CENTER;");

                btnEdit.setOnAction(e->{
                    int idx=getIndex(); if(idx<0||idx>=tableAutoBid.getItems().size()) return;
                    AutoBidRow row=tableAutoBid.getItems().get(idx); editingIndex=idx;
                    cboProduct.getSelectionModel().select(row.getProduct());
                    txtMaxPrice.setText(row.getMaxPrice().replaceAll("[^0-9]",""));
                    txtStep.setText(row.getStep().replaceAll("[^0-9]",""));
                    String[] tp=row.getTimes().split("/");
                    txtMaxTimes.setText(tp.length>1?tp[1].trim():"20");
                    btnCreate.setText("💾  Lưu thay đổi");
                    row.setStatus("Tạm dừng"); tableAutoBid.refresh();
                });

                btnDelete.setOnAction(e->{
                    int idx=getIndex(); if(idx<0) return;
                    AutoBidRow row=tableAutoBid.getItems().get(idx);
                    if(row.itemId!=null) {
                        final String cid=row.itemId;
                        new Thread(()->{
                            try{AuctionClient cl=SessionManager.getClient();
                                if(cl!=null){Map<String,Object> d=new HashMap<>();d.put("itemId",cid);
                                    ResponseMessage r=cl.sendRequest(CommandType.CANCEL_AUTO_BID,d);
                                    System.out.println("✅ Hủy auto-bid: "+(r!=null?r.getMessage():"Không phản hồi"));}}
                            catch(Exception ex){System.out.println("⚠ Lỗi hủy: "+ex.getMessage());}
                        }).start();
                    }
                    User usr=SessionManager.getCurrentUser();
                    if(usr!=null&&row.itemId!=null){
                        SessionManager.removeAutoBidCache(usr.getUsername(),row.itemId);
                        SessionManager.removeAutoBidHistory(usr.getUsername(),row.itemId);
                    }
                    tableAutoBid.getItems().remove(idx);
                    for(int i=0;i<tableAutoBid.getItems().size();i++) tableAutoBid.getItems().get(i).stt=i+1;
                    tableAutoBid.refresh();
                    if(editingIndex==idx) resetForm();
                });
            }
            @Override protected void updateItem(Void v,boolean empty){ super.updateItem(v,empty); setGraphic(empty?null:box); }
        });

        tableAutoBid.setRowFactory(tv->new TableRow<>() {
            @Override protected void updateItem(AutoBidRow item,boolean empty) {
                super.updateItem(item,empty);
                if(empty||item==null){setStyle("-fx-background-color:"+BG_CARD+";-fx-border-color:transparent;");setOnMouseEntered(null);setOnMouseExited(null);}
                else{
                    String bg=(getIndex()%2==0)?BG_CARD:BG_ROW_ALT;
                    String normal="-fx-background-color:"+bg+";-fx-border-color:"+BORDER+";-fx-border-width:0 0 1 0;";
                    String hover="-fx-background-color:"+HOVER_ROW+";-fx-border-color:"+BORDER+";-fx-border-width:0 0 1 0;";
                    setStyle(normal);
                    setOnMouseEntered(ev->setStyle(hover));
                    setOnMouseExited(ev->setStyle(normal));
                }
            }
        });
    }

    private void styleHeaderTable() {
        Platform.runLater(()->Platform.runLater(()->{
            tableAutoBid.lookupAll(".column-header").forEach(n->n.setStyle("-fx-background-color:"+BG_HEADER+";-fx-border-color:transparent "+BORDER+" transparent transparent;-fx-border-width:0 1 0 0;"));
            tableAutoBid.lookupAll(".column-header .label").forEach(n->n.setStyle("-fx-text-fill:"+TEXT_MUTED+";-fx-font-weight:bold;-fx-font-size:11px;-fx-text-transform:uppercase;"));
            tableAutoBid.lookupAll(".column-header-background").forEach(n->n.setStyle("-fx-background-color:"+BG_HEADER+";"));
            tableAutoBid.lookupAll(".filler").forEach(n->n.setStyle("-fx-background-color:"+BG_HEADER+";"));
        }));
    }

    private void setupHovers() {
        addHover(btnTabDanhSach,TAB_INACTIVE,TAB_HOVER);
        addHover(btnTabDauGia,TAB_INACTIVE,TAB_HOVER);
        String bN="-fx-background-color:"+BG_CARD+";-fx-background-radius:7;-fx-border-color:"+BORDER+";-fx-border-radius:7;-fx-border-width:1;-fx-text-fill:"+TEXT_SEC+";-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:8 16 8 14;-fx-cursor:hand;";
        String bH="-fx-background-color:rgba(245,197,24,0.12);-fx-background-radius:7;-fx-border-color:"+GOLD+";-fx-border-radius:7;-fx-border-width:1;-fx-text-fill:"+GOLD+";-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:8 16 8 14;-fx-cursor:hand;";
        addHover(btnQuayLai,bN,bH);
        String btnN="-fx-background-color:"+BG_CARD+";-fx-background-radius:7;-fx-border-color:"+BORDER+";-fx-border-radius:7;-fx-border-width:1;-fx-text-fill:"+TEXT_SEC+";-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:8 18 8 14;-fx-cursor:hand;";
        String btnH="-fx-background-color:"+HOVER_ROW+";-fx-background-radius:7;-fx-border-color:#2a5090;-fx-border-radius:7;-fx-border-width:1;-fx-text-fill:"+TEXT_PRI+";-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:8 18 8 14;-fx-cursor:hand;";
        addHover(btnRefresh,btnN,btnH); addHover(btnHistory,btnN,btnH);
        String cN="-fx-background-color:#15803d;-fx-background-radius:7;-fx-border-color:transparent;-fx-text-fill:#f0fff4;-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:8 18 8 14;-fx-cursor:hand;";
        String cH="-fx-background-color:#166534;-fx-background-radius:7;-fx-border-color:transparent;-fx-text-fill:#f0fff4;-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:8 18 8 14;-fx-cursor:hand;";
        addHover(btnCreate,cN,cH);
    }

    private void addHover(Button b,String n,String h){ b.setOnMouseEntered(e->b.setStyle(h)); b.setOnMouseExited(e->b.setStyle(n)); }
    private static String formatPrice(long p){ return String.format("%,d",p).replace(",",".")+" đ"; }

    @FXML void onTabDanhSach(){ navigate("/fxml/bidding/ProductListPanel.fxml"); }
    @FXML void onTabDauGia(){ navigate("/fxml/bidding/ProductDetailPanel.fxml"); }
    @FXML void onTabAutoBid(){ }
    @FXML void onQuayLai(){ navigate("/fxml/bidding/MainDashboardUI.fxml"); }

    @FXML void onToggleAutoBid() {
        autoBidOn=!autoBidOn;
        boolean formEmpty=cboProduct.getValue()==null||cboProduct.getValue().isEmpty();
        if(formEmpty&&!tableAutoBid.getItems().isEmpty()){
            String ns=autoBidOn?"Đang hoạt động":"Tạm dừng";
            for(AutoBidRow row:tableAutoBid.getItems()) if(!row.getStatus().equals("Đã dừng")) row.setStatus(ns);
            tableAutoBid.refresh();
        }
        syncToggleUI();
    }

    private void syncToggleUI() {
        if(autoBidOn){
            btnToggle.setText("BẬT");
            btnToggle.setStyle("-fx-background-color:"+GREEN+";-fx-background-radius:20;-fx-border-color:transparent;-fx-text-fill:#f0fff4;-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:7 10 7 10;-fx-cursor:hand;-fx-pref-height:36;");
        } else {
            btnToggle.setText("TẮT");
            btnToggle.setStyle("-fx-background-color:#1e2d40;-fx-background-radius:20;-fx-border-color:"+BORDER+";-fx-text-fill:"+TEXT_MUTED+";-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:7 10 7 10;-fx-cursor:hand;-fx-pref-height:36;");
        }
    }

    @FXML void onRefresh(){ loadProductsFromServer(); styleHeaderTable(); }

    // ── Cửa sổ lịch sử auto-bid ─────────────────────────────────
    @FXML void onHistory() {
        User usr=SessionManager.getCurrentUser();
        if(usr==null){showAlert("Chưa đăng nhập!");return;}
        AuctionClient client=SessionManager.getClient();

        Stage st=new Stage(); st.setTitle("Lịch sử Auto-Bid"); st.setMinWidth(920); st.setMinHeight(600);

        VBox root=new VBox(0); root.setStyle("-fx-background-color:"+BG_MAIN+";"); root.setPadding(new Insets(0));

        // ── Topbar ──────────────────────────────────────────────
        HBox topbar=new HBox(); topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setStyle("-fx-background-color:"+BG_DEEP+";-fx-border-color:transparent transparent "+BORDER+" transparent;-fx-border-width:0 0 1 0;-fx-padding:0 24 0 24;-fx-min-height:52;-fx-pref-height:52;");
        Label logoLbl=new Label("⚖  HỆ THỐNG ĐẤU GIÁ");
        logoLbl.setStyle("-fx-text-fill:"+GOLD+";-fx-font-size:16px;-fx-font-weight:bold;");
        topbar.getChildren().add(logoLbl);

        // ── Content ──────────────────────────────────────────────
        VBox content=new VBox(16); content.setPadding(new Insets(24,28,24,28));
        VBox.setVgrow(content,Priority.ALWAYS);

        // Header
        HBox iconBox=new HBox();
        iconBox.setStyle("-fx-background-color:rgba(245,197,24,0.10);-fx-background-radius:8;-fx-border-color:rgba(245,197,24,0.25);-fx-border-radius:8;-fx-padding:8;-fx-pref-width:40;-fx-pref-height:40;-fx-alignment:CENTER;");
        Label iconLbl=new Label("🤖"); iconLbl.setStyle("-fx-font-size:18px;"); iconBox.getChildren().add(iconLbl);
        Label titleLbl=new Label("Lịch sử Auto-Bid");
        titleLbl.setStyle("-fx-text-fill:"+TEXT_PRI+";-fx-font-size:18px;-fx-font-weight:bold;");
        Label subLbl=new Label("Tất cả các lần hệ thống tự động đặt giá thay bạn");
        subLbl.setStyle("-fx-text-fill:"+TEXT_MUTED+";-fx-font-size:12px;");
        VBox titleBox=new VBox(3,titleLbl,subLbl); titleBox.setAlignment(Pos.CENTER_LEFT);
        HBox header=new HBox(12,iconBox,titleBox); header.setAlignment(Pos.CENTER_LEFT);

        // Stats cards
        Label lblTotal=mkStatCard("Tổng","0",GOLD);
        Label lblWin=mkStatCard("Dẫn đầu/Thắng","0",GREEN);
        Label lblLose=mkStatCard("Bị vượt/Thua","0",RED);

        HBox statsRow=new HBox(12,
                wrapStat("Tổng lần auto-bid",lblTotal),
                wrapStat("Đang thắng / Dẫn đầu",lblWin),
                wrapStat("Bị vượt / Thua",lblLose));
        statsRow.setAlignment(Pos.CENTER_LEFT);

        // Table
        TableView<AutoBidRow> tbl=new TableView<>();
        tbl.setStyle("-fx-background-color:"+BG_CARD+";-fx-border-color:"+BORDER+";-fx-border-radius:12;-fx-background-radius:12;");
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setPlaceholder(new Label("📭  Chưa có lần đặt giá tự động nào"){{setStyle("-fx-text-fill:"+TEXT_MUTED+";-fx-font-size:13px;");}});
        VBox.setVgrow(tbl,Priority.ALWAYS);

        tbl.getColumns().addAll(
                mkHCol("#",50,r->String.valueOf(r.getStt()),TEXT_MUTED,false),
                mkHCol("Sản phẩm",210,r->r.getProduct(),TEXT_PRI,false),
                mkHCol("Giá auto-bid",130,r->r.getMaxPrice(),GOLD,true),
                mkHCol("Giá lúc đặt",130,r->r.getCurrentPrice(),TEXT_SEC,false),
                mkHCol("Thời gian",160,r->r.getTime(),TEXT_MUTED,false),
                mkHColStatus(120)
        );

        tbl.setRowFactory(tv->new TableRow<>(){
            @Override protected void updateItem(AutoBidRow item,boolean empty){
                super.updateItem(item,empty);
                if(empty||item==null){setStyle("-fx-background-color:"+BG_CARD+";");setOnMouseEntered(null);setOnMouseExited(null);}
                else{
                    String bg=(getIndex()%2==0)?BG_CARD:BG_ROW_ALT;
                    String normal="-fx-background-color:"+bg+";-fx-border-color:transparent transparent "+BORDER+" transparent;-fx-border-width:0 0 1 0;";
                    String hover="-fx-background-color:"+HOVER_ROW+";-fx-border-color:transparent transparent "+BORDER+" transparent;-fx-border-width:0 0 1 0;";
                    setStyle(normal); setOnMouseEntered(e->setStyle(hover)); setOnMouseExited(e->setStyle(normal));
                }
            }
        });

        Platform.runLater(()->Platform.runLater(()->{
            tbl.lookupAll(".column-header").forEach(n->n.setStyle("-fx-background-color:"+BG_HEADER+";-fx-border-color:transparent "+BORDER+" transparent transparent;-fx-border-width:0 1 0 0;"));
            tbl.lookupAll(".column-header .label").forEach(n->n.setStyle("-fx-text-fill:"+TEXT_MUTED+";-fx-font-weight:bold;-fx-font-size:11px;"));
            tbl.lookupAll(".column-header-background").forEach(n->n.setStyle("-fx-background-color:"+BG_HEADER+";"));
            tbl.lookupAll(".filler").forEach(n->n.setStyle("-fx-background-color:"+BG_HEADER+";"));
        }));

        content.getChildren().addAll(header,statsRow,tbl);
        root.getChildren().addAll(topbar,content);

        // ── Load data ────────────────────────────────────────────
        new Thread(()->{
            List<AutoBidRow> rows=new ArrayList<>();
            List<SessionManager.AutoBidHistoryEntry> cached=SessionManager.getAutoBidHistory(usr.getUsername());
            int stt=1;
            Set<String> cachedIds=new HashSet<>();
            for(SessionManager.AutoBidHistoryEntry e:cached){
                AutoBidRow row=new AutoBidRow(stt++,e.itemName,e.currentPrice,e.bidAmount,"auto-bid",e.status,"--",e.bidTime);
                row.itemId=e.itemId; rows.add(row);
                if(e.itemId!=null) cachedIds.add(e.itemId);
            }
            if(client!=null){
                try{
                    ResponseMessage resp=client.sendRequest(CommandType.GET_MY_BID_HISTORY,null);
                    if(resp!=null&&resp.getCode()==ResponseCode.SUCCESS){
                        java.lang.reflect.Type lt=new TypeToken<List<Map<String,Object>>>(){}.getType();
                        List<Map<String,Object>> allBids=gson.fromJson(gson.toJson(resp.getData()),lt);
                        if(allBids!=null){
                            if(!allBids.isEmpty()) System.out.println("🔍 Server fields: "+allBids.get(0).keySet());
                            for(Map<String,Object> b:allBids){
                                Object ia=b.get("autoBid"); if(ia==null) ia=b.get("isAutoBid"); if(ia==null) ia=b.get("auto_bid");
                                boolean auto=ia!=null&&(Boolean.TRUE.equals(ia)||"true".equalsIgnoreCase(ia.toString())||"AUTO".equalsIgnoreCase(ia.toString()));
                                if(!auto) continue;
                                String sid=hStr(b,"itemId","item_id","id");
                                if(cachedIds.contains(sid)) continue;
                                String name=hStr(b,"itemName","name"),amt=hFmt(b,"amount","bidAmount"),curP=hFmt(b,"currentPrice","finalPrice"),time=hTime(hStr(b,"bidTime","createdAt"));
                                double ma=hDbl(b,"amount","bidAmount"),fp=hDbl(b,"finalPrice","currentPrice");
                                String stat=ma>=fp-1.0?"✅ Dẫn đầu":"⚡ Bị vượt";
                                AutoBidRow row=new AutoBidRow(stt++,name,curP,amt,"auto-bid",stat,"--",time);
                                row.itemId=sid; rows.add(row);
                            }
                        }
                    }
                }catch(Exception e){System.out.println("⚠ Server: "+e.getMessage());}
            }
            final List<AutoBidRow> fr=new ArrayList<>(rows);
            final long win=fr.stream().filter(r->r.getStatus().contains("Dẫn đầu")||r.getStatus().contains("Thắng")).count();
            Platform.runLater(()->{
                tbl.setItems(FXCollections.observableArrayList(fr));
                lblTotal.setText(String.valueOf(fr.size()));
                lblWin.setText(String.valueOf(win));
                lblLose.setText(String.valueOf(fr.size()-win));
            });
        }).start();

        st.setScene(new Scene(root,920,600)); st.show();
    }

    // ── Stat card helpers ────────────────────────────────────────
    private Label mkStatCard(String label,String val,String color){
        Label l=new Label(val); l.setStyle("-fx-text-fill:"+color+";-fx-font-size:26px;-fx-font-weight:bold;"); return l;
    }
    private VBox wrapStat(String label,Label valLabel){
        Label lbl=new Label(label); lbl.setStyle("-fx-text-fill:"+TEXT_MUTED+";-fx-font-size:11px;-fx-font-weight:bold;");
        VBox box=new VBox(4,lbl,valLabel); box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color:"+BG_CARD+";-fx-background-radius:10;-fx-border-color:"+BORDER+";-fx-border-radius:10;-fx-padding:14 20 14 20;-fx-pref-width:180;");
        return box;
    }

    // ── Column factories ─────────────────────────────────────────
    private TableColumn<AutoBidRow,String> mkHCol(String title,double w,java.util.function.Function<AutoBidRow,String> getter,String color,boolean bold){
        TableColumn<AutoBidRow,String> col=new TableColumn<>(title); col.setPrefWidth(w);
        col.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(getter.apply(d.getValue())));
        col.setCellFactory(c->new TableCell<>(){
            @Override protected void updateItem(String v,boolean e){
                super.updateItem(v,e); if(e||v==null){setText(null);setStyle("");return;}
                setText(v); setStyle("-fx-text-fill:"+color+";-fx-alignment:CENTER;"+(bold?"-fx-font-weight:bold;-fx-font-size:13px;":"-fx-font-size:12px;"));
            }
        }); return col;
    }

    private TableColumn<AutoBidRow,String> mkHColStatus(double w){
        TableColumn<AutoBidRow,String> col=new TableColumn<>("Trạng thái"); col.setPrefWidth(w);
        col.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().getStatus()));
        col.setCellFactory(c->new TableCell<>(){
            @Override protected void updateItem(String v,boolean e){
                super.updateItem(v,e);
                if(e||v==null){setText(null);setGraphic(null);setStyle("");return;}
                String fg,bg,bc;
                if(v.contains("Dẫn đầu")||v.contains("Thắng")){fg=GREEN;bg="rgba(34,197,94,0.10)";bc="rgba(34,197,94,0.25)";}
                else if(v.contains("Vượt")||v.contains("vượt")){fg=AMBER;bg="rgba(245,158,11,0.10)";bc="rgba(245,158,11,0.25)";}
                else{fg=RED;bg="rgba(239,68,68,0.10)";bc="rgba(239,68,68,0.20)";}
                Label dot=new Label("●"); dot.setStyle("-fx-text-fill:"+fg+";-fx-font-size:8px;");
                Label lbl=new Label(v.replaceAll("[🏆✅⚡❌]","").trim()); lbl.setStyle("-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-font-weight:bold;");
                HBox badge=new HBox(5,dot,lbl); badge.setAlignment(Pos.CENTER);
                badge.setStyle("-fx-background-color:"+bg+";-fx-background-radius:20;-fx-border-color:"+bc+";-fx-border-radius:20;-fx-padding:3 8 3 8;");
                setGraphic(badge); setText(null); setStyle("-fx-alignment:CENTER;");
            }
        }); return col;
    }

    // ── Helpers ──────────────────────────────────────────────────
    private String hStr(Map<String,Object> m,String...keys){for(String k:keys){Object v=m.get(k);if(v!=null)return v.toString();}return "--";}
    private String hFmt(Map<String,Object> m,String...keys){for(String k:keys){Object v=m.get(k);if(v!=null){try{return String.format("%,.0f đ",Double.parseDouble(v.toString())).replace(",",".");}catch(Exception x){}}}return "--";}
    private double hDbl(Map<String,Object> m,String...keys){for(String k:keys){Object v=m.get(k);if(v!=null){try{return Double.parseDouble(v.toString());}catch(Exception x){}}}return 0;}
    private String hTime(String s){if(s==null||s.equals("--"))return "--";try{if(s.contains("T")){String[]p=s.split("T");String[]d=p[0].split("-");String t=p[1].length()>=5?p[1].substring(0,5):p[1];return d[2]+"/"+d[1]+"/"+d[0]+" "+t;}}catch(Exception x){}return s;}

    // ── Create auto-bid ──────────────────────────────────────────
    @FXML void onCreateAutoBid() {
        String product=cboProduct.getValue();
        if(product==null||product.isEmpty()){showAlert("Vui lòng chọn sản phẩm trước.");return;}
        String maxStr=txtMaxPrice.getText().trim().replaceAll("[^0-9]","");
        if(maxStr.isEmpty()){showAlert("Vui lòng nhập giá tối đa.");return;}
        try{
            long maxPrice=Long.parseLong(maxStr);
            String stepStr=txtStep.getText().trim().replaceAll("[^0-9]","");
            long stepVal=stepStr.isEmpty()?0:Long.parseLong(stepStr);
            if(stepVal<500_000L){lblStepError.setText("⚠ Bước giá tối thiểu là 500.000 đ");return;}
            else lblStepError.setText("");
            String times=txtMaxTimes.getText().trim().isEmpty()?"20":txtMaxTimes.getText().trim().replaceAll("[^0-9]","");
            int si=cboProduct.getSelectionModel().getSelectedIndex();
            String itemId=null; String currentP="0 đ";
            if(si>=0&&si<serverItems.size()){Item sel=serverItems.get(si);itemId=sel.getId();currentP=formatPrice((long)sel.getCurrentPrice());}
            final String fId=itemId,fCur=currentP; final long fMax=maxPrice,fStep=stepVal; final String fTimes=times,fProd=product;
            if(fId!=null){
                new Thread(()->{
                    try{AuctionClient cl=SessionManager.getClient(); User u=SessionManager.getCurrentUser();
                        if(cl==null||u==null) throw new Exception("Chưa đăng nhập");
                        ResponseMessage resp=cl.setAutoBid(fId,fMax);
                        Platform.runLater(()->{
                            if(resp!=null&&resp.getCode()==ResponseCode.SUCCESS) addRowToTable(fProd,fCur,fMax,fStep,fTimes,fId);
                            else showAlert("Lỗi server: "+(resp!=null?resp.getMessage():"Không phản hồi"));
                        });
                    }catch(Exception e){Platform.runLater(()->addRowToTable(fProd,fCur,fMax,fStep,fTimes,fId));}
                }).start();
            } else addRowToTable(fProd,fCur,fMax,fStep,fTimes,null);
        }catch(NumberFormatException ex){showAlert("Giá không hợp lệ.");}
    }

    private void addRowToTable(String product,String currentP,long maxPrice,long stepVal,String times,String itemId){
        String now=LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String status=autoBidOn?"Đang hoạt động":"Tạm dừng";
        if(editingIndex>=0&&editingIndex<tableAutoBid.getItems().size()){
            AutoBidRow row=tableAutoBid.getItems().get(editingIndex);
            row.product=product; row.currentPrice=currentP; row.maxPrice=formatPrice(maxPrice);
            row.step=formatPrice(stepVal); row.status=status; row.times="0 / "+times; row.time=now; row.itemId=itemId;
            tableAutoBid.refresh();
        } else {
            AutoBidRow row=new AutoBidRow(tableAutoBid.getItems().size()+1,product,currentP,formatPrice(maxPrice),formatPrice(stepVal),status,"0 / "+times,now);
            row.itemId=itemId; tableAutoBid.getItems().add(row);
        }
        // ✅ Lưu vào history cache
        User usr=SessionManager.getCurrentUser();
        if(usr!=null){
            SessionManager.addAutoBidHistory(usr.getUsername(),new SessionManager.AutoBidHistoryEntry(
                    itemId!=null?itemId:UUID.randomUUID().toString(),product,formatPrice(maxPrice),currentP,now));
            if(itemId!=null) SessionManager.addAutoBidCache(usr.getUsername(),itemId,product,formatPrice(maxPrice),currentP);
        }
        resetForm();
    }

    private void navigate(String path){
        try{URL res=getClass().getResource(path);if(res==null){System.out.println("Chưa có: "+path);return;}
            FXMLLoader l=new FXMLLoader(res); Parent root=l.load();
            Stage s=(Stage)tableAutoBid.getScene().getWindow();
            s.setScene(new Scene(root,s.getScene().getWidth(),s.getScene().getHeight()));}
        catch(Exception ex){ex.printStackTrace();}
    }

    private void loadAutoBidFromServer(){
        User user=SessionManager.getCurrentUser(); if(user==null) return;
        List<SessionManager.AutoBidCache> cache=SessionManager.getAutoBidCache(user.getUsername());
        if(cache.isEmpty()) return;
        tableAutoBid.getItems().clear(); int stt=1;
        for(SessionManager.AutoBidCache c:cache){
            AutoBidRow row=new AutoBidRow(stt++,c.itemName,c.currentPrice,c.maxPrice,"500.000 đ","Đang hoạt động","0 / 20","Đang chạy");
            row.itemId=c.itemId; tableAutoBid.getItems().add(row);
        }
    }

    private void showAlert(String msg){Alert a=new Alert(Alert.AlertType.WARNING);a.setTitle("Thông báo");a.setHeaderText(null);a.setContentText(msg);a.showAndWait();}
}
