package com.example.testit;

import com.example.testit.adapter.member.CurrentMemberServiceFake;
import com.example.testit.model.Member;
import com.example.testit.repository.BookRepository;
import com.example.testit.repository.MemberRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    BookRepository bookRepository;
    @Autowired
    CurrentMemberServiceFake currentMember;

    @AfterEach
    void nettoyer() {
        currentMember.setCurrent(null);
        bookRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void fatouCreeUnLivrePuisLEmprunteViaLApi() throws Exception {
        // GIVEN : Fatou est connectée
        Member fatou = memberRepository.save(new Member("fatou"));
        currentMember.setCurrent(fatou.getId());

        // WHEN 1 : création du livre via l'API
        String reponse = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Une si longue lettre\",\"author\":\"Mariama Bâ\"}"))
                // THEN 1 : 200 + le JSON contient les bonnes valeurs
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Une si longue lettre"))
                .andExpect(jsonPath("$.author").value("Mariama Bâ"))
                .andExpect(jsonPath("$.status").value("DISPONIBLE"))
                .andReturn().getResponse().getContentAsString();

        // on récupère l'id du livre créé dans le JSON
        long id = ((Number) JsonPath.read(reponse, "$.id")).longValue();

        // WHEN 2 : Fatou emprunte le livre
        mockMvc.perform(post("/books/{id}/borrow", id))
                // THEN 2 : 200 + statut EMPRUNTE
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMPRUNTE"));
    }
}