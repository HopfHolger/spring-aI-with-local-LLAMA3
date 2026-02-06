package it.gdorsi.controller;

import it.gdorsi.dao.Author;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {

    private final ChatClient chatClient;

    public BookController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/search-by-author")
    public Author getBooksByAuthor(@RequestParam(value = "author", defaultValue = "Dan Brown") String author) {

        return chatClient.prompt()
                .user(author)
                .call()
                .entity(Author.class);
    }

}
