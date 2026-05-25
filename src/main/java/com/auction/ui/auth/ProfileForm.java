package com.auction.ui.auth;

import com.auction.client.AuctionClient;
import com.auction.common.model.communication.CommandType;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.user.User;
import com.auction.ui.utils.DialogUtils;
import com.auction.ui.components.SidebarHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class ProfileForm {

    private static final String BG     = "#0a0e27";
    private static final String BG_IN  = "#1a1f45";
    private static final String BORDER = "#2d3270";
    private static final String TEXT   = "#f0f4ff";
    private static final String MUTED  = "#8899bb";
    private static final String LABEL  = "#c4cfe8";
    private static final String BLUE   = "#2563eb";
    private static final String GREEN  = "#10b981";

    private AuctionClient client;
    private User currentUser;
    private Stage stage;

    private Label lblName, lblEmail, lblPhone, lblAddress, lblBirth;
    private TextField tfName, tfEmail, tfPhone, tfAddress;
    private Button btnEdit;
    private HBox editActions;
    private Label lblMsg;

    public void show(AuctionClient client, User user) {
        this.client = client;
        this.currentUser = user;
        this.stage = new Stage();
        stage.setTitle("Quản lý tài khoản – " + user.getUsername());

        HBox root = new HBox(0);
        root.setPrefSize(980, 600);
        root.setStyle("-fx-background-color:" + BG + ";");

        String[][] menu = {{"👤","Thông tin cá nhân"},{"🔒","Đổi mật khẩu"},{"🚪","Đăng xuất"}};
        VBox sidebar = SidebarHelper.build(currentUser, menu, 0, idx -> {
            if (idx == 1) showChangePassDialog();
            else if (idx == 2) {
                try { client.disconnect(); } catch (Exception ignored) {}
                stage.close();
                new LoginApp().start(new Stage());
            }
        });

        VBox content = buildContent();
        HBox.setHgrow(content, Priority.ALWAYS);

        root.getChildren().addAll(sidebar, content);

        Scene scene = new Scene(root, 980, 600);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private VBox buildContent() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color:" + BG + ";");
        panel.setPadding(new Insets(26, 30, 26, 30));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(header, new Insets(0, 0, 20, 0));

        Label t = new Label("Thông tin cá nhân");
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        t.setTextFill(Color.web(TEXT));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        btnEdit = new Button("✏  Chỉnh sửa");
        btnEdit.setPrefHeight(36);
        btnEdit.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btnEdit.setStyle("-fx-background-color:" + BLUE + ";-fx-text-fill:white;"
                + "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:0 16;");
        btnEdit.setOnAction(e -> toggleEdit());
        header.getChildren().addAll(t, sp, btnEdit);

        Label secTitle = new Label("Thông tin cơ bản");
        secTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        secTitle.setTextFill(Color.web(LABEL));
        VBox.setMargin(secTitle, new Insets(0, 0, 12, 0));

        VBox infoBox = new VBox(0);
        infoBox.setStyle("-fx-background-color:#11153a;-fx-background-radius:12;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:12;");

        String un = currentUser.getUsername();
        String em = currentUser.getEmail() != null ? currentUser.getEmail() : "";

        lblName    = val(un);    tfName    = editField(un);
        lblEmail   = val(em);    tfEmail   = editField(em);
        lblPhone   = val("0123 456 789"); tfPhone = editField("0123456789");
        lblAddress = val("123 Đường ABC, Quận 1, TP. Hồ Chí Minh"); tfAddress = editField("123 Đường ABC...");
        lblBirth   = val("01/01/1995");

        infoBox.getChildren().addAll(
                infoRow("Họ và tên",     lblName,    tfName),
                infoRow("Tên đăng nhập", val(un),    null),
                infoRow("Email",         lblEmail,   tfEmail),
                infoRow("Số điện thoại", lblPhone,   tfPhone),
                infoRow("Địa chỉ",       lblAddress, tfAddress),
                infoRow("Ngày sinh",     lblBirth,   null)
        );
        VBox.setMargin(infoBox, new Insets(0, 0, 20, 0));

        editActions = new HBox(10);
        editActions.setVisible(false); editActions.setManaged(false);
        Button btnSave = new Button("Lưu thay đổi");
        btnSave.setPrefHeight(36);
        btnSave.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btnSave.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:0 16;");
        btnSave.setOnAction(e -> saveChanges());
        Button btnCancel = new Button("Hủy");
        btnCancel.setPrefHeight(36);
        btnCancel.setFont(Font.font("Segoe UI", 12));
        btnCancel.setStyle("-fx-background-color:" + BG_IN + ";-fx-border-color:" + BORDER + ";-fx-border-radius:8;-fx-background-radius:8;-fx-text-fill:" + MUTED + ";-fx-cursor:hand;-fx-padding:0 16;");
        btnCancel.setOnAction(e -> cancelEdit());
        editActions.getChildren().addAll(btnSave, btnCancel);
        VBox.setMargin(editActions, new Insets(0, 0, 8, 0));

        lblMsg = new Label(); lblMsg.setFont(Font.font("Segoe UI", 12)); lblMsg.setWrapText(true);
        VBox.setMargin(lblMsg, new Insets(0, 0, 16, 0));

        Label secStat = new Label("Thống kê tài khoản");
        secStat.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        secStat.setTextFill(Color.web(LABEL));
        VBox.setMargin(secStat, new Insets(0, 0, 12, 0));

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(14); statsGrid.setVgap(14);
        ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(50);
        statsGrid.getColumnConstraints().addAll(cc, new ColumnConstraints(){{ setPercentWidth(50); }});

        double bal = currentUser.getBalance();
        statsGrid.add(statCard("💰", "Tổng số dư",           String.format("%,.0f đ", bal), "#2563eb"), 0, 0);
        statsGrid.add(statCard("🏠", "Tổng đã chi tiêu",     "125.750.000 đ",               "#2563eb"), 1, 0);
        statsGrid.add(statCard("📦", "Số sản phẩm đã đấu",   "128",                          "#10b981"), 0, 1);
        statsGrid.add(statCard("🏆", "Số sản phẩm đã thắng", "23",                           "#f59e0b"), 1, 1);

        panel.getChildren().addAll(header, secTitle, infoBox, editActions, lblMsg, secStat, statsGrid);
        return panel;
    }

    private HBox infoRow(String key, Label valLbl, TextField tf) {
        Label k = new Label(key); k.setFont(Font.font("Segoe UI", 12)); k.setTextFill(Color.web(MUTED));
        k.setPrefWidth(130); k.setMinWidth(130);
        HBox row = new HBox(16, k, valLbl);
        if (tf != null) row.getChildren().add(tf);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 20, 12, 20));
        row.setStyle("-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;");
        HBox.setHgrow(valLbl, Priority.ALWAYS);
        if (tf != null) HBox.setHgrow(tf, Priority.ALWAYS);
        return row;
    }

    private Label val(String s) {
        Label l = new Label(s); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); l.setTextFill(Color.web(TEXT)); l.setWrapText(true); return l;
    }

    private TextField editField(String s) {
        TextField tf = new TextField(s); tf.setPrefHeight(34); tf.setMaxWidth(Double.MAX_VALUE);
        tf.setVisible(false); tf.setManaged(false);
        tf.setStyle("-fx-background-color:" + BG_IN + ";-fx-border-color:#2563eb;-fx-border-radius:6;-fx-background-radius:6;-fx-text-fill:" + TEXT + ";-fx-font-size:13;-fx-padding:0 10;");
        return tf;
    }

    private HBox statCard(String icon, String label, String value, String color) {
        HBox card = new HBox(14); card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:#11153a;-fx-background-radius:10;-fx-border-color:" + BORDER + ";-fx-border-radius:10;");
        StackPane ic = new StackPane(); ic.setPrefSize(44, 44); ic.setMinSize(44, 44);
        ic.setStyle("-fx-background-color:" + color + "22;-fx-background-radius:8;");
        Label emo = new Label(icon); emo.setFont(Font.font(20)); ic.getChildren().add(emo);
        VBox info = new VBox(3);
        Label lbl = new Label(label); lbl.setFont(Font.font("Segoe UI", 11)); lbl.setTextFill(Color.web(MUTED));
        Label val = new Label(value); val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15)); val.setTextFill(Color.web(TEXT));
        info.getChildren().addAll(lbl, val); HBox.setHgrow(info, Priority.ALWAYS);
        card.getChildren().addAll(ic, info); return card;
    }

    private boolean editing = false;
    private void toggleEdit() {
        editing = !editing;
        lblName.setVisible(!editing);    lblName.setManaged(!editing);    tfName.setVisible(editing);    tfName.setManaged(editing);
        lblEmail.setVisible(!editing);   lblEmail.setManaged(!editing);   tfEmail.setVisible(editing);   tfEmail.setManaged(editing);
        lblPhone.setVisible(!editing);   lblPhone.setManaged(!editing);   tfPhone.setVisible(editing);   tfPhone.setManaged(editing);
        lblAddress.setVisible(!editing); lblAddress.setManaged(!editing); tfAddress.setVisible(editing); tfAddress.setManaged(editing);
        editActions.setVisible(editing); editActions.setManaged(editing);
        btnEdit.setText(editing ? "Đang chỉnh sửa..." : "✏  Chỉnh sửa");
        lblMsg.setText("");
    }

    private void saveChanges() {
        String email = tfEmail.getText().trim();
        if (email.isEmpty()) { lblMsg.setTextFill(Color.web("#ef4444")); lblMsg.setText("Email không được để trống!"); return; }
        User updated = new User(); updated.setId(currentUser.getId()); updated.setUsername(currentUser.getUsername());
        updated.setEmail(email); updated.setRole(currentUser.getRole());
        new Thread(() -> {
            try {
                ResponseMessage resp = client.sendRequest(CommandType.UPDATE_USER_INFO, updated);
                Platform.runLater(() -> {
                    if (resp.getCode() == ResponseCode.SUCCESS) {
                        lblName.setText(tfName.getText()); lblEmail.setText(email);
                        lblPhone.setText(tfPhone.getText()); lblAddress.setText(tfAddress.getText());
                        currentUser.setEmail(email); editing = true; toggleEdit();
                        lblMsg.setTextFill(Color.web(GREEN)); lblMsg.setText("✓ Cập nhật thành công!");
                    } else { lblMsg.setTextFill(Color.web("#ef4444")); lblMsg.setText("Lỗi: " + resp.getMessage()); }
                });
            } catch (Exception e) { Platform.runLater(() -> { lblMsg.setTextFill(Color.web("#ef4444")); lblMsg.setText("Lỗi kết nối!"); }); }
        }).start();
    }

    private void cancelEdit() { editing = true; toggleEdit(); }

    private void showChangePassDialog() {
        Stage d = new Stage();
        d.setTitle("Đổi mật khẩu");
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color:#11153a;");
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setPrefWidth(380);

        Label t = new Label("Đổi mật khẩu"); t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); t.setTextFill(Color.web(TEXT));
        PasswordField pOld = pf("Mật khẩu cũ"); PasswordField pNew = pf("Mật khẩu mới (≥6 ký tự)"); PasswordField pCon = pf("Nhập lại mật khẩu mới");
        Label lblErr = new Label(); lblErr.setFont(Font.font("Segoe UI", 12)); lblErr.setTextFill(Color.web("#ef4444")); lblErr.setWrapText(true);
        Button btnOk = new Button("Đổi mật khẩu"); btnOk.setMaxWidth(Double.MAX_VALUE); btnOk.setPrefHeight(44);
        btnOk.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnOk.setStyle("-fx-background-color:" + BLUE + ";-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;");
        root.getChildren().addAll(t,
                lbl("Mật khẩu cũ"), pOld, lbl("Mật khẩu mới"), pNew, lbl("Xác nhận"), pCon,
                lblErr, btnOk);
        btnOk.setOnAction(e -> {
            if (pOld.getText().isEmpty()||pNew.getText().isEmpty()) { lblErr.setText("Nhập đầy đủ!"); return; }
            if (!pNew.getText().equals(pCon.getText())) { lblErr.setText("Mật khẩu không khớp!"); return; }
            if (pNew.getText().length() < 6) { lblErr.setText("Mật khẩu phải ≥6 ký tự!"); return; }
            Map<String,String> data = new HashMap<>(); data.put("oldPassword",pOld.getText()); data.put("newPassword",pNew.getText());
            new Thread(() -> {
                try {
                    ResponseMessage resp = client.sendRequest(CommandType.CHANGE_PASSWORD, data);
                    Platform.runLater(() -> {
                        if (resp.getCode() == ResponseCode.SUCCESS) { DialogUtils.showInfo("Thành công","Đổi mật khẩu thành công!"); d.close(); }
                        else lblErr.setText("Lỗi: " + resp.getMessage());
                    });
                } catch (Exception ex) { Platform.runLater(() -> lblErr.setText("Lỗi kết nối!")); }
            }).start();
        });
        d.setScene(new Scene(root)); d.show();
    }

    private PasswordField pf(String p) { PasswordField pf = new PasswordField(); pf.setPromptText(p); pf.setPrefHeight(42); pf.setStyle("-fx-background-color:#1a1f45;-fx-border-color:#2d3270;-fx-border-radius:8;-fx-background-radius:8;-fx-text-fill:#f0f4ff;-fx-prompt-text-fill:#4a5a7a;-fx-font-size:13;-fx-padding:0 12;"); return pf; }
    private Label lbl(String s) { Label l = new Label(s); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); l.setTextFill(Color.web(LABEL)); return l; }
}