package uz.app.pdptube.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.app.pdptube.dto.VideoDTO;
import uz.app.pdptube.entity.*;
import uz.app.pdptube.helper.Helper;
import uz.app.pdptube.payload.ResponseMessage;
import uz.app.pdptube.repository.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uz.app.pdptube.helper.Helper.ageRestricted;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final ChannelService channelService;
    private final ChannelOwnerRepository channelOwnerRepository;
    private final UserLikedVideosRepository userLikedVideosRepository;
    private final UserDislikedVideosRepository userDislikedVideosRepository;
    private final PlaylistVideosRepository playlistVideosRepository;
    private final HistoryService historyService;
    private final HistoryRepository historyRepository;
    private final HistoryVideosRepository historyVideosRepository;
    private final UserLikedCommentsRepository userLikedCommentsRepository;
    private final UserDislikedCommentsRepository userDislikedCommentsRepository;
    private final CommentRepository commentRepository;
    private final ChannelRepository channelRepository;

    public ResponseMessage getChannelVideos(Integer channelId) {
        //Channelni tekshirish
        Optional<Channel> channel = channelRepository.findById(channelId);
        if (channel.isEmpty()) {
            return new ResponseMessage(false, "Channel does not exist", null);
        }

        //Channel ga tegishli videolarini olish
        List<Video> videos = videoRepository.findAllByChannelId(channelId);

        if (videos.isEmpty()) {
            return new ResponseMessage(true, "No videos found for this channel", List.of());
        }

        //Faqat yuklangan videolarni olish
        List<Video> uploadedVideos = videos.stream()
                .filter(video -> Helper.fileUploaded(video.getVideoLink()))
                .filter(video -> !ageRestricted(video))
                .collect(Collectors.toList());


        if (uploadedVideos.isEmpty()) {
            return new ResponseMessage(false, "No videos uploaded for this channel", null);
        }

        //Yuklangan videolarni qaytaradi agar if dan otsa
        return new ResponseMessage(true, "Here are the uploaded videos of this channel", uploadedVideos);
    }
















    public ResponseMessage getVideo(Integer id) {
        Optional<Video> optionalVideo = videoRepository.findById(id);

        if (optionalVideo.isPresent()) {
            Video video = optionalVideo.get();
            if (ageRestricted(video)) {
                return new ResponseMessage(false, "you are not old enough or haven't signed in to watch this video!", Helper.getCurrentPrincipal().getAge());
            }
            if (!fileUploaded(video.getVideoLink())){
                return new ResponseMessage(false, "video doesn't have its file yet, first upload the file!", video);
            }
            video.setViews(video.getViews() + 1);
            videoRepository.save(video);
            ResponseMessage serviceResponse = historyService.getHistory();
            //this part might need some checkings
            if (!serviceResponse.success()) {
                if (serviceResponse.message().contains("xato!")) {
                    return serviceResponse;
                }else {
                    History history = new History();
                    history.setOwner(Helper.getCurrentPrincipal());
                    historyRepository.save(history);
                    HistoryVideos historyVideos = new HistoryVideos();
                    History historyWithId = historyRepository.findByOwner(Helper.getCurrentPrincipal()).get();
                    historyVideos.setHistory(historyWithId.getId());
                    historyVideos.setVideo(video.getId());
                    historyVideosRepository.save(historyVideos);
                }
            }else {
                HistoryVideos historyVideos = new HistoryVideos();
                History history = historyRepository.findByOwner(Helper.getCurrentPrincipal()).get();
                historyVideos.setHistory(history.getId());
                historyVideos.setVideo(video.getId());
                historyVideosRepository.save(historyVideos);
            }
            return new ResponseMessage(true, "here is the video", video);
        } else {
            return new ResponseMessage(false, "video with this id doesn't exist", id);
        }
    }
    public ResponseMessage getVideos() {
        List<Video> videos = videoRepository.findAll();
        List<Video> filteredVideos = videos.stream()
                .filter(video -> {
                    return fileUploaded(video.getVideoLink());
                })
                .toList();
        return new ResponseMessage(true, "Here are all the videos", filteredVideos);
    }
    public ResponseMessage postVideo(VideoDTO videoDTO) {
        ResponseMessage responseMessage = channelService.getChannel();
        boolean success = responseMessage.success();
        if (success) {
          int input = videoDTO.getAgeRestriction();
           int age = Helper.getCurrentPrincipal().getAge();
           boolean modified = false;
            if (input > age) {
                videoDTO.setAgeRestriction(age);
                modified = true;
            }
            Channel channel = (Channel) responseMessage.data();
            Video video = Video.builder()
                    .videoLink("string")
                    .channel(channel)
                    .description(videoDTO.getDescription())
                    .likes(0)
                    .dislikes(0)
                    .title(videoDTO.getTitle())
                    .ageRestriction(videoDTO.getAgeRestriction())
                    .category(videoDTO.getCategory())
                    .views(0)
                    .build();
            videoRepository.save(video);
            String message = "Video posted, now please upload the file";
            if (modified) return new ResponseMessage(true, message + ", and age restriction is set to the highest allowed to you", video);
            return new ResponseMessage(true,message ,video);
        } else {
            return new ResponseMessage(false, "you don't have a channel , so you can't post video", videoDTO);
        }
    }
    private boolean fileUploaded(String videoLink){
        return !(videoLink.equalsIgnoreCase("String") || videoLink.isBlank());
    }
    @Transactional
    public ResponseMessage likeVideo(Integer videoId) {
        Optional<Video> optionalVideo = videoRepository.findById(videoId);
        if (optionalVideo.isPresent()) {
            Video video = optionalVideo.get();
            if (ageRestricted(video)) {
                return new ResponseMessage(false, "you are not old enough or haven't signed in!", Helper.getCurrentPrincipal().getAge());
            }
            boolean previouslyLiked = userLikedVideosRepository.existsByOwnerAndVideo(Helper.getCurrentPrincipal().getId(), videoId);
            if (previouslyLiked) {
                return new ResponseMessage(false, "you are already liked the video before!", video);
            }else {
                if (!fileUploaded(video.getVideoLink())){
                    return new ResponseMessage(false, "video doesn't have its file yet, first owner must upload the file!", video);
                }
                if (userDislikedVideosRepository.existsByOwnerAndVideo(Helper.getCurrentPrincipal().getId(), videoId)){
                    userDislikedVideosRepository.deleteByOwnerAndVideo(Helper.getCurrentPrincipal().getId(), videoId);
                    video.setLikes(video.getLikes() + 1);
                    video.setDislikes(video.getDislikes() - 1);
                }else {
                    video.setLikes(video.getLikes() + 1);
                }
                videoRepository.save(video);
                UserLikedVideos userLikedVideos = new UserLikedVideos();
                userLikedVideos.setVideo(video.getId());
                userLikedVideos.setOwner(Helper.getCurrentPrincipal().getId());
                userLikedVideosRepository.save(userLikedVideos);
                return new ResponseMessage(true, "Video liked", video);
            }
        } else {
            return new ResponseMessage(false, "video with this id doesn't exist", videoId);
        }
    }


    @Transactional
    public ResponseMessage dislikeVideo(Integer videoId) {
        Optional<Video> optionalVideo = videoRepository.findById(videoId);
        if (optionalVideo.isPresent()) {
            Video video = optionalVideo.get();
            if (ageRestricted(video)) {
                return new ResponseMessage(false, "you are not old enough or haven't signed in!", Helper.getCurrentPrincipal().getAge());
            }
            boolean previouslyDisliked = userDislikedVideosRepository.existsByOwnerAndVideo(Helper.getCurrentPrincipal().getId(), videoId);
            if (previouslyDisliked) {
                return new ResponseMessage(false, "you  already disliked the video before!", video);
            }else {
                if (!fileUploaded(video.getVideoLink())){
                    return new ResponseMessage(false, "video doesn't have its file yet, first owner must upload the file!", video);
                }
                if (userLikedVideosRepository.existsByOwnerAndVideo(Helper.getCurrentPrincipal().getId(), videoId)) {
                    userLikedVideosRepository.deleteByOwnerAndVideo(Helper.getCurrentPrincipal().getId(), videoId);
                    video.setDislikes(video.getDislikes() + 1);
                    video.setLikes(video.getLikes() - 1);
                }else {
                    video.setDislikes(video.getDislikes() + 1);
                }
                videoRepository.save(video);
                UserDislikedVideos userDislikedVideos = new UserDislikedVideos();
                userDislikedVideos.setVideo(video.getId());
                userDislikedVideos.setOwner(Helper.getCurrentPrincipal().getId());
                userDislikedVideosRepository.save(userDislikedVideos);
                return new ResponseMessage(true, "Video disliked", video);
            }
        } else {
            return new ResponseMessage(false, "video with this id doesn't exist", videoId);
        }
    }

    @Transactional
    public ResponseMessage deleteVideo(Integer videoId) {
        Optional<Video> optionalVideo = videoRepository.findById(videoId);
        if (optionalVideo.isPresent()) {
            Video video = optionalVideo.get();
            Channel channel = video.getChannel();
            Optional<ChannelOwner> optionalRelation = channelOwnerRepository.findByChannel(channel.getId());
            if (optionalRelation.isPresent()) {
                ChannelOwner relation = optionalRelation.get();
                Integer owner = relation.getOwner();
                if (owner.equals(Helper.getCurrentPrincipal().getId())) {
                    videoRepository.delete(video);
                    Integer id = video.getId();
                    userDislikedVideosRepository.deleteAllByVideo(id);
                    userLikedVideosRepository.deleteAllByVideo(id);
                    playlistVideosRepository.deleteAllByVideo(id);
                    historyVideosRepository.deleteAllByVideo(id);
                    Optional<List<Comment>> allByVideoId = commentRepository.findAllByVideoId(id);
                    if (allByVideoId.isPresent()) {
                        List<Comment> comments = allByVideoId.get();
                        for (Comment comment : comments) {
                            commentRepository.delete(comment);
                            Integer commentId = comment.getId();
                            commentRepository.deleteByParentCommentId(commentId);
                            userLikedCommentsRepository.deleteAllByComment(commentId);
                            userDislikedCommentsRepository.deleteAllByComment(commentId);
                        }
                    }else {
                        commentRepository.deleteAllByVideoId(id);
                    }
                    return new ResponseMessage(true, "Video deleted", video);
                } else {
                    return new ResponseMessage(false, "you can't delete this video because you are not the owner", videoId);
                }
            } else {
                return new ResponseMessage(false, "channel_owner yo'q , lekin channel bor , backendda xato", channel);
            }
        } else {
            return new ResponseMessage(false, "video with this id doesn't exist", videoId);
        }
    }

    public ResponseMessage searchVideos(String title) {
        // Nomi bo'yicha qidirish
        List<Video> videos = videoRepository.findByTitleContainingIgnoreCase(title);

        if (videos.isEmpty()) {
            return new ResponseMessage(false, "No videos found with the given title", null);
        }

        //File link bilan muammo bo'lmasligi uchun o'zgartirildi, frontend uchun
        List<Video> filteredVideos = videos.stream().filter(video -> {
            return Helper.fileUploaded(video.getVideoLink());
        }).toList();
        return new ResponseMessage(true, "Videos found", filteredVideos);
    }
}
