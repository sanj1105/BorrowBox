package com.borrowbox.borrowbox.service;

import com.borrowbox.borrowbox.model.Item;
import com.borrowbox.borrowbox.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Item getOwnedItem(Long itemId, Long actingUserId) {
        return loadOwned(itemId, actingUserId);
    }

    public Item update(Long itemId, Long actingUserId,
                       String name, String description, String category) {
        Item item = loadOwned(itemId, actingUserId);
        item.setName(name);
        item.setDescription(description);
        item.setCategory(category);
        return itemRepository.save(item);
    }

    public Item setAvailable(Long itemId, Long actingUserId, boolean available) {
        Item item = loadOwned(itemId, actingUserId);
        item.setAvailable(available);
        return itemRepository.save(item);
    }

    private Item loadOwned(Long itemId, Long actingUserId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BorrowRuleException("Item not found"));
        if (!item.getOwner().getId().equals(actingUserId)) {
            throw new BorrowRuleException("Only the item owner can do this");
        }
        return item;
    }
}