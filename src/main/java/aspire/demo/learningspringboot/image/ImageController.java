package aspire.demo.learningspringboot.image;

import aspire.demo.learningspringboot.comment.Comment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by andy.lv
 * on: 2018/12/6 13:56
 */
@Controller
public class ImageController {

    private static final String FILE_NAME = "{filename:.+}";

    private ImageService imageService;

    private RestTemplate restTemplate;

    public ImageController(ImageService imageService, RestTemplate restTemplate) {
        this.imageService = imageService;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/")
    public Mono<String> index(Model model) {
        model.addAttribute("images", imageService.findAllImages()
                .flatMap(image -> Mono.just(image)
                        .zipWith(
                                Flux.fromIterable(restTemplate.exchange("http://COMMENT/comments/{imageId}",
                                        HttpMethod.GET,
                                        null,
                                        new ParameterizedTypeReference<List<Comment>>() {
                                        },
                                        image.getId()).getBody()).collectList()
                        )
                ).map(imageAndComment -> new HashMap() {{
                    put("id", imageAndComment.getT1().getId());
                    put("name", imageAndComment.getT1().getName());
                    put("comments", imageAndComment.getT2());
                }})
        );
        model.addAttribute("extra", "DevTools can also detect changes too");
        return Mono.just("index");
    }

    @PostMapping("/images")
    public Mono<String> createImage(@RequestPart(name = "file") Flux<FilePart> files) {
        return imageService.createImage(files)
                .then(Mono.just("redirect:/"))
                ;
    }


    @GetMapping(value = "/images/" + FILE_NAME + "/raw", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<?>> oneRawImage(@PathVariable String filename) {
        return imageService.findOneImage(filename).map(resource -> {
            try {
                return ResponseEntity.ok().contentLength(resource.contentLength())
                        .body(new InputStreamResource(resource.getInputStream()));
            } catch (IOException e) {
                return ResponseEntity.badRequest()
                        .body("Couldn't find " + filename + " => " + e.getMessage());
            }
        });
    }

    @DeleteMapping("/images/" + FILE_NAME)
    public Mono<String> deleteImage(@PathVariable String filename) {
        return imageService.deleteImage(filename)
                .then(Mono.just("redirect:/"))
                ;
    }
}
