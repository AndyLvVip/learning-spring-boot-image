package aspire.demo.learningspringboot.image;

import aspire.demo.learningspringboot.comment.Comment;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by andy.lv
 * on: 2018/12/19 12:51
 */
@Service
public class CommentService {

    private final RestTemplate restTemplate;

    public CommentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @HystrixCommand(fallbackMethod = "defaultComments")
    public List<Comment> getComments(String imageId) {
        return restTemplate.exchange("http://COMMENT/comments/{imageId}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Comment>>() {
                },
                imageId
        ).getBody();
    }

    public List<Comment> defaultComments(String imageId) {
        return Collections.emptyList();
    }
}
