package aspire.demo.learningspringboot.comment;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Comment {
    @Id private String id;

    private String imageId;

    private String comment;
}
