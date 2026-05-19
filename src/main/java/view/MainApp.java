package view;

import client.AuctionClient;
import common.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MainApp {

    private static final String BG_DEEP     = "#0a0f1e";
    private static final String BG_PANEL    = "#111827";
    private static final String BG_INPUT    = "#1a2336";
    private static final String BG_ROW_ALT  = "#141d2e";
    private static final String ACCENT_BLUE = "#2563eb";
    private static final String ACCENT_GREEN= "#10b981";
    private static final String ACCENT_RED  = "#ef4444";
    private static final String BORDER      = "#1e2d4a";
    private static final String TEXT_MAIN   = "#f0f4ff";
    private static final String TEXT_MUTED  = "#8899bb";
    private static final String TEXT_LABEL  = "#c4cfe8";

    private AuctionClient client;
    private User currentUser;
    private ObservableList<Item> itemData;
    private TableView<Item> itemTable;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public void showWindow(String username, String role) {
        Stage stage = new Stage();
        stage.setTitle("Hệ thống Đấu giá – " + username + " [" + role + "]");

        try {
            client = new AuctionClient();
            client.connect("localhost", 9999);

            // ĐÃ SỬA: comment out startRealtimeListener vì method chưa có trong AuctionClient
            // client.startRealtimeListener(msg -> {
            //     Platform.runLater(() -> reloadItemsFromServer(itemData, itemTable));
            // });

            ResponseMessage loginResp = client.login(username, "123456");
            if (loginResp.getCode() == ResponseCode.SUCCESS) {
                Object data = loginResp.getData();
                if (data instanceof User) {
                    currentUser = (User) data;
                } else if (data instanceof Map) {
                    currentUser = gson.fromJson(gson.toJson(data), User.class);
                }
                System.out.println("MainApp: kết nối thành công, user=" + currentUser.getUsername());
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Đăng nhập lại thất bại: " + loginResp.getMessage());
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối server: " + e.getMessage());
            return;
        }

        ItemManager itemManager = new ItemManager();
        List<Item> itemList = itemManager.loadItems();
        ObservableList<Item> data = FXCollections.observableArrayList(itemList);
        itemData = data;

        TableView<Item> table = new TableView<>();
        itemTable = table;
        setupTable(table, data);
        VBox.setVgrow(table, Priority.ALWAYS);

        FilteredList<Item> filteredData = new FilteredList<>(data, p -> true);
        table.setItems(filteredData);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DEEP + ";");

        VBox topArea = new VBox(0);
        topArea.getChildren().addAll(
                buildMenuBar(stage, table, itemManager, itemList),
                buildHeader(username, role)
        );
        root.setTop(topArea);

        VBox centerArea = new VBox(12);
        centerArea.setPadding(new Insets(16, 24, 0, 24));

        HBox filterRow = buildFilterRow(filteredData);
        centerArea.getChildren().addAll(filterRow, table);
        root.setCenter(centerArea);

        VBox bottomArea = new VBox(0);
        bottomArea.setPadding(new Insets(12, 24, 20, 24));
        bottomArea.setSpacing(12);

        Label lblSelected = new Label("Chưa chọn sản phẩm nào.");
        lblSelected.setFont(Font.font("Segoe UI", 12));
        lblSelected.setTextFill(Color.web(TEXT_MUTED));

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, selected) -> {
            if (selected != null) {
                lblSelected.setText("Đang chọn: " + selected.getName()
                        + "   |   Giá hiện tại: " + String.format("%,.0f đ", selected.getCurrentPrice()));

                // ĐÃ SỬA: comment out vì subscribeItem/unsubscribeItem chưa có trong AuctionClient
                // if (oldItem != null) client.unsubscribeItem(oldItem.getId());
                // client.subscribeItem(selected.getId());
            }
        });

        HBox bidRow = buildBidRow(table, itemManager, itemList);
        VBox addBox = buildAddBox(role, table, data, itemManager, itemList);

        bottomArea.getChildren().addAll(
                new Separator() {{ setStyle("-fx-background-color: " + BORDER + ";"); }},
                lblSelected,
                bidRow,
                addBox
        );
        root.setBottom(bottomArea);

        Scene scene = new Scene(root, 1000, 720);
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar buildMenuBar(Stage stage, TableView<Item> table,
                                 ItemManager itemManager, List<Item> itemList) {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: " + BG_PANEL + ";"
                + "-fx-border-color: transparent transparent " + BORDER + " transparent;"
                + "-fx-border-width: 0 0 1 0;");

        Menu menu = new Menu("☰  Chức năng");
        menu.setStyle("-fx-font-size: 13;");

        MenuItem profileItem  = new MenuItem("👤  Thông tin cá nhân");
        MenuItem historyItem  = new MenuItem("📋  Lịch sử đấu giá");
        MenuItem wonItem      = new MenuItem("🏆  Sản phẩm đã thắng");
        MenuItem chartItem    = new MenuItem("📈  Biểu đồ giá");
        MenuItem logoutItem   = new MenuItem("🚪  Đăng xuất");

        String miStyle = "-fx-font-size: 13; -fx-padding: 6 20;";
        for (MenuItem mi : new MenuItem[]{profileItem, historyItem, wonItem, chartItem, logoutItem}) {
            mi.setStyle(miStyle);
        }

        profileItem.setOnAction(e -> new ProfileForm().show(client, currentUser));
        historyItem.setOnAction(e -> new MyBidsHistoryUI().show(client, currentUser));
        wonItem.setOnAction(e -> new WonProductsUI().show(client, currentUser));

        chartItem.setOnAction(e -> {
            Item selected = table.getSelectionModel().getSelectedItem();
            if (selected == null)
                showAlert(Alert.AlertType.WARNING, "Chọn sản phẩm", "Hãy chọn 1 sản phẩm để xem biểu đồ");
            else
                new PriceChartUI().show(client, selected.getId(), selected.getName());
        });

        logoutItem.setOnAction(e -> {
            try { client.disconnect(); } catch (Exception ex) {}
            stage.close();
            new LoginApp().start(new Stage());
        });

        menu.getItems().addAll(
                profileItem, historyItem, wonItem, chartItem,
                new SeparatorMenuItem(),
                logoutItem
        );
        menuBar.getMenus().add(menu);
        return menuBar;
    }

    private HBox buildHeader(String username, String role) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setStyle("-fx-background-color: " + BG_PANEL + ";"
                + "-fx-border-color: transparent transparent " + BORDER + " transparent;"
                + "-fx-border-width: 0 0 1 0;");

        StackPane avatar = new StackPane();
        avatar.setPrefSize(36, 36);
        avatar.setStyle("-fx-background-color: #1e3a6e; -fx-background-radius: 18;");
        Label avatarLbl = new Label("👤");
        avatarLbl.setFont(Font.font(16));
        avatar.getChildren().add(avatarLbl);

        VBox userInfo = new VBox(2);
        Label lblName = new Label(username);
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblName.setTextFill(Color.web(TEXT_MAIN));
        Label lblRole = new Label(role);
        lblRole.setFont(Font.font("Segoe UI", 11));
        lblRole.setTextFill(Color.web(
                "ADMIN".equalsIgnoreCase(role) ? "#f59e0b" :
                        "SELLER".equalsIgnoreCase(role) ? "#a78bfa" : ACCENT_BLUE
        ));
        userInfo.getChildren().addAll(lblName, lblRole);

        Label lblCenter = new Label("🔨  Hệ thống Đấu giá Trực tuyến");
        lblCenter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblCenter.setTextFill(Color.web(TEXT_MAIN));

        Region left = new Region();
        Region right = new Region();
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        double balance = currentUser != null ? currentUser.getBalance() : 0;
        VBox balanceBox = new VBox(2);
        balanceBox.setAlignment(Pos.CENTER_RIGHT);
        Label lblBalanceLbl = new Label("Số dư");
        lblBalanceLbl.setFont(Font.font("Segoe UI", 11));
        lblBalanceLbl.setTextFill(Color.web(TEXT_MUTED));
        Label lblBalance = new Label(String.format("%,.0f đ", balance));
        lblBalance.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblBalance.setTextFill(Color.web(ACCENT_GREEN));
        balanceBox.getChildren().addAll(lblBalanceLbl, lblBalance);

        HBox leftGroup = new HBox(10, avatar, userInfo);
        leftGroup.setAlignment(Pos.CENTER_LEFT);

        header.getChildren().addAll(leftGroup, left, lblCenter, right, balanceBox);
        return header;
    }

    private HBox buildFilterRow(FilteredList<Item> filteredData) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblSearch = new Label("🔍");
        lblSearch.setTextFill(Color.web(TEXT_MUTED));

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm tên sản phẩm...");
        txtSearch.setPrefWidth(220);
        txtSearch.setPrefHeight(36);
        txtSearch.setStyle(inputStyle());

        Label lblFilter = new Label("Trạng thái:");
        lblFilter.setFont(Font.font("Segoe UI", 12));
        lblFilter.setTextFill(Color.web(TEXT_LABEL));

        ComboBox<String> cbFilter = new ComboBox<>();
        cbFilter.getItems().addAll("Tất cả", "ACTIVE", "PENDING", "ENDED");
        cbFilter.setValue("Tất cả");
        cbFilter.setPrefHeight(36);
        cbFilter.setStyle(
                "-fx-background-color: " + BG_INPUT + ";"
                        + "-fx-border-color: " + BORDER + ";"
                        + "-fx-border-radius: 8;"
                        + "-fx-background-radius: 8;"
                        + "-fx-font-size: 12;"
        );

        Runnable updateFilter = () -> filteredData.setPredicate(item -> {
            boolean matchName   = item.getName().toLowerCase().contains(txtSearch.getText().toLowerCase());
            boolean matchStatus = cbFilter.getValue().equals("Tất cả") || item.getStatus().equals(cbFilter.getValue());
            return matchName && matchStatus;
        });
        txtSearch.textProperty().addListener((obs, o, v) -> updateFilter.run());
        cbFilter.valueProperty().addListener((obs, o, v) -> updateFilter.run());

        row.getChildren().addAll(lblSearch, txtSearch, lblFilter, cbFilter);
        return row;
    }

    private HBox buildBidRow(TableView<Item> table, ItemManager itemManager, List<Item> itemList) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblBidLbl = new Label("Giá đặt:");
        lblBidLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblBidLbl.setTextFill(Color.web(TEXT_LABEL));

        TextField txtBidAmount = new TextField();
        txtBidAmount.setPromptText("Nhập số tiền (VNĐ)...");
        txtBidAmount.setPrefWidth(220);
        txtBidAmount.setPrefHeight(38);
        txtBidAmount.setStyle(inputStyle());

        Button btnBid = new Button("🔨  Đặt Giá");
        btnBid.setPrefHeight(38);
        btnBid.setPrefWidth(130);
        btnBid.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnBid.setStyle(
                "-fx-background-color: " + ACCENT_GREEN + ";"
                        + "-fx-text-fill: white;"
                        + "-fx-background-radius: 8;"
                        + "-fx-cursor: hand;"
        );
        btnBid.setCursor(Cursor.HAND);
        btnBid.setOnMouseEntered(e -> btnBid.setStyle(btnBid.getStyle().replace(ACCENT_GREEN, "#059669")));
        btnBid.setOnMouseExited(e ->  btnBid.setStyle(btnBid.getStyle().replace("#059669", ACCENT_GREEN)));

        btnBid.setOnAction(e -> {
            Item selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Hãy chọn sản phẩm");
                return;
            }
            if (!"ACTIVE".equals(selected.getStatus())) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Sản phẩm không hoạt động");
                return;
            }
            try {
                double amount = Double.parseDouble(txtBidAmount.getText().replace(",", "").replace(".", ""));
                if (amount <= selected.getCurrentPrice()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá phải cao hơn giá hiện tại!");
                    return;
                }
                Bid bid = new Bid();
                bid.setItemId(selected.getId());
                bid.setAmount(amount);
                new Thread(() -> {
                    try {
                        ResponseMessage resp = client.placeBid(bid);
                        Platform.runLater(() -> {
                            if (resp.getCode() == ResponseCode.SUCCESS) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đặt giá thành công!");
                                txtBidAmount.clear();
                                reloadItemsFromServer(itemData, itemTable);
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", resp.getMessage());
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage()));
                    }
                }).start();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số hợp lệ!");
            }
        });

        row.getChildren().addAll(lblBidLbl, txtBidAmount, btnBid);
        return row;
    }

    private VBox buildAddBox(String role, TableView<Item> table,
                             ObservableList<Item> data,
                             ItemManager itemManager, List<Item> itemList) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle(
                "-fx-background-color: " + BG_PANEL + ";"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: " + BORDER + ";"
                        + "-fx-border-radius: 10;"
        );

        Label lblSeller = new Label("KHU VỰC NGƯỜI BÁN");
        lblSeller.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblSeller.setTextFill(Color.web("#a78bfa"));

        TextField txtNewName  = new TextField();
        txtNewName.setPromptText("Tên sản phẩm");
        txtNewName.setPrefHeight(36);
        txtNewName.setPrefWidth(240);
        txtNewName.setStyle(inputStyle());

        TextField txtNewPrice = new TextField();
        txtNewPrice.setPromptText("Giá khởi điểm (VNĐ)");
        txtNewPrice.setPrefHeight(36);
        txtNewPrice.setPrefWidth(200);
        txtNewPrice.setStyle(inputStyle());

        Button btnAdd = new Button("📦  Đăng bán");
        btnAdd.setPrefHeight(36);
        btnAdd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btnAdd.setStyle(
                "-fx-background-color: #7c3aed;"
                        + "-fx-text-fill: white;"
                        + "-fx-background-radius: 8;"
                        + "-fx-cursor: hand;"
        );
        btnAdd.setCursor(Cursor.HAND);

        btnAdd.setOnAction(e -> {
            try {
                String name  = txtNewName.getText();
                double price = Double.parseDouble(txtNewPrice.getText().replace(",", "").replace(".", ""));
                if (name.isEmpty()) throw new Exception("Tên sản phẩm không được để trống!");
                LocalDateTime start = LocalDateTime.now();
                LocalDateTime end   = start.plusDays(7);

                // ĐÃ SỬA: dùng setter thay vì constructor có tham số (tránh lỗi "no suitable constructor")
                Item newItem = new Item();
                newItem.setName(name);
                newItem.setDescription("Sản phẩm mới");
                newItem.setStartingPrice(price);
                newItem.setCurrentPrice(price);
                newItem.setStartTime(start);
                newItem.setEndTime(end);
                newItem.setSellerName(currentUser.getUsername());
                newItem.setStatus("ACTIVE");

                itemList.add(newItem);
                data.add(newItem);
                itemManager.saveItems(itemList);
                txtNewName.clear();
                txtNewPrice.clear();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đăng bán sản phẩm!");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
            }
        });

        HBox inputRow = new HBox(10, txtNewName, txtNewPrice, btnAdd);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(lblSeller, inputRow);

        if (!"SELLER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            box.setVisible(false);
            box.setManaged(false);
        }

        return box;
    }

    @SuppressWarnings("unchecked")
    private void setupTable(TableView<Item> table, ObservableList<Item> data) {
        table.setStyle(
                "-fx-background-color: " + BG_PANEL + ";"
                        + "-fx-border-color: " + BORDER + ";"
                        + "-fx-border-radius: 10;"
                        + "-fx-background-radius: 10;"
                        + "-fx-table-cell-border-color: " + BORDER + ";"
        );
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Không có dữ liệu") {{
            setTextFill(Color.web(TEXT_MUTED));
            setFont(Font.font("Segoe UI", 13));
        }});

        TableColumn<Item, String> colName = new TableColumn<>("Tên sản phẩm");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Double> colPrice = new TableColumn<>("Giá hiện tại");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); return; }
                setText(String.format("%,.0f đ", val));
                setTextFill(Color.web(ACCENT_GREEN));
                setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            }
        });

        TableColumn<Item, String> colBidder = new TableColumn<>("Người trả cao nhất");
        colBidder.setCellValueFactory(new PropertyValueFactory<>("currentBidderName"));

        TableColumn<Item, String> colSeller = new TableColumn<>("Người bán");
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        TableColumn<Item, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(val);
                badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                badge.setPadding(new Insets(3, 10, 3, 10));
                switch (val) {
                    case "ACTIVE":
                        badge.setTextFill(Color.web(ACCENT_GREEN));
                        badge.setStyle("-fx-background-color: rgba(16,185,129,0.12); -fx-background-radius: 20;");
                        break;
                    case "PENDING":
                        badge.setTextFill(Color.web("#f59e0b"));
                        badge.setStyle("-fx-background-color: rgba(245,158,11,0.12); -fx-background-radius: 20;");
                        break;
                    case "ENDED":
                        badge.setTextFill(Color.web(ACCENT_RED));
                        badge.setStyle("-fx-background-color: rgba(239,68,68,0.12); -fx-background-radius: 20;");
                        break;
                    default:
                        badge.setTextFill(Color.web(TEXT_MUTED));
                        badge.setStyle("-fx-background-color: rgba(136,153,187,0.12); -fx-background-radius: 20;");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        table.getColumns().addAll(colName, colPrice, colBidder, colSeller, colStatus);
        table.setItems(data);

        table.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setStyle("-fx-background-color: " + BG_PANEL + ";");
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty())
                    row.setStyle("-fx-background-color: rgba(37,99,235,0.08);");
            });
            row.setOnMouseExited(e -> row.setStyle("-fx-background-color: " + BG_PANEL + ";"));
            return row;
        });
    }

    private void reloadItemsFromServer(ObservableList<Item> data, TableView<Item> table) {
        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.GET_ACTIVE_ITEMS, null);
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    java.lang.reflect.Type listType =
                            new TypeToken<List<Item>>(){}.getType();
                    List<Item> newItems = gson.fromJson(gson.toJson(resp.getData()), listType);
                    if (newItems != null) {
                        Platform.runLater(() -> {
                            data.setAll(newItems);
                            table.refresh();
                            System.out.println("✅ Đã reload " + newItems.size() + " sản phẩm từ server");
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Reload items error: " + e.getMessage());
            }
        }).start();
    }

    private String inputStyle() {
        return "-fx-background-color: " + BG_INPUT + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-text-fill: " + TEXT_MAIN + ";"
                + "-fx-prompt-text-fill: #4a5a7a;"
                + "-fx-font-size: 13;"
                + "-fx-padding: 0 12;";
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}