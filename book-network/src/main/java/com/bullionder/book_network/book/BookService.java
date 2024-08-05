package com.bullionder.book_network.book;

import com.bullionder.book_network.book.file.FileStorageService;
import com.bullionder.book_network.common.PageResponse;
import com.bullionder.book_network.exception.OperationNotPermittedException;
import com.bullionder.book_network.history.BookTransactionHistory;
import com.bullionder.book_network.history.BookTransactionHistoryRepository;
import com.bullionder.book_network.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.bullionder.book_network.book.BookSpecification.withOwnerId;

@Service
@RequiredArgsConstructor
public class BookService {

    public static final String CREATED_DATE = "createdDate";
    public static final String NO_BOOK_FOUND_WITH_THE_ID = "No book found with the ID:: ";
    public static final String THE_REQUESTED_BOOK_CANNOT_BE_BORROWED_SINCE_IT_IS_ARCHIVED_OR_NOT_SHAREABLE = "The requested book cannot be borrowed since it is archived or not shareable";
    private final BookRepository repository;
    private final BookMapper bookMapper;
    private final BookTransactionHistoryRepository transactionHistoryRepository;
    private final BookTransactionHistoryRepository bookTransactionHistoryRepository;
    private final FileStorageService fileStorageService;

    public Integer save(BookRequest request, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Book book = bookMapper.toBook(request);
        book.setOwner(user);
        return repository.save(book).getId();
    }

    public BookResponse findById(Integer id) {
        return repository.findById(id)
                .map(bookMapper::toBookResponse)
                .orElseThrow(() -> new EntityNotFoundException(NO_BOOK_FOUND_WITH_THE_ID + id));
    }

    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_DATE).descending());
        Page<Book> books = repository.findAllDisplayableBooks(pageable, user.getId());
        List<BookResponse> bookResponses = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponses,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_DATE).descending());
        Page<Book> books = repository.findAll(withOwnerId(user.getId()), pageable);
        List<BookResponse> bookResponses = books.stream()
                .map(bookMapper::toBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponses,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedBooks(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_DATE).descending());
        Page<BookTransactionHistory> allBorrowedBooks = transactionHistoryRepository.findBorrowedBooks(pageable, user.getId());
        List<BorrowedBookResponse> bookResponses = allBorrowedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponses,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {
        User user = (User) connectedUser.getPrincipal();
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_DATE).descending());
        Page<BookTransactionHistory> allReturnedBooks = transactionHistoryRepository.findReturnedBooks(pageable, user.getId());
        List<BorrowedBookResponse> bookResponses = allReturnedBooks.stream()
                .map(bookMapper::toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponses,
                allReturnedBooks.getNumber(),
                allReturnedBooks.getSize(),
                allReturnedBooks.getTotalElements(),
                allReturnedBooks.getTotalPages(),
                allReturnedBooks.isFirst(),
                allReturnedBooks.isLast()
        );
    }

    public Integer updateShareableStatus(Authentication connectedUser, Integer bookId) {
        Book book = repository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(NO_BOOK_FOUND_WITH_THE_ID + bookId));
        User user = (User) connectedUser.getPrincipal();
        if (!book.getOwner().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You cannot update others books shareable status");
        }
        book.setShareable(!book.isShareable());
        repository.save(book);
        return bookId;
    }

    public Integer updateArchivedStatus(Authentication connectedUser, Integer bookId) {
        Book book = repository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(NO_BOOK_FOUND_WITH_THE_ID + bookId));
        User user = (User) connectedUser.getPrincipal();
        if (!book.getOwner().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You cannot update others books archived status");
        }
        book.setArchived(!book.isArchived());
        repository.save(book);
        return bookId;
    }

    public Integer borrowBook(Authentication connectedUser, Integer bookId) {
        Book book = repository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(NO_BOOK_FOUND_WITH_THE_ID + bookId));
        if (!book.isShareable() || book.isArchived()) {
            throw new OperationNotPermittedException(THE_REQUESTED_BOOK_CANNOT_BE_BORROWED_SINCE_IT_IS_ARCHIVED_OR_NOT_SHAREABLE);
        }

        User user = (User) connectedUser.getPrincipal();
        if (!book.getOwner().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow your own book");
        }

        final boolean isAlreadyBorrowedByOtherUser = transactionHistoryRepository.isAlreadyBorrowed(bookId);
        if (isAlreadyBorrowedByOtherUser) {
            throw new OperationNotPermittedException("The requested book is already borrowed");
        }

        final boolean isAlreadyBorrowed = transactionHistoryRepository.isAlreadyBorrowedByUser(bookId, user.getId());
        if (isAlreadyBorrowed) {
            throw new OperationNotPermittedException("You have already borrowed this book and not returned it yet");
        }

        BookTransactionHistory bookTransactionHistory = BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();
        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer returnBorrowedBook(Authentication connectedUser, Integer bookId) {
        Book book = repository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(NO_BOOK_FOUND_WITH_THE_ID + bookId));
        if (!book.isShareable() || book.isArchived()) {
            throw new OperationNotPermittedException(THE_REQUESTED_BOOK_CANNOT_BE_BORROWED_SINCE_IT_IS_ARCHIVED_OR_NOT_SHAREABLE);
        }

        User user = (User) connectedUser.getPrincipal();
        if (!book.getOwner().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow and return your own book");
        }

        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("You did not borrow this book"));
        bookTransactionHistory.setReturned(true);

        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public Integer approveReturnBorrowedBook(Authentication connectedUser, Integer bookId) {
        Book book = repository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(NO_BOOK_FOUND_WITH_THE_ID + bookId));
        if (!book.isShareable() || book.isArchived()) {
            throw new OperationNotPermittedException(THE_REQUESTED_BOOK_CANNOT_BE_BORROWED_SINCE_IT_IS_ARCHIVED_OR_NOT_SHAREABLE);
        }

        User user = (User) connectedUser.getPrincipal();
        if (!book.getOwner().getId().equals(user.getId())) {
            throw new OperationNotPermittedException("You cannot borrow and return your own book");
        }

        BookTransactionHistory bookTransactionHistory = transactionHistoryRepository.findByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(() -> new OperationNotPermittedException("The book is not returned yet. You cannot approve its return"));
        bookTransactionHistory.setReturnApproved(true);

        return bookTransactionHistoryRepository.save(bookTransactionHistory).getId();
    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
        Book book = repository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException(NO_BOOK_FOUND_WITH_THE_ID + bookId));
        User user = (User) connectedUser.getPrincipal();
        var bookCover = fileStorageService.saveFile(file, user.getId());
        book.setBookCover(bookCover);
        repository.save(book);
    }
}
