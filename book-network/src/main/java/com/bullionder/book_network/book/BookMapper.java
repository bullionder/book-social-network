package com.bullionder.book_network.book;

import com.bullionder.book_network.book.file.FileUtils;
import com.bullionder.book_network.history.BookTransactionHistory;
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
                .cover(FileUtils.readFileFromLocation(book.getBookCover()))
                .isbn(book.getIsbn())
                .authorName(book.getAuthorName())
                .build();
    }

    public BorrowedBookResponse toBorrowedBookResponse(BookTransactionHistory history) {
        return BorrowedBookResponse.builder()
                .id(history.getBook().getId())
                .title(history.getBook().getTitle())
                .rate(history.getBook().getRate())
                .isbn(history.getBook().getIsbn())
                .returned(history.isReturned())
                .returnApproved(history.isReturnApproved())
                .authorName(history.getBook().getAuthorName())
                .build();
    }
}