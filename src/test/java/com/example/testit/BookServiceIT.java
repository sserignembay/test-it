package com.example.testit;

import com.example.testit.model.Book;
import com.example.testit.model.Member;
import com.example.testit.model.Status;
import com.example.testit.repository.BookRepository;
import com.example.testit.repository.MemberRepository;
import com.example.testit.service.BookService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookServiceIT {

    @Autowired
    BookService bookService;
    @Autowired
    BookRepository bookRepository;
    @Autowired
    MemberRepository memberRepository;

    @AfterEach
    void nettoyer() {
        bookRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void fatouEmpruntepuisRendLeLivreQuiFinitRendu() {
        // GIVEN : Fatou et un livre
        Member fatou = memberRepository.save(new Member("fatou"));
        Book ventre = bookService.createBook("Le Ventre de l'Atlantique", "Fatou Diome");

        // WHEN 1 : Fatou emprunte le livre
        bookService.borrowBook(ventre.getId(), fatou.getId());

        // THEN 1 : relu en base, il est EMPRUNTÉ
        Book apresEmprunt = bookRepository.findById(ventre.getId()).orElseThrow();
        assertThat(apresEmprunt.getStatus()).isEqualTo(Status.EMPRUNTE);

        // WHEN 2 : Fatou rend le livre
        bookService.returnBook(ventre.getId(), fatou.getId());

        // THEN 2 : relu en base, il est RENDU et Fatou reste la dernière emprunteuse
        Book apresRetour = bookRepository.findById(ventre.getId()).orElseThrow();
        assertThat(apresRetour.getStatus()).isEqualTo(Status.RENDU);
        assertThat(apresRetour.getBorrower().getId()).isEqualTo(fatou.getId());
    }
}