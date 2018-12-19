package aspire.demo.learningspringboot;

import aspire.demo.learningspringboot.image.ImageController;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.support.BindingAwareModelMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Created by andy.lv
 * on: 2018/12/19 13:43
 */
@Component
@Profile("simulator")
public class ImageSimulator {

    private final ImageController imageController;

    public ImageSimulator(ImageController imageController) {
        this.imageController = imageController;
    }

    @EventListener
    public void simulateUserClicking(ApplicationReadyEvent event) {
        Flux.interval(Duration.ofMillis(500))
                .flatMap(tick -> Mono.defer(() -> imageController.index(new BindingAwareModelMap())))
                .subscribe();

    }
}
