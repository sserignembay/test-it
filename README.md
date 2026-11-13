# Test It - Prêt de livres

API REST Spring Boot (Java 25) de prêt de livres pour une bibliothèque.
**Le code est fourni : votre travail est d'écrire les tests, ce sont eux qui vous diront si tout va bien.**

## Le métier en 30 secondes

Un livre passe par 3 statuts : `DISPONIBLE` → `EMPRUNTE` → `RENDU`

| Action | Autorisée si | Mails envoyés |
|---|---|---|
| Emprunter | le livre est `DISPONIBLE` **et** le membre n'a pas déjà un livre `EMPRUNTE` | au membre |
| Rendre | le livre est `EMPRUNTE` **et** c'est ce membre qui l'a emprunté | au membre, et à son bibliothécaire référent s'il en a un |

Sinon, le service lève une exception (`IllegalArgumentException` ou `IllegalStateException`).

## Où est le code ?

```
controller/BookController     endpoints REST
service/BookService           règles métier (c'est le cœur du TD)
repository/BookRepository     accès base (JPA)
adapter/mail                  envoi de mails (fake : il écrit dans les logs)
adapter/member                membre connecté (fake : on choisit qui avec setCurrent)
```

## API

| Méthode | URL | Rôle |
|---|---|---|
| GET | `/books` | tous les livres |
| GET | `/books/{id}` | un livre (404 si absent) |
| GET | `/books/member/{memberId}` | livres d'un membre |
| POST | `/books` | créer `{title, author}`, les deux obligatoires (400 sinon) |
| PUT | `/books/{id}` | modifier (404 si absent) |
| DELETE | `/books/{id}` | supprimer |
| POST | `/books/{id}/borrow` | emprunter (400 si refusé) |
| POST | `/books/{id}/return` | rendre (400 si refusé) |

## Lancer

```
./gradlew test      # lance vos tests
./gradlew bootRun   # lance l'application sur http://localhost:8080
```

Base H2 en mémoire, console sur http://localhost:8080/h2-console

## Le TD

Écrivez les tests dans `src/test/java/com/example/testit/`, un fichier par exercice.
**Minimum attendu : 1 test par exercice.** Si vous avez du temps, ajoutez-en.

| # | Fichier | Type | Test minimum |
|---|---|---|---|
| 1 | `BookServiceTest` | unitaire, tout est mocké (Mockito), sans Spring ni base | Emprunter un livre disponible : vérifiez le statut, l'emprunteur **et les mails envoyés** |
| 2 | `BookRepositoryIT` | JPA, vraie base H2 | `findByMemberAndStatus` avec **deux membres** et plusieurs livres |
| 3 | `BookServiceIT` | contexte Spring complet, vraie base | Emprunter puis rendre un livre, et relire le résultat |
| 4 | `BookControllerIT` | MockMvc | Créer un livre puis l'emprunter, et vérifier les réponses |

Un test qui échoue n'est pas un problème : c'est **ce que vous cherchez**. Notez ce qu'il révèle (le code ou le test ?) puis corrigez.

## Bonus (si vous avez fini)

Dans l'ordre que vous voulez, un test par idée :

- **Cas d'erreur du service** : livre ou membre inconnu, livre déjà emprunté, membre qui a déjà un livre en cours.
- **Retour d'un livre** : par un autre membre que l'emprunteur, et avec ou sans référent (qui doit être prévenu ?).
- **Modifier un livre** (`PUT`) qui est déjà emprunté : que devient son statut ?
- **Supprimer** un livre qui n'existe pas.
- **Personne n'est connecté** et on emprunte un livre : que se passe-t-il ?
- **Validation** : `POST /books` avec un titre ou un auteur vide.
- **TDD** : écrivez le test d'abord, regardez-le échouer, puis corrigez le code.
- **Fake plutôt que mock** : remplacez le mock de `MailService` par un fake qui garde les mails envoyés dans une liste.

## Conseils

- Nommez vos tests comme des phrases : `refuseLEmpruntSiLeLivreEstDejaEmprunte`.
- Un test = un comportement. Structure *Given / When / Then*.
- Un test que vous n'avez jamais vu échouer ne prouve rien : cassez volontairement le service pour vérifier qu'il devient rouge.
- Ne testez pas vos mocks : testez une décision du code (un `if`, un calcul, un appel ou un non-appel).

---

# Bugs trouvés par les tests

## Exercice 1 : `BookServiceTest` (test unitaire, Mockito)

### Bug 1.1 : aucun mail envoyé lors d'un emprunt

- **Test :** `fatouEmprunteUnLivreDisponibleEtRecoitUnMail`
- **Scénario :** Fatou emprunte « Une si longue lettre », qui est disponible.
- **Erreur :** `Wanted but not invoked: mailService.sendMail(...) — Actually, there were zero interactions with this mock.`
- **Ce que ça révèle :** le statut (EMPRUNTE) et l'emprunteur (Fatou) sont corrects,
  mais **aucun mail n'est envoyé au membre**.
