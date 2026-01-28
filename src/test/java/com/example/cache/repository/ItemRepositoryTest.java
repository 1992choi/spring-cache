package com.example.cache.repository;

import com.example.cache.model.Item;
import com.example.cache.model.ItemCreateRequest;
import com.example.cache.model.ItemUpdateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest {
    ItemRepository itemRepository = new ItemRepository();

    @Test
    void readAll() {
        // given
        List<Item> items = IntStream.range(0, 3)
                .mapToObj(idx -> itemRepository.create(Item.create(new ItemCreateRequest("data" + idx))))
                .toList();

        // when
        List<Item> firstPage = itemRepository.readAll(1L, 2L);
        List<Item> secondPage = itemRepository.readAll(2L, 2L);

        // then
        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0).getItemId()).isEqualTo(items.get(2).getItemId());
        assertThat(firstPage.get(1).getItemId()).isEqualTo(items.get(1).getItemId());

        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.get(0).getItemId()).isEqualTo(items.get(0).getItemId());
    }

    @Test
    void readAllInfiniteScroll() {
        // given
        List<Item> items = IntStream.range(0, 3)
                .mapToObj(idx -> itemRepository.create(Item.create(new ItemCreateRequest("data" + idx))))
                .toList();

        // when
        List<Item> firstPage = itemRepository.readAllInfiniteScroll(null, 2L);
        List<Item> secondPage = itemRepository.readAllInfiniteScroll(firstPage.getLast().getItemId(), 2L);

        // then
        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0).getItemId()).isEqualTo(items.get(2).getItemId());
        assertThat(firstPage.get(1).getItemId()).isEqualTo(items.get(1).getItemId());

        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.get(0).getItemId()).isEqualTo(items.get(0).getItemId());
    }

    @Test
    void create() {
        // given
        Item item = Item.create(new ItemCreateRequest("data"));

        // when
        Item result = itemRepository.create(item);

        // then
        Optional<Item> itemOptional = itemRepository.read(item.getItemId());
        assertThat(itemOptional).isPresent();
        assertThat(itemOptional.get().getItemId()).isEqualTo(result.getItemId());
        assertThat(itemOptional.get().getData()).isEqualTo(result.getData());
    }

    @Test
    void update() {
        // given
        Item item = itemRepository.create(Item.create(new ItemCreateRequest("data")));

        // when
        item.update(new ItemUpdateRequest("data2"));
        Item result = itemRepository.update(item);

        // then
        Optional<Item> itemOptional = itemRepository.read(item.getItemId());
        assertThat(itemOptional).isPresent();
        assertThat(itemOptional.get().getItemId()).isEqualTo(result.getItemId());
        assertThat(itemOptional.get().getData()).isEqualTo("data2");
    }

    @Test
    void delete() {
        // given
        Item item = itemRepository.create(Item.create(new ItemCreateRequest("data")));

        // when
        itemRepository.delete(itemRepository.read(item.getItemId()).get());

        // then
        Optional<Item> itemOptional = itemRepository.read(item.getItemId());
        assertThat(itemOptional).isEmpty();
    }
}