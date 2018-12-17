package aspire.demo.learningspringboot.image;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by andy.lv
 * on: 2018/11/30 16:22
 */
@Document
@Data
public class Image {

    @Id private final String id;
        private final String name;

}
