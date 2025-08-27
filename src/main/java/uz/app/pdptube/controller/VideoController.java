package uz.app.pdptube.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.app.pdptube.dto.VideoDTO;
import uz.app.pdptube.payload.ResponseMessage;
import uz.app.pdptube.service.VideoService;

@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;



    @GetMapping("/{channelId}/videos")
    public ResponseEntity<ResponseMessage> getChannelVideos(@PathVariable Integer channelId){
        ResponseMessage serviceResponse = videoService.getChannelVideos(channelId);
        return ResponseEntity.status(serviceResponse.success() ? HttpStatus.OK : HttpStatus.NOT_FOUND).body(serviceResponse);
    }


    @PostMapping
    public ResponseEntity<?> postVideo(@RequestBody VideoDTO videoDTO) {
        ResponseMessage serviceResponse = videoService.postVideo(videoDTO);
        return ResponseEntity.status(serviceResponse.success() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(serviceResponse);
    }

    @PostMapping("/like/{videoId}")
    public ResponseEntity<?> likeVideo(@PathVariable Integer videoId) {
        ResponseMessage serviceResponse = videoService.likeVideo(videoId);
        return ResponseEntity.status(serviceResponse.success() ? HttpStatus.ACCEPTED : HttpStatus.NOT_FOUND).body(serviceResponse);
    }

    @PostMapping("/dislike/{videoId}")
    public ResponseEntity<?> dislikeVideo(@PathVariable Integer videoId) {
        ResponseMessage serviceResponse = videoService.dislikeVideo(videoId);
        return ResponseEntity.status(serviceResponse.success() ? HttpStatus.ACCEPTED : HttpStatus.NOT_FOUND).body(serviceResponse);
    }

    @DeleteMapping("/delete/{videoId}")
    public ResponseEntity<?> deleteVideo(@PathVariable Integer videoId) {
        ResponseMessage serviceResponse = videoService.deleteVideo(videoId);
        return ResponseEntity.status(serviceResponse.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(serviceResponse);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchVideos(@RequestParam String title) {
        ResponseMessage responseMessage = videoService.searchVideos(title);
        return ResponseEntity.status(responseMessage.success() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(responseMessage);
    }
}
