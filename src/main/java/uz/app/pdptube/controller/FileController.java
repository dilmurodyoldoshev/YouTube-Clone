package uz.app.pdptube.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.app.pdptube.entity.Channel;
import uz.app.pdptube.entity.ChannelOwner;
import uz.app.pdptube.entity.Notification;
import uz.app.pdptube.entity.Video;
import uz.app.pdptube.enums.NotificationType;
import uz.app.pdptube.helper.Helper;
import uz.app.pdptube.repository.ChannelOwnerRepository;
import uz.app.pdptube.repository.ChannelRepository;
import uz.app.pdptube.repository.NotificationRepository;
import uz.app.pdptube.repository.VideoRepository;
import uz.app.pdptube.service.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for video storage operations via multipart file upload.
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private final VideoStorageService videoStorageService;
    private final VideoRepository videoRepository;
    private final ChannelOwnerRepository channelOwnerRepository;
    private final ChannelService channelService;
    private final NotificationRepository notificationRepository;
    private final ChannelRepository channelRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public FileController(VideoStorageService videoStorageService, VideoRepository videoRepository, ChannelOwnerRepository channelOwnerRepository, ChannelService channelService, NotificationRepository notificationRepository, ChannelRepository channelRepository, FileStorageService fileStorageService) {
        this.videoStorageService = videoStorageService;
        this.videoRepository = videoRepository;
        this.channelOwnerRepository = channelOwnerRepository;
        this.channelService = channelService;
        this.notificationRepository = notificationRepository;
        this.channelRepository = channelRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Uploads a video file to Amazon S3.
     *
     * @param file Video file to upload
     * @return Response with the URL of the uploaded video
     * @throws IOException if file upload fails
     */
    @PostMapping(value = "/upload/{videoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a video file to S3")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file, @PathVariable Integer videoId) throws IOException {
        // Ensure the file is a valid video format (optional, based on your requirement)
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No video file provided");
        }
        if (videoId == null) {
            return ResponseEntity.badRequest().body("No video id provided");
        }

        Optional<Video> optionalVideo = videoRepository.findById(videoId);
        if (optionalVideo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No video found with id " + videoId);
        }

        Video video = optionalVideo.get();
        System.out.println(video);
        Optional<ChannelOwner> optionalChannel = channelOwnerRepository.findByChannel(video.getChannel().getId());
        ChannelOwner channelOwner = optionalChannel.get();
        if (Helper.getCurrentPrincipal().getId().equals(channelOwner.getOwner())) {
            if (video.getVideoLink().equalsIgnoreCase("String") || video.getVideoLink().isEmpty() || video.getVideoLink().isBlank()) {

                String videoUrl = videoStorageService.storeVideo(file);
                video.setVideoLink(videoUrl);
                videoRepository.save(video);
                Channel channel = video.getChannel();
                List<Integer> allFollowers = channelService.getAllFollowers(channel.getId());
                Notification notification = Notification.builder()
                        .type(NotificationType.NEW_VIDEO_POSTED)
                        .subjectId(video.getId())
                        .build();

                for (Integer follower : allFollowers) {
                    notification.setUserId(follower);
                    notificationRepository.save(notification);
                    notification.setId(null);
                }

                return ResponseEntity.status(HttpStatus.CREATED).body(video.getVideoLink());
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("this video already has a file:  " + video.getVideoLink());
            }
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("This video isn't yours , so you can't upload the file!");
        }

    }

    @PostMapping(value = "/upload/profile-image/{channelId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a profile image for a channel to S3")
    public ResponseEntity<String> uploadProfileImage(@RequestParam("file") MultipartFile file, @PathVariable Integer channelId) throws IOException {
        // Ensure the file is not empty
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No profile image file provided");
        }

        // Fetch the channel by ID
        Optional<Channel> optionalChannel = channelRepository.findById(channelId);
        if (optionalChannel.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Channel not found with id " + channelId);
        }
        Channel channel = optionalChannel.get();

        // Use the fileStorageService to upload the image to S3
        String imageUrl = fileStorageService.uploadFile(file);

        // Save the URL of the uploaded image to the channel entity
        channel.setProfilePicture(imageUrl);  // Assuming there's a method like this
        channelRepository.save(channel);  // Save the channel with the updated profile image URL

        return ResponseEntity.status(HttpStatus.CREATED).body(imageUrl);
    }
}
