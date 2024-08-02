package com.bullionder.book_network.book;

import lombok.*;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BookResponse {

    private String title;
    private String owner;
    private Integer id;
    private String authorName;
    private String isbn;
    private String synopsis;
    private byte[] cover;
    private boolean archived;
    private double rate;
    private boolean shareable;
}
