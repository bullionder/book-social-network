package com.bullionder.book_network.book;

import org.springframework.stereotype.Service;

@Service
public class BookMapper {
    public Book toBook(BookRequest request) {
        return Book.builder()
                .title(request.title())
                .synopsis(request.synopsis())
                .archived(false)
                .shareable(request.shareable())
                .bookCover(request.bookCover())
                .isbn(request.isbn())
                .authorName(request.authorName())
                .build();
    }

    public BookResponse toBookResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .synopsis(book.getSynopsis())
                .archived(book.isArchived())
                .shareable(book.isShareable())
                .rate(book.getRate())
                .owner(book.getOwner().fullName())
                // TODO implement this later
                // .cover(book.getBookCover())
                .isbn(book.getIsbn())
                .authorName(book.getAuthorName())
                .build();
    }
}