- **Le code ou le test ?** Le **code** : le README impose un mail au membre lors d'un emprunt,
  or `borrowBook` n'appelle jamais `mailService.sendMail(...)`.
- **Correction proposée :** dans `BookService.borrowBook`, après `bookRepository.save(book)`, ajouter
  `mailService.sendMail(member, "Livre emprunté", "Vous avez emprunté '" + book.getTitle() + "'.");`
- **Statut :** bug documenté, test laissé en rouge.

## Exercice 2 : `BookRepositoryIT` (test JPA, vraie base H2)

Le test n'a d'abord **pas pu démarrer** : la base ne se créait pas. Il a fallu corriger
le schéma étape par étape (bugs 2.1 à 2.5) avant de révéler le bug de la requête (2.6).

### Bug 2.1 : clés étrangères sans nom

- **Erreur :** `Attribute 'constraintName' must appear on element 'addForeignKeyConstraint'`
- **Où :** `001-create-members-table.xml` et `002-create-book-table.xml`
- **Explication :** Liquibase exige un nom pour chaque clé étrangère ; sans nom, il refuse de lire le fichier.
- **Correction :** ajout de `constraintName="fk_members_referent"` (001) et `constraintName="fk_book_borrower"` (002).

### Bug 2.2 : types incompatibles pour le référent

- **Erreur :** `Values of types "UUID" and "BIGINT" are not comparable`
- **Où :** `001-create-members-table.xml`
- **Explication :** `referent_id` est en `uuid` alors qu'il pointe vers `members.id` qui est un `bigint`.
  Une clé étrangère exige le même type des deux côtés.
- **Correction :** `referent_id` passe en `type="bigint"`.

### Bug 2.3 : nom de table différent

- **Erreur :** `Schema validation: missing table [member]`
- **Où :** `Member.java`
- **Explication :** Liquibase crée la table `members`, mais l'entité `Member` cherche par défaut
  une table `member`. Hibernate (mode `validate`) refuse de démarrer.
- **Correction :** ajout de `@Table(name = "members")` sur la classe `Member`.

### Bug 2.4 : nom de colonne différent

- **Erreur :** `Schema validation: missing column [username] in table [members]`
- **Où :** `001-create-members-table.xml`
- **Explication :** la colonne s'appelle `user` alors que l'entité a un champ `username`.
  De plus, `USER` est un mot réservé en SQL.
- **Correction :** la colonne est renommée `username`.

### Bug 2.5 : génération d'id incohérente

- **Erreur :** `Schema validation: missing sequence [book_seq]`
- **Où :** `Book.java`
- **Explication :** l'entité utilise `GenerationType.SEQUENCE` (compteur séparé `book_seq`),
  alors que la table `book` est en auto-incrément. La séquence n'existe pas.
- **Correction :** `GenerationType.SEQUENCE` → `GenerationType.IDENTITY` (comme `Member`).

### Bug 2.6 : la requête `findByMemberAndStatus` utilise OR au lieu de AND

- **Test :** `findByMemberAndStatusNeRenvoieQueLesLivresEmpruntesParFatou`
- **Scénario :** 2 membres (Fatou, Moussa) et 4 livres ; on cherche les livres EMPRUNTÉS par Fatou.
- **Erreur :** attendu `["L'Aventure ambiguë"]`, obtenu
  `["L'Aventure ambiguë", "Une si longue lettre", "Les Bouts de bois de Dieu"]`.
- **Explication :** la requête renvoie les livres du membre **ou** ayant ce statut,
  au lieu des livres du membre **et** ayant ce statut. Le second membre (Moussa)
  était indispensable pour révéler le problème.
- **Conséquence dans l'appli :** le service s'appuie sur cette requête pour vérifier
  qu'un membre n'a pas déjà un livre emprunté : la règle métier est faussée.
- **Correction :** `OR` → `AND` dans `BookRepository`.
- **Statut :** corrigé, test vert .

## Exercice 3 : `BookServiceIT` (contexte Spring complet, vraie base)

### Bug 3.1 : `LazyInitializationException` au retour d'un livre

- **Test :** `fatouEmpruntepuisRendLeLivreQuiFinitRendu`
- **Scénario :** Fatou emprunte puis rend « Le Ventre de l'Atlantique ».
- **Erreur :** `LazyInitializationException: Could not initialize proxy [Member#1] - no session`
- **Explication :** l'emprunteur d'un livre est chargé en `LAZY` (proxy vide).
  `BookService` n'est pas transactionnel : la session Hibernate est fermée après
  `findById`. Quand `returnBook` envoie le mail et lit `member.getUsername()`,
  le proxy ne peut plus se charger.
- **Pourquoi l'exercice 1 ne l'a pas vu :** avec des mocks, il n'y a ni base ni proxy Hibernate.
- **Correction :** ajout de `@Transactional` sur la classe `BookService`.