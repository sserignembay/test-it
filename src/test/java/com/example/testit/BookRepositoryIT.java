package com.example.testit;

import com.example.testit.model.Book;
import com.example.testit.model.Member;
import com.example.testit.model.Status;
import com.example.testit.repository.BookRepository;
import com.example.testit.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryIT {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void findByMemberAndStatusNeRenvoieQueLesLivresEmpruntesParFatou() {
        // GIVEN : deux membres...
        Member fatou = memberRepository.save(new Member("fatou"));
        Member moussa = memberRepository.save(new Member("moussa"));

        // ... et plusieurs livres
        Book aventure = new Book("L'Aventure ambiguë", "Cheikh Hamidou Kane");
        aventure.setBorrower(fatou);
        aventure.setStatus(Status.EMPRUNTE);
        bookRepository.save(aventure);          // Fatou, EMPRUNTÉ  ← le seul attendu

        Book lettre = new Book("Une si longue lettre", "Mariama Bâ");
        lettre.setBorrower(fatou);
        lettre.setStatus(Status.RENDU);
        bookRepository.save(lettre);            // Fatou, mais RENDU

        Book boutsDeBois = new Book("Les Bouts de bois de Dieu", "Ousmane Sembène");
        boutsDeBois.setBorrower(moussa);
        boutsDeBois.setStatus(Status.EMPRUNTE);
        bookRepository.save(boutsDeBois);       // EMPRUNTÉ, mais par Moussa

        bookRepository.save(new Book("Le Ventre de l'Atlantique", "Fatou Diome")); // DISPONIBLE

        // WHEN : on cherche les livres EMPRUNTÉS par Fatou
        List<Book> resultat = bookRepository.findByMemberAndStatus(fatou.getId(), Status.EMPRUNTE);

        // THEN : un seul livre, L'Aventure ambiguë
        assertThat(resultat)
                .extracting(Book::getTitle)
                .containsExactly("L'Aventure ambiguë");
    }
}