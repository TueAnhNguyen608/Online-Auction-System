package server.service;

import common.model.exception.DataValidationException;
import common.model.item.Item;
import common.model.item.ItemStatus;
import server.repository.ItemRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ItemService {

    private static volatile ItemService instance;
    private final ItemRepository itemRepo;

    private ItemService() {
        this.itemRepo = ItemRepository.getInstance();
    }

    public static ItemService getInstance() {
        if (instance == null) {
            synchronized (ItemService.class) {
                if (instance == null) {
                    instance = new ItemService();
                }
            }
        }
        return instance;
    }

    public List<Item> getActiveItems() throws SQLException {
        return itemRepo.findActiveItems();
    }

    public Item getItemById(String itemId) throws SQLException {
        return itemRepo.findById(itemId).orElse(null);
    }

    public Item createItem(Item item, String sellerId, String sellerName) throws SQLException, DataValidationException {
        validateItem(item);
        item.setSellerId(sellerId);
        item.setSellerName(sellerName);
        item.setStatus(ItemStatus.ACTIVE);
        item.setStartTime(LocalDateTime.now());

        boolean success = itemRepo.save(item);
        if (!success) {
            throw new DataValidationException("Không thể tạo sản phẩm");
        }
        return item;
    }

    private void validateItem(Item item) throws DataValidationException {
        DataValidationException errors = new DataValidationException("Dữ liệu sản phẩm không hợp lệ");

        if (item.getName() == null || item.getName().trim().isEmpty()) {
            errors.addError("Tên sản phẩm không được để trống");
        }
        if (item.getStartingPrice() <= 0) {
            errors.addError("Giá khởi điểm phải lớn hơn 0");
        }
        if (item.getEndTime() == null || item.getEndTime().isBefore(LocalDateTime.now().plusMinutes(5))) {
            errors.addError("Thời gian kết thúc phải sau ít nhất 5 phút");
        }

        if (errors.hasErrors()) {
            throw errors;
        }
    }
}