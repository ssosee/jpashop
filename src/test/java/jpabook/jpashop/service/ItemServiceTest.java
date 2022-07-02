package jpabook.jpashop.service;


import jpabook.jpashop.domain.item.Album;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.domain.item.Movie;
import jpabook.jpashop.repository.ItemRepository;
import org.hibernate.internal.build.AllowSysOut;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class ItemServiceTest {

    @Autowired
    ItemRepository itemRepository;
    @Autowired
    ItemService itemService;

    @Test
    public void 상품_등록() throws Exception {
        //given
        Item album = new Album();
        album.setName("앨범");
        //when
        itemService.saveItem(album);
        //then
        assertEquals(album, itemService.findItemOne(album.getId()));
    }

    @Test
    public void 상품_전체_조회() throws Exception {
        //given
        Item album = new Album();
        album.setName("앨범");
        Item book = new Book();
        book.setName("책");
        Item movie = new Movie();
        movie.setName("영화");
        //when
        itemService.saveItem(album);
        itemService.saveItem(book);
        itemService.saveItem(movie);

        List<Item> item = itemService.findItem();
        //then
        assertEquals(3, item.size());
    }

    @Test
    public void 상품_단건_조회() throws Exception {
        //given
        Item album = new Album();
        album.setName("앨범");
        Item book = new Book();
        book.setName("책");
        Item movie = new Movie();
        movie.setName("영화");
        //when
        itemService.saveItem(album);
        itemService.saveItem(book);
        itemService.saveItem(movie);

        //then
        assertEquals(album, itemService.findItemOne(album.getId()));
        assertEquals(book, itemService.findItemOne(book.getId()));
        assertEquals(movie, itemService.findItemOne(movie.getId()));
    }
}