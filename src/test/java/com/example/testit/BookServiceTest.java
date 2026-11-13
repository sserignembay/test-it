package com.example.testit;

import com.example.testit.adapter.mail.MailService;
import com.example.testit.model.Book;
import com.example.testit.model.Member;
import com.example.testit.model.Status;
import com.example.testit.repository.BookRepository;
import com.example.testit.repository.MemberRepository;
import com.example.testit.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    BookRepository bookRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    MailService mailService;

    @InjectMocks
    BookService bookService;

    @Test
    void emprunterUnLivreDisponible() {
        // GIVEN : un livre disponible et un membre
        Book dune = new Book("Dune", "Frank Herbert");
        dune.setId(1L);
        Member alice = new Member("alice");
        alice.setId(10L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(dune));
        when(memberRepository.findById(10L)).thenReturn(Optional.of(alice));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN : Alice emprunte Dune
        Book resultat = bookService.borrowBook(1L, 10L);

        // THEN : statut, emprunteur et mail
        assertThat(resultat.getStatus()).isEqualTo(Status.EMPRUNTE);
        assertThat(resultat.getBorrower()).isSameAs(alice);
        verify(mailService).sendMail(eq(alice), anyString(), anyString());
    }
}