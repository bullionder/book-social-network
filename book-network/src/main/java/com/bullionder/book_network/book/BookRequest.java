package com.bullionder.book_network.book;

public record BookRequest(
        String title,
        String authorName,
        String isbn,
        String synopsis,
        String bookCover,
        boolean archived,
        boolean shareable
) {
}
