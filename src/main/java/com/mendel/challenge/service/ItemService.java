package com.mendel.challenge.service;

import com.mendel.challenge.dto.ItemRequest;
import com.mendel.challenge.dto.ItemResponse;
import com.mendel.challenge.exception.ResourceNotFoundException;
import com.mendel.challenge.model.Item;
import com.mendel.challenge.repository.ItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer: business logic and transaction boundaries.
 *
 * Dependencies come in through the constructor (constructor injection). Spring
 * supplies them automatically; no @Autowired is needed when there is a single constructor.
 */
@Service
@Transactional(readOnly = true)
public class ItemService {

	private final ItemRepository itemRepository;

	public ItemService(ItemRepository itemRepository) {
		this.itemRepository = itemRepository;
	}

	public List<ItemResponse> findAll() {
		return itemRepository.findAll().stream().map(ItemResponse::from).toList();
	}

	public ItemResponse findById(Long id) {
		return itemRepository.findById(id)
				.map(ItemResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("Item " + id + " not found"));
	}

	@Transactional
	public ItemResponse create(ItemRequest request) {
		Item saved = itemRepository.save(new Item(request.name()));
		return ItemResponse.from(saved);
	}
}
