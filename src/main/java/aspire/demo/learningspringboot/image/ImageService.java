package aspire.demo.learningspringboot.image;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Created by andy.lv
 * on: 2018/11/30 17:01
 */
@Service
public class ImageService {

    public static String UPLOAD_ROOT = "upload-dir";

    private final ImageRepository imageRepository;

    private final ResourceLoader resourceLoader;

    private final MeterRegistry meterRegistry;

    public ImageService(ImageRepository imageRepository, ResourceLoader resourceLoader, MeterRegistry meterRegistry) {
        this.imageRepository = imageRepository;
        this.resourceLoader = resourceLoader;
        this.meterRegistry = meterRegistry;
    }

    public Flux<Image> findAllImages() {
        return imageRepository.findAll()
                .log("findAll");
    }

    @Bean
    public CommandLineRunner initFiles() {
        return args -> {
            FileSystemUtils.deleteRecursively(Paths.get(UPLOAD_ROOT));
            Files.createDirectory(Paths.get(UPLOAD_ROOT));
            FileCopyUtils.copy("learning-spring-boot-cover.jpg", new FileWriter(UPLOAD_ROOT + "/learning-spring-boot-cover.jpg"));
            FileCopyUtils.copy("learning-spring-boot-2nd-edition-cover.jpg", new FileWriter(UPLOAD_ROOT + "/learning-spring-boot-2nd-edition-cover.jpg"));
            FileCopyUtils.copy("bazinga.png", new FileWriter(UPLOAD_ROOT + "/bazinga.png"));
        };
    }

    public Mono<Void> createImage(Flux<FilePart> files) {
        return files
                .log("createImage-files")
                .flatMap(file -> {
            Mono<Image> saveDatabaseImage = imageRepository.save(new Image(UUID.randomUUID().toString(), file.filename()))
                    .log("createImage-save")
                    ;

            Mono<Void> copyFile = Mono.just(
                    Paths.get(UPLOAD_ROOT, file.filename()).toFile()
            ).log("createImage-lockTarget")
                    .map(destFile -> {
                        try {
                            destFile.createNewFile();
                            return destFile;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .log("createImage-newfile")
                    .flatMap(destFile -> file.transferTo(destFile))
                    .log("createImage-copy");

                    Mono<Void> countFile = Mono.fromRunnable(() -> {
                        meterRegistry
                                .summary("files.uploaded.bytes")
                                .record(Paths.get(UPLOAD_ROOT, file.filename())
                                        .toFile().length());

                    });
            return Mono.when(saveDatabaseImage, copyFile, countFile)
                    .log("createImage-when")
                    ;
        }).then().log("createImage-done");
    }

    public Mono<Void> deleteImage(String filename) {
        Mono<Void> deleteImageAction = imageRepository
                .findByName(filename)
                .log("deleteImage-find")
                .flatMap(imageRepository::delete)
                .log("deleteImage-record")
                ;
        Mono<Object> deleteFileAction = Mono.fromRunnable(() -> {
            try {
                Files.deleteIfExists(Paths.get(UPLOAD_ROOT, filename));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        })
                .log("deleteImage-file")
                ;
        return Mono.when(deleteImageAction, deleteFileAction)
                .log("deleteImage-when")
                .then()
                .log("deleteImage-done")
                ;
    }
    public Mono<Resource> findOneImage(String filename) {
        return Mono.fromSupplier(() -> resourceLoader.getResource("file:" + UPLOAD_ROOT + "/" + filename));
    }
}
