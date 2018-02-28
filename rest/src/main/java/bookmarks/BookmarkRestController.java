package bookmarks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/{userId}/bookmarks")
class BookmarkRestController {

    private final AccountRepository accountRepository;

    private final BookmarkRepository bookmarkRepository;

    @Autowired
    BookmarkRestController(AccountRepository accountRepository, BookmarkRepository bookmarkRepository) {
        this.accountRepository = accountRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    @RequestMapping(method = RequestMethod.GET)
    Resources<BookmarkResource> readBookmarks(Principal principal) {
        this.validateUser(principal);

        List<BookmarkResource> bookmarkResourceList = bookmarkRepository
                .findByAccountUsername(principal.getName()).stream()
                .map(BookmarkResource::new)
                .collect(Collectors.toList());

        return new Resources<>(bookmarkResourceList);
    }

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<?> add(Principal principal, @RequestBody Bookmark input) {
        this.validateUser(principal);

        return accountRepository
                .findByUsername(principal.getName())
                .map(account -> {
                    Bookmark bookmark = bookmarkRepository.save(
                            new Bookmark(account, input.getUri(), input.getDescription()));

                    Link forOneBookmark = new BookmarkResource(bookmark).getLink(Link.REL_SELF);

                    return ResponseEntity.created(URI
                            .create(forOneBookmark.getHref()))
                            .build();
                })
                .orElse(ResponseEntity.noContent().build());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{bookmarkId}")
    BookmarkResource readBookmark(Principal principal, @PathVariable Long bookmarkId) {
        this.validateUser(principal);
        return new BookmarkResource(
                this.bookmarkRepository.findOne(bookmarkId));
    }



    private void validateUser(Principal principal) {
        String userId = principal.getName();
        this.accountRepository
                .findByUsername(userId)
                .orElseThrow(
                        () -> new UserNotFoundException(userId));
    }

}